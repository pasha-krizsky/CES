package com.ces.infrastructure.rabbitmq

interface ReceiveQueue {

    suspend fun receiveMessage(): ReceivedMessage

    suspend fun markProcessed(id: DeliveryId)

    suspend fun markUnprocessed(id: DeliveryId, requeue: Boolean)
}