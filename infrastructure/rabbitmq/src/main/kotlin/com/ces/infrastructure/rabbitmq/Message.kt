package com.ces.infrastructure.rabbitmq

typealias DeliveryId = String
typealias MessageContent = String

open class Message(val content: MessageContent)
class ReceivedMessage(val deliveryId: DeliveryId, content: MessageContent) : Message(content)