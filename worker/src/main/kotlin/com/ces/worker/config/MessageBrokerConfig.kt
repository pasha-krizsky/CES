package com.ces.worker.config

class MessageBrokerConfig(
    val connectionName: String,
)

class QueueConfig(
    val name: String,
    val prefetchCount: Int = 1,
)