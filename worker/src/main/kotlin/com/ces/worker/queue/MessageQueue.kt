package com.ces.worker.queue

data class Message(val content: String)

interface MessageQueue {
    suspend fun getMessage(): Message

    suspend fun sendMessage(message: Message)

    fun close()
}