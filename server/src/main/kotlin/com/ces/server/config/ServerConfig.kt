package com.ces.server.config

import com.ces.infrastructure.minio.MinioConfig
import com.ces.infrastructure.rabbitmq.config.QueueConfig
import com.ces.infrastructure.rabbitmq.config.RabbitmqConfig
import io.ktor.server.config.*

class ServerConfig(
    val rabbitmq: RabbitmqConfig,
    val codeExecutionRequestQueue: QueueConfig,
    val codeExecutionResponseQueue: QueueConfig,
    val minio: MinioConfig,
    val codeExecutionBucketName: String,
) {
    companion object {
        fun from(hocon: HoconApplicationConfig): ServerConfig {
            val rabbitmq = RabbitmqConfig(stringProperty(hocon, RABBITMQ_CONNECTION))
            val codeExecutionRequestQueue = QueueConfig(
                stringProperty(hocon, CODE_EXECUTION_REQUEST_QUEUE_NAME),
                intProperty(hocon, CODE_EXECUTION_REQUEST_QUEUE_PREFETCH),
            )
            val codeExecutionResponseQueue = QueueConfig(
                stringProperty(hocon, CODE_EXECUTION_RESPONSE_QUEUE_NAME),
                intProperty(hocon, CODE_EXECUTION_RESPONSE_QUEUE_PREFETCH),
            )
            val minio = MinioConfig(
                stringProperty(hocon, MINIO_ENDPOINT),
                stringProperty(hocon, MINIO_ACCESS_KEY),
                stringProperty(hocon, MINIO_SECRET_KEY),
            )
            val codeExecutionBucketName = stringProperty(hocon, BUCKET_NAME)

            return ServerConfig(
                rabbitmq = rabbitmq,
                codeExecutionRequestQueue = codeExecutionRequestQueue,
                codeExecutionResponseQueue = codeExecutionResponseQueue,
                minio = minio,
                codeExecutionBucketName = codeExecutionBucketName,
            )
        }

        private fun stringProperty(hocon: HoconApplicationConfig, name: String) =
            hocon.property(name).getString()

        private fun intProperty(hocon: HoconApplicationConfig, name: String) =
            stringProperty(hocon, name).toInt()

        private const val RABBITMQ_CONNECTION = "rabbitmq.connectionName"

        private const val CODE_EXECUTION_REQUEST_QUEUE_NAME = "codeExecutionRequestQueue.name"
        private const val CODE_EXECUTION_REQUEST_QUEUE_PREFETCH = "codeExecutionRequestQueue.prefetchCount"

        private const val CODE_EXECUTION_RESPONSE_QUEUE_NAME = "codeExecutionResponseQueue.name"
        private const val CODE_EXECUTION_RESPONSE_QUEUE_PREFETCH = "codeExecutionResponseQueue.prefetchCount"

        private const val MINIO_ENDPOINT = "minio.endpoint"
        private const val MINIO_ACCESS_KEY = "minio.accessKey"
        private const val MINIO_SECRET_KEY = "minio.secretKey"

        private const val BUCKET_NAME = "codeExecutionBucketName"
    }
}