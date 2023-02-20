package com.ces.worker.infra.queue

typealias DeliveryId = String
typealias MessageContent = String

open class Message(val content: MessageContent)
class ReceivedMessage(val deliveryId: DeliveryId, content: MessageContent) : Message(content)

interface MessageQueue {
    suspend fun sendMessage(message: Message)

    suspend fun receiveMessage(): ReceivedMessage

    suspend fun markProcessed(id: DeliveryId)

    suspend fun markUnprocessed(id: DeliveryId, requeue: Boolean)

    fun close()
}