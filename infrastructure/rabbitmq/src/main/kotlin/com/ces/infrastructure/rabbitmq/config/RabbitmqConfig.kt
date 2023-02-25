package com.ces.infrastructure.rabbitmq.config

class RabbitmqConfig(
    val connectionName: String,
)

class QueueConfig(
    val name: String,
    val prefetchCount: Int = 1,
)