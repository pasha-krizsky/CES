package com.ces.infrastructure.rabbitmq.config

class RabbitmqConfig(
    val user: String,
    val password: String,
    val host: String,
    val port: Int,
)

class QueueConfig(
    val name: String,
    val prefetchCount: Int = 1,
)