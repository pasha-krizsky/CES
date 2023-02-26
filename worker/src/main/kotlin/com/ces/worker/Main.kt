package com.ces.worker

import com.ces.infrastructure.docker.DockerClient
import com.ces.infrastructure.minio.MinioStorage
import com.ces.infrastructure.rabbitmq.RabbitMessageQueue
import com.ces.worker.config.WorkerConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.typesafe.config.ConfigFactory
import io.minio.MinioAsyncClient
import kotlinx.coroutines.runBlocking
import java.time.Duration

fun main(): Unit = runBlocking {
    val config = WorkerConfig.from(ConfigFactory.load())

    // TODO move dependencies creation and other logic from main
    val httpClient = httpDockerClient(
        DefaultDockerClientConfig
            .createDefaultConfigBuilder()
            .withDockerHost(config.docker.socket)
            .build()
    )
    val docker = DockerClient(httpClient)
    val requestQueue = requestQueue(config)
    val responseQueue = responseQueue(config)
    val minioClient: MinioAsyncClient = MinioAsyncClient.builder()
        .endpoint(config.minio.endpoint)
        .credentials(config.minio.accessKey, config.minio.secretKey)
        .build()
    val minioStorage = MinioStorage(minioClient)

    val flow = CodeExecutionFlow(config, docker, requestQueue, responseQueue, minioStorage)

    // TODO Doesn't look good, change
    while (true) {
        flow.run()
    }
}

private fun httpDockerClient(dockerConfig: DefaultDockerClientConfig) =
    ApacheDockerHttpClient.Builder()
        .dockerHost(dockerConfig.dockerHost)
        .maxConnections(10)
        .connectionTimeout(Duration.ofSeconds(10))
        .responseTimeout(Duration.ofMinutes(5))
        .build()

private fun requestQueue(config: WorkerConfig) = RabbitMessageQueue(
    config.codeExecutionRequestQueue.name,
    config.rabbitmq.connectionName,
    config.codeExecutionRequestQueue.prefetchCount,
)

private fun responseQueue(config: WorkerConfig) = RabbitMessageQueue(
    config.codeExecutionResponseQueue.name,
    config.rabbitmq.connectionName
)

