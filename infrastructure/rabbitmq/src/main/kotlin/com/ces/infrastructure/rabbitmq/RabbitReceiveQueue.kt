package com.ces.infrastructure.rabbitmq

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.DeliverCallback
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID.randomUUID
import com.rabbitmq.client.Channel as RabbitChannel
import kotlinx.coroutines.channels.Channel as CoroutineChannel

class RabbitReceiveQueue(
    queueName: String,
    connector: RabbitmqConnector,
    prefetchCount: Int = 1,
) : ReceiveQueue {

    private val log = KotlinLogging.logger {}

    private val deliveryChannel = CoroutineChannel<ReceivedMessage>(capacity = prefetchCount)

    private val rabbitChannel: RabbitChannel

    private val deliveryHandler: DeliverCallback
    private val cancelHandler: CancelCallback

    init {
        rabbitChannel = connector.rabbitChannel
        deliveryHandler = DeliverCallback { _, delivery ->
            runBlocking {
                val messageId = delivery.envelope.deliveryTag.toString()
                val messageContent = String(delivery.body, UTF_8)
                log.debug { "Received a message, messageId=$messageId, content=$messageContent" }
                val message = ReceivedMessage(messageId, messageContent)
                deliveryChannel.send(message)
            }
        }
        cancelHandler = CancelCallback { _ -> }

        rabbitChannel.basicQos(prefetchCount)
        rabbitChannel.queueDeclare(queueName, false, false, false, null)
        val consumerTag = "$CONSUMER_TAG_PREFIX${randomUUID()}"
        rabbitChannel.basicConsume(queueName, AUTO_ACC, consumerTag, deliveryHandler, cancelHandler)
    }

    override suspend fun receiveMessage(): ReceivedMessage {
        return deliveryChannel.receive()
    }

    override suspend fun markProcessed(id: DeliveryId) {
        log.debug { "Marking message as processed, messageId=$id" }
        rabbitChannel.basicAck(id.toLong(), false)
    }

    override suspend fun markUnprocessed(id: DeliveryId, requeue: Boolean) {
        log.debug { "Marking message as unprocessed, messageId=$id" }
        rabbitChannel.basicNack(id.toLong(), false, requeue)
    }

    companion object {
        const val CONSUMER_TAG_PREFIX = "worker"
        const val AUTO_ACC = false
    }
}