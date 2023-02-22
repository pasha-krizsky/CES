package com.ces.worker

import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.types.CodeCompilerType.MONO
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.ProgrammingLanguage.C_SHARP
import com.ces.infrastructure.docker.DockerClient
import com.ces.infrastructure.docker.DockerTestFixtures
import com.ces.infrastructure.docker.DockerTestFixtures.Companion.createTestImage
import com.ces.infrastructure.docker.DockerTestFixtures.Companion.loadResource
import com.ces.infrastructure.minio.MinioExtension
import com.ces.infrastructure.minio.MinioStorage
import com.ces.infrastructure.rabbitmq.Message
import com.ces.infrastructure.rabbitmq.RabbitMessageQueue
import com.ces.infrastructure.rabbitmq.RabbitmqExtension
import com.ces.worker.config.*
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.minio.MinioAsyncClient
import kotlinx.datetime.Clock.System.now
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.seconds

class CodeExecutionFlowTest : StringSpec({

    extension(MinioExtension(config.minio.accessKey, config.minio.secretKey))
    extension(RabbitmqExtension())

    val runnerDockerfile = loadResource(DOCKERFILE)
    val runnerEntrypoint = loadResource(ENTRY_POINT)
    val sourceCode = loadResource(SOURCE_CODE)

    val docker = DockerClient(DockerTestFixtures.httpDockerClient)

    beforeSpec {
        createTestImage(runnerDockerfile, runnerEntrypoint, sourceCode)
    }

    "should run code execution flow" {
        val requestQueue = requestQueue()
        val responseQueue = responseQueue()
        val minioStorage = minioStorage()

        val flow = CodeExecutionFlow(config, docker, requestQueue, responseQueue, minioStorage)

        val codeExecutionId = CodeExecutionId.random()
        val storagePath = "test/${codeExecutionId.value}"
        val bucketName = config.bucketName
        minioStorage.createBucket(bucketName)
        minioStorage.uploadFile(bucketName, sourceCode.absolutePath, storagePath)

        val request = CodeExecutionRequestedEvent(codeExecutionId, now(), C_SHARP, MONO, storagePath)
        requestQueue.sendMessage(Message(Json.encodeToString(request)))

        flow.run()

        eventually(5.seconds) {
            val result = responseQueue.receiveMessage()
            result.shouldNotBeNull()
            // todo
            println(result.content)
        }
    }
})

private fun requestQueue() = RabbitMessageQueue(
    config.codeExecutionRequestQueue.name,
    config.broker.connectionName,
    config.codeExecutionRequestQueue.prefetchCount
)

private fun responseQueue() = RabbitMessageQueue(
    config.codeExecutionResponseQueue.name,
    config.broker.connectionName,
    config.codeExecutionResponseQueue.prefetchCount
)

private fun minioStorage() = MinioStorage(
    MinioAsyncClient.builder()
        .endpoint(config.minio.endpoint)
        .credentials(config.minio.accessKey, config.minio.secretKey)
        .build()
)

private const val DOCKERFILE = "Dockerfile"
private const val ENTRY_POINT = "entrypoint.sh"
private const val SOURCE_CODE = "code.cs"

// TODO Move configuration to file
private val config = ApplicationConfig(
    docker = DockerConfig("npipe:////./pipe/docker_engine"),
    runner = RunnerConfig(
        imageName = "runner-mono",
        workDir = "/home/runner",
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
    localStoragePath = File(CodeExecutionFlowTest::class.java.getResource("/")!!.toURI()).absolutePath,
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
