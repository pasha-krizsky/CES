package com.ces.worker

import com.ces.worker.config.*
import com.ces.worker.infra.docker.NettyDockerImpl
import com.ces.worker.infra.docker.NettySocketClient
import com.ces.worker.infra.queue.RabbitMessageQueue
import com.ces.worker.infra.storage.MinioStorage
import io.minio.MinioAsyncClient
import kotlinx.coroutines.runBlocking

// TODO Move to test
private val SCRIPT_SOURCE_CODE = """
namespace HelloWorld
{
    class Hello {
        static void Main(string[] args)
        {
            for (int i = 0; i < 10; i++) {
                System.Console.WriteLine("Hello World " + i);
                System.Console.Error.WriteLine("Hello Error " + i);
                System.Threading.Thread.Sleep(1000);
            }
        }
    }
}
""".trimIndent()

fun main(): Unit = runBlocking {
    val config = applicationConfig()

    // TODO move dependencies creation from main
    val docker = NettyDockerImpl(NettySocketClient(config.docker.socket))
    val requestQueue = getRequestQueue(config)
    val responseQueue = getResponseQueue(config)
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

private fun getRequestQueue(config: ApplicationConfig) = RabbitMessageQueue(
    config.broker.connectionName,
    config.codeExecutionRequestQueue.name,
    config.codeExecutionRequestQueue.prefetchCount,
)

private fun getResponseQueue(config: ApplicationConfig) = RabbitMessageQueue(
    config.broker.connectionName,
    config.codeExecutionResponseQueue.name
)

// TODO Move to file
private fun applicationConfig() = ApplicationConfig(
    docker = DockerConfig("/var/run/docker.sock"),
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
    localStoragePath = "/tmp",
    broker = MessageBrokerConfig(
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

