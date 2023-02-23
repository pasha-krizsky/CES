package com.ces.infrastructure.rabbitmq

import com.ces.infrastructure.rabbitmq.RabbitmqTestData.Companion.RABBIT_MQ_CONNECTION_NAME
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.apache.commons.lang3.RandomStringUtils.randomAlphabetic
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric

class RabbitMessageQueueTest : StringSpec({

    extension(RabbitmqExtension())

    "should send and receive message" {
        val queue = RabbitMessageQueue(randomAlphabetic(10).lowercase(), RABBIT_MQ_CONNECTION_NAME)

        val sourceMessage = Message(randomAlphanumeric(TEST_MESSAGE_CONTENT_LENGTH))
        queue.sendMessage(sourceMessage)

        val receivedMessage = queue.receiveMessage()

        receivedMessage.content shouldBe sourceMessage.content
    }
}) {
    companion object {
        private const val TEST_MESSAGE_CONTENT_LENGTH = 1000
    }
}