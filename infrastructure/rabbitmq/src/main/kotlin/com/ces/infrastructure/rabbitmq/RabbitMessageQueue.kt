package com.ces.infrastructure.rabbitmq

import com.ces.infrastructure.rabbitmq.config.RabbitmqConfig
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.impl.DefaultCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.nio.charset.StandardCharsets.UTF_8
import com.rabbitmq.client.Channel as RabbitChannel
import kotlinx.coroutines.channels.Channel as CoroutineChannel

class RabbitMessageQueue(
    private val queueName: String,
    config: RabbitmqConfig,
    prefetchCount: Int = 1,
) : MessageQueue {

    private val log = KotlinLogging.logger {}

    private val deliveryChannel = CoroutineChannel<ReceivedMessage>(capacity = prefetchCount)

    // TODO share connections/channels between queues
    private val rabbitConnection: Connection
    private val rabbitChannel: RabbitChannel

    private val deliveryHandler: DeliverCallback
    private val cancelHandler: CancelCallback

    init {
        val connectionFactory = ConnectionFactory()
        connectionFactory.setCredentialsProvider(DefaultCredentialsProvider(config.user, config.password))
        connectionFactory.host = config.host
        connectionFactory.port = config.port
        rabbitConnection = connectionFactory.newConnection()
        rabbitChannel = rabbitConnection.createChannel()
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
        rabbitChannel.basicConsume(queueName, AUTO_ACC, CONSUMER_TAG, deliveryHandler, cancelHandler)
    }

    override suspend fun sendMessage(message: Message) = withContext(Dispatchers.IO) {
        log.debug { "Sending a message, content=${message.content}" }
        rabbitChannel.basicPublish(EXCHANGE_NAME, queueName, null, message.content.toByteArray(UTF_8))
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

    override fun close() {
        rabbitChannel.close()
        rabbitConnection.close()
    }

    companion object {
        const val EXCHANGE_NAME = ""
        const val CONSUMER_TAG = "worker"
        const val AUTO_ACC = false
    }
}