package com.ces.infrastructure.rabbitmq

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.TestContainerExtension
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.GenericContainer

class RabbitMessageQueueTest : StringSpec({

    val imageName = "rabbitmq:3-management"
    val queueName = "test-queue"
    val connectionName = "amqp://guest:guest@localhost:5672"

    // docker run -d --hostname rabbit-mq-node --name rabbit-mq-instance -p 15672:15672 -p 5672:5672 rabbitmq:3-management
    install(TestContainerExtension(GenericContainer(imageName))) {
        portBindings = listOf("5672:5672", "15672:15672")
        withExposedPorts(5672, 15672)
    }

    "should send and receive message" {
        val messageToSend = Message("test message")

        val queue = RabbitMessageQueue(connectionName, queueName)
        queue.sendMessage(messageToSend)
        val receivedMessage = queue.receiveMessage()

        receivedMessage shouldBe messageToSend
    }
})
