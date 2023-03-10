package com.ces.server.config

import com.ces.infrastructure.database.DatabaseConfig
import com.ces.infrastructure.minio.MinioConfig
import com.ces.infrastructure.rabbitmq.config.QueueConfig
import com.ces.infrastructure.rabbitmq.config.RabbitmqConfig
import io.ktor.server.config.*

class ServerConfig(
    val database: DatabaseConfig,
    val rabbitmq: RabbitmqConfig,
    val codeExecutionRequestQueue: QueueConfig,
    val codeExecutionResponseQueue: QueueConfig,
    val minio: MinioConfig,
    val codeExecutionBucketName: String,
) {

    companion object {

        val tmpDir: String = System.getProperty("java.io.tmpdir")

        fun from(config: HoconApplicationConfig): ServerConfig {
            val database = DatabaseConfig(
                url = stringProperty(config, DATABASE_URL),
                driver = stringProperty(config, DATABASE_DRIVER),
                user = stringProperty(config, DATABASE_USER),
                password = stringProperty(config, DATABASE_PASSWORD),
            )
            val rabbitmq = RabbitmqConfig(
                user = stringProperty(config, RABBITMQ_USER),
                password = stringProperty(config, RABBITMQ_PASSWORD),
                host = stringProperty(config, RABBITMQ_HOST),
                port = intProperty(config, RABBITMQ_PORT),
            )
            val codeExecutionRequestQueue = QueueConfig(
                name = stringProperty(config, CODE_EXECUTION_REQUEST_QUEUE_NAME)
            )
            val codeExecutionResponseQueue = QueueConfig(
                name = stringProperty(config, CODE_EXECUTION_RESPONSE_QUEUE_NAME),
                prefetchCount = intProperty(config, CODE_EXECUTION_RESPONSE_QUEUE_PREFETCH),
            )
            val minio = MinioConfig(
                endpoint = stringProperty(config, MINIO_ENDPOINT),
                accessKey = stringProperty(config, MINIO_ACCESS_KEY),
                secretKey = stringProperty(config, MINIO_SECRET_KEY),
            )
            return ServerConfig(
                database = database,
                rabbitmq = rabbitmq,
                codeExecutionRequestQueue = codeExecutionRequestQueue,
                codeExecutionResponseQueue = codeExecutionResponseQueue,
                minio = minio,
                codeExecutionBucketName = stringProperty(config, BUCKET_NAME),
            )
        }

        private fun stringProperty(hocon: HoconApplicationConfig, name: String) =
            hocon.property(name).getString()

        private fun intProperty(hocon: HoconApplicationConfig, name: String) =
            stringProperty(hocon, name).toInt()

        private const val DATABASE_URL = "database.url"
        private const val DATABASE_DRIVER = "database.driver"
        private const val DATABASE_USER = "database.user"
        private const val DATABASE_PASSWORD = "database.password"

        private const val RABBITMQ_USER = "rabbitmq.user"
        private const val RABBITMQ_PASSWORD = "rabbitmq.password"
        private const val RABBITMQ_HOST = "rabbitmq.host"
        private const val RABBITMQ_PORT = "rabbitmq.port"

        private const val CODE_EXECUTION_REQUEST_QUEUE_NAME = "codeExecutionRequestQueue.name"
        private const val CODE_EXECUTION_RESPONSE_QUEUE_NAME = "codeExecutionResponseQueue.name"
        private const val CODE_EXECUTION_RESPONSE_QUEUE_PREFETCH = "codeExecutionResponseQueue.prefetchCount"

        private const val MINIO_ENDPOINT = "minio.endpoint"
        private const val MINIO_ACCESS_KEY = "minio.accessKey"
        private const val MINIO_SECRET_KEY = "minio.secretKey"

        private const val BUCKET_NAME = "codeExecutionBucketName"
    }
}