package com.ces.worker

import com.ces.infrastructure.docker.DockerClient
import com.ces.infrastructure.minio.MinioConfig
import com.ces.infrastructure.minio.MinioStorage
import com.ces.infrastructure.rabbitmq.RabbitMessageQueue
import com.ces.infrastructure.rabbitmq.config.QueueConfig
import com.ces.infrastructure.rabbitmq.config.RabbitmqConfig
import com.ces.worker.config.DockerConfig
import com.ces.worker.config.RunnerConfig
import com.ces.worker.config.RunnerContainerConfig
import com.ces.worker.config.WorkerConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import io.minio.MinioAsyncClient
import kotlinx.coroutines.runBlocking
import java.time.Duration

fun main(): Unit = runBlocking {
    val config = applicationConfig()

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
    config.broker.connectionName,
    config.codeExecutionRequestQueue.prefetchCount,
)

private fun responseQueue(config: WorkerConfig) = RabbitMessageQueue(
    config.codeExecutionResponseQueue.name,
    config.broker.connectionName
)

// TODO Move configuration to file
private fun applicationConfig() = WorkerConfig(
    docker = DockerConfig("npipe:////./pipe/docker_engine"),
    runner = RunnerConfig(
        imageName = "runner-mono",
        workDir = "/home/newuser",
        codeExecutionTimeoutMillis = 5_000,
        logsPollIntervalMillis = 100,
        container = RunnerContainerConfig(
            capDrop = "ALL",
            cgroupnsMode = "private",
            networkMode = "none",
            cpusetCpus = "1",
            cpuQuota = 10000000,
            memory = 100000000,
            memorySwap = 500000000,
        )
    ),
    bucketName = "code-execution",
    broker = RabbitmqConfig(
        connectionName = "amqp://guest:guest@localhost:5672"
    ),
    codeExecutionRequestQueue = QueueConfig(
        name = "code-execution-request",
        prefetchCount = 1,
    ),
    codeExecutionResponseQueue = QueueConfig(
        name = "code-execution-response",
        prefetchCount = 1,
    ),
    minio = MinioConfig(
        endpoint = "http://127.0.0.1:9000",
        accessKey = "minioadmin",
        secretKey = "minioadmin",
    )
)

