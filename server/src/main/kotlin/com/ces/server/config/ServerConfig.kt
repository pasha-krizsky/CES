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
            val rabbitmq = RabbitmqConfig(hocon.property(RABBITMQ_CONNECTION).getString())
            val codeExecutionRequestQueue = QueueConfig(
                hocon.property(CODE_EXECUTION_REQUEST_QUEUE_NAME).getString(),
                hocon.property(CODE_EXECUTION_REQUEST_QUEUE_PREFETCH).getString().toInt(),
            )
            val codeExecutionResponseQueue = QueueConfig(
                hocon.property(CODE_EXECUTION_RESPONSE_QUEUE_NAME).getString(),
                hocon.property(CODE_EXECUTION_RESPONSE_QUEUE_PREFETCH).getString().toInt(),
            )
            val minio = MinioConfig(
                hocon.property(MINIO_ENDPOINT).getString(),
                hocon.property(MINIO_ACCESS_KEY).getString(),
                hocon.property(MINIO_SECRET_KEY).getString(),
            )
            val codeExecutionBucketName = hocon.property(BUCKET_NAME).getString()

            return ServerConfig(
                rabbitmq = rabbitmq,
                codeExecutionRequestQueue = codeExecutionRequestQueue,
                codeExecutionResponseQueue = codeExecutionResponseQueue,
                minio = minio,
                codeExecutionBucketName = codeExecutionBucketName,
            )
        }

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