package com.ces.server.module

import com.ces.infrastructure.minio.MinioStorage
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.MessageQueue
import com.ces.infrastructure.rabbitmq.RabbitMessageQueue
import com.ces.server.config.ServerConfig
import com.ces.server.flow.CodeExecutionCreateFlow
import com.ces.server.storage.CodeExecutionStorage
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.minio.MinioAsyncClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.named
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

// TODO Consider splitting this module into submodules as application grows
val appModule = module {
    val serverConfig = ServerConfig.from(HoconApplicationConfig(ConfigFactory.load()))
    single {
        serverConfig
    }
    single<MinioAsyncClient> {
        MinioAsyncClient.builder()
            .endpoint(serverConfig.minio.endpoint)
            .credentials(serverConfig.minio.accessKey, serverConfig.minio.secretKey)
            .build()
    }
    single {
        RabbitMessageQueue(
            serverConfig.codeExecutionRequestQueue.name,
            serverConfig.rabbitmq.connectionName,
            serverConfig.codeExecutionRequestQueue.prefetchCount,
        )
    } withOptions {
        named(REQUEST_QUEUE_QUALIFIER)
        bind<MessageQueue>()
    }
    single {
        RabbitMessageQueue(
            serverConfig.codeExecutionResponseQueue.name,
            serverConfig.rabbitmq.connectionName,
            serverConfig.codeExecutionResponseQueue.prefetchCount,
        )
    } withOptions {
        named(RESPONSE_QUEUE_QUALIFIER)
        bind<MessageQueue>()
    }
    singleOf(::CodeExecutionStorage)
    singleOf(::MinioStorage) bind ObjectStorage::class
    single {
        CodeExecutionCreateFlow(
            get(),
            get(),
            get(),
            get(named(REQUEST_QUEUE_QUALIFIER))
        )
    }
}

private const val REQUEST_QUEUE_QUALIFIER = "codeExecutionRequestQueue"
private const val RESPONSE_QUEUE_QUALIFIER = "codeExecutionResponseQueue"