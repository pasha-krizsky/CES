package com.ces.worker.infra.queue

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
) : MessageQueue {

    private val deliveryChannel = CoroutineChannel<Message>(capacity = 1)

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
                val message = Message(String(delivery.body, UTF_8))
                deliveryChannel.send(message)
                rabbitChannel.basicAck(delivery.envelope.deliveryTag, false)
            }
        }
        cancelHandler = CancelCallback { _ -> }

        rabbitChannel.basicQos(RABBIT_PREFETCH_COUNT)
        rabbitChannel.queueDeclare(queueName, false, false, false, null)
        rabbitChannel.basicConsume(queueName, AUTO_ACC, CONSUMER_TAG, deliveryHandler, cancelHandler)
    }

    override suspend fun receiveMessage(): Message {
        return deliveryChannel.receive()
    }

    override suspend fun sendMessage(message: Message) = withContext(Dispatchers.IO) {
        rabbitChannel.basicPublish(EXCHANGE_NAME, queueName, null, message.content.toByteArray(UTF_8))
    }

    override fun close() {
        rabbitChannel.close()
        rabbitConnection.close()
    }

    companion object {
        const val EXCHANGE_NAME = ""
        const val CONSUMER_TAG = "worker"
        const val RABBIT_PREFETCH_COUNT = 1
        const val AUTO_ACC = false
    }
}