package com.ces.infrastructure.rabbitmq

import com.ces.infrastructure.rabbitmq.config.RabbitmqConfig
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.impl.DefaultCredentialsProvider
import kotlin.LazyThreadSafetyMode.SYNCHRONIZED

class RabbitmqConnector(
    private val config: RabbitmqConfig,
) {

    private val rabbitConnection: Connection by lazy(mode = SYNCHRONIZED) { initConnection() }

    val rabbitChannel: Channel by lazy(mode = SYNCHRONIZED) { initChannel() }

    private fun initConnection(): Connection {
        val connectionFactory = ConnectionFactory()
        connectionFactory.setCredentialsProvider(DefaultCredentialsProvider(config.user, config.password))
        connectionFactory.host = config.host
        connectionFactory.port = config.port
        return connectionFactory.newConnection()
    }

    private fun initChannel() = rabbitConnection.createChannel()

    fun close() {
        rabbitChannel.close()
        rabbitConnection.close()
    }
}