package com.ces.infrastructure.rabbitmq

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.nio.charset.StandardCharsets.UTF_8
import com.rabbitmq.client.Channel as RabbitChannel

class RabbitSendQueue(
    private val queueName: String,
    connector: RabbitmqConnector
) : SendQueue {

    private val log = KotlinLogging.logger {}

    private val rabbitChannel: RabbitChannel

    init {
        rabbitChannel = connector.rabbitChannel
        rabbitChannel.queueDeclare(queueName, false, false, false, null)
    }

    override suspend fun sendMessage(message: Message) = withContext(Dispatchers.IO) {
        log.debug { "Sending a message, content=${message.content}" }
        rabbitChannel.basicPublish(EXCHANGE_NAME, queueName, null, message.content.toByteArray(UTF_8))
    }

    companion object {
        const val EXCHANGE_NAME = ""
    }
}