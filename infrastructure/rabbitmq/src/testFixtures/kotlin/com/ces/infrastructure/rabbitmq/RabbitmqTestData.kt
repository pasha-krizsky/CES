package com.ces.infrastructure.rabbitmq

class RabbitmqTestData {
    companion object {
        const val RABBIT_MQ_CONNECTION_NAME = "amqp://guest:guest@localhost:5672"

        const val CODE_EXECUTION_REQUEST_QUEUE_NAME = "code-execution-request"
        const val CODE_EXECUTION_REQUEST_QUEUE_PREFETCH = 1

        const val CODE_EXECUTION_RESPONSE_QUEUE_NAME = "code-execution-response"
        const val CODE_EXECUTION_RESPONSE_QUEUE_PREFETCH = 1
    }
}