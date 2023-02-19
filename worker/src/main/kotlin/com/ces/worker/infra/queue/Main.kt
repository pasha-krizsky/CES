package com.ces.worker.infra.queue

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

const val QUEUE_NAME = "code-execution-requests"
const val CONNECTION_NAME = "amqp://guest:guest@localhost:5672"

// To start RabbitMQ in Docker:
// docker run -d --hostname rabbit-mq-node --name rabbit-mq-instance -p 15672:15672 -p 5672:5672 rabbitmq:3-management
fun main(): Unit = runBlocking {
    val queue = RabbitMessageQueue(QUEUE_NAME, CONNECTION_NAME)
    repeat(3) {
        queue.sendMessage(Message("Hello world"))
        println(queue.getMessage())
        delay(1000)
    }
    queue.close()
}