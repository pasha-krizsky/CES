package com.ces.worker.module

import com.ces.infrastructure.docker.Docker
import com.ces.infrastructure.docker.DockerClient
import com.ces.infrastructure.minio.MinioStorage
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.MessageQueue
import com.ces.infrastructure.rabbitmq.RabbitMessageQueue
import com.ces.worker.CodeExecutor
import com.ces.worker.config.WorkerConfig
import com.ces.worker.flow.CodeExecutionFlow
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.typesafe.config.ConfigFactory
import io.minio.MinioAsyncClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.named
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URI
import java.time.Duration

// TODO Consider splitting this module into submodules as application grows
val workerModule = module {
    val workerConfig = WorkerConfig.from(ConfigFactory.load())
    single { workerConfig }

    single {
        DockerClient(
            ApacheDockerHttpClient.Builder()
                .dockerHost(URI.create(workerConfig.docker.socket))
                .maxConnections(10)
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ofMinutes(5))
                .build()
        )
    } withOptions {
        bind<Docker>()
    }

    single<MinioAsyncClient> {
        MinioAsyncClient.builder()
            .endpoint(workerConfig.minio.endpoint)
            .credentials(workerConfig.minio.accessKey, workerConfig.minio.secretKey)
            .build()
    }
    singleOf(::MinioStorage) bind ObjectStorage::class

    single {
        RabbitMessageQueue(
            workerConfig.codeExecutionRequestQueue.name,
            workerConfig.rabbitmq,
            workerConfig.codeExecutionRequestQueue.prefetchCount,
        )
    } withOptions {
        named(REQUEST_QUEUE_QUALIFIER)
        bind<MessageQueue>()
    }
    single {
        RabbitMessageQueue(
            workerConfig.codeExecutionResponseQueue.name,
            workerConfig.rabbitmq,
            workerConfig.codeExecutionResponseQueue.prefetchCount,
        )
    } withOptions {
        named(RESPONSE_QUEUE_QUALIFIER)
        bind<MessageQueue>()
    }

    single {
        CodeExecutionFlow(
            workerConfig,
            get(),
            get(named(REQUEST_QUEUE_QUALIFIER)),
            get(named(RESPONSE_QUEUE_QUALIFIER)),
            get(),
        )
    }

    single {
        CodeExecutor(get())
    }
}

private const val REQUEST_QUEUE_QUALIFIER = "codeExecutionRequestQueue"
private const val RESPONSE_QUEUE_QUALIFIER = "codeExecutionResponseQueue"