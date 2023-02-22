package com.ces.infrastructure.rabbitmq

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets.UTF_8
import com.rabbitmq.client.Channel as RabbitChannel
import kotlinx.coroutines.channels.Channel as CoroutineChannel

class RabbitMessageQueue(
    private val queueName: String,
    connectionName: String,
    prefetchCount: Int = 1,
) : MessageQueue {

    private val deliveryChannel = CoroutineChannel<ReceivedMessage>(capacity = prefetchCount)

    // TODO share connections/channels between queues
    private val rabbitConnection: Connection
    private val rabbitChannel: RabbitChannel

    private val deliveryHandler: DeliverCallback
    private val cancelHandler: CancelCallback

    init {
        val connectionFactory = ConnectionFactory()
        rabbitConnection = connectionFactory.newConnection(connectionName)
        rabbitChannel = rabbitConnection.createChannel()
        deliveryHandler = DeliverCallback { _, delivery ->
            runBlocking {
                val messageId = delivery.envelope.deliveryTag.toString()
                val messageContent = String(delivery.body, UTF_8)
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
        rabbitChannel.basicPublish(EXCHANGE_NAME, queueName, null, message.content.toByteArray(UTF_8))
    }

    override suspend fun receiveMessage(): ReceivedMessage {
        return deliveryChannel.receive()
    }

    override suspend fun markProcessed(id: DeliveryId) {
        rabbitChannel.basicAck(id.toLong(), false)
    }

    override suspend fun markUnprocessed(id: DeliveryId, requeue: Boolean) {
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