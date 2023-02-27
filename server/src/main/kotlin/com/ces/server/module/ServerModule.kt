package com.ces.server.module

import com.ces.infrastructure.minio.MinioStorage
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.MessageQueue
import com.ces.infrastructure.rabbitmq.RabbitMessageQueue
import com.ces.server.config.ServerConfig
import com.ces.server.flow.CodeExecutionCreateFlow
import com.ces.server.listener.CodeExecutionEventsListener
import com.ces.server.storage.CodeExecutionDao
import com.ces.server.storage.CodeExecutionInMemoryDao
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
val serverModule = module {
    val serverConfig = ServerConfig.from(HoconApplicationConfig(ConfigFactory.load()))
    single { serverConfig }

    single<MinioAsyncClient> {
        MinioAsyncClient.builder()
            .endpoint(serverConfig.minio.endpoint)
            .credentials(serverConfig.minio.accessKey, serverConfig.minio.secretKey)
            .build()
    }
    singleOf(::MinioStorage) bind ObjectStorage::class

    single {
        RabbitMessageQueue(
            serverConfig.codeExecutionRequestQueue.name,
            serverConfig.rabbitmq,
            serverConfig.codeExecutionRequestQueue.prefetchCount,
        )
    } withOptions {
        named(REQUEST_QUEUE_QUALIFIER)
        bind<MessageQueue>()
    }
    single {
        RabbitMessageQueue(
            serverConfig.codeExecutionResponseQueue.name,
            serverConfig.rabbitmq,
            serverConfig.codeExecutionResponseQueue.prefetchCount,
        )
    } withOptions {
        named(RESPONSE_QUEUE_QUALIFIER)
        bind<MessageQueue>()
    }

    singleOf(::CodeExecutionInMemoryDao) bind CodeExecutionDao::class

    single {
        CodeExecutionCreateFlow(
            get(),
            get(),
            get(),
            get(named(REQUEST_QUEUE_QUALIFIER))
        )
    }

    single {
        CodeExecutionEventsListener(get(named(RESPONSE_QUEUE_QUALIFIER)), get())
    }
}

private const val REQUEST_QUEUE_QUALIFIER = "codeExecutionRequestQueue"
private const val RESPONSE_QUEUE_QUALIFIER = "codeExecutionResponseQueue"