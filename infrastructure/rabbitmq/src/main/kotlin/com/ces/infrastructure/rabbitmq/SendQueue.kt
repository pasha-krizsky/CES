package com.ces.infrastructure.rabbitmq

interface SendQueue {
    suspend fun sendMessage(message: Message)
}