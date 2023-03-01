package com.ces.worker

import com.ces.domain.events.CodeExecutionFinishedEvent
import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.events.CodeExecutionStartedEvent
import com.ces.domain.json.JsonConfig.Companion.decodeCodeExecutionEvent
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeCompilerType.MONO
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState.COMPLETED
import com.ces.domain.types.ProgrammingLanguage.C_SHARP
import com.ces.infrastructure.docker.DockerClient
import com.ces.infrastructure.docker.DockerTestData
import com.ces.infrastructure.docker.DockerTestData.Companion.createTestImage
import com.ces.infrastructure.docker.DockerTestData.Companion.loadResource
import com.ces.infrastructure.minio.MinioExtension
import com.ces.infrastructure.minio.MinioStorage
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.*
import com.ces.worker.config.ApplicationConfigTestData.Companion.applicationConfig
import com.ces.worker.flow.CodeExecutionFlow
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.minio.MinioAsyncClient
import kotlinx.datetime.Clock.System.now

val config = applicationConfig()

class CodeExecutionFlowTest : StringSpec({

    timeout = 600_000

    extension(MinioExtension(config.minio.accessKey, config.minio.secretKey))
    extension(RabbitmqExtension())

    val runnerDockerfile = loadResource(DOCKERFILE)
    val runnerEntrypoint = loadResource(ENTRY_POINT)

    val docker = DockerClient(DockerTestData.httpDockerClient)

    lateinit var connector: RabbitmqConnector

    lateinit var requestQueueIn: ReceiveQueue
    lateinit var requestQueueOut: SendQueue

    lateinit var responseQueueIn: ReceiveQueue
    lateinit var responseQueueOut: SendQueue

    lateinit var minioStorage: ObjectStorage

    lateinit var flow: CodeExecutionFlow
    beforeSpec {
        connector = RabbitmqConnector(config.rabbitmq)

        requestQueueIn = requestInQueue(connector)
        requestQueueOut = requestOutQueue(connector)

        responseQueueIn = responseInQueue(connector)
        responseQueueOut = responseOutQueue(connector)

        minioStorage = minioStorage()
        minioStorage.createBucket(config.codeExecutionBucketName)

        createTestImage(runnerDockerfile, runnerEntrypoint)

        flow = CodeExecutionFlow(config, docker, requestQueueIn, responseQueueOut, minioStorage)
    }

    "should run code execution flow" {
        listOf(
            BASIC_SCRIPT to BASIC_SCRIPT_EXPECTED_OUTCOME,
            FOUR_SECONDS_SCRIPT to FOUR_SECONDS_SCRIPT_EXPECTED_OUTCOME
        ).forAll { (scriptName, expectedLogs) ->
            val codeExecutionId = CodeExecutionId.random()
            val storagePath = "${codeExecutionId.value}/$SOURCE_FILE"
            minioStorage.uploadFile(config.codeExecutionBucketName, loadResource(scriptName).absolutePath, storagePath)

            val request = CodeExecutionRequestedEvent(codeExecutionId, now(), C_SHARP, MONO, storagePath)
            requestQueueOut.sendMessage(Message(encodeCodeExecutionEvent(request)))

            flow.run()

            val startedResponse = responseQueueIn.receiveMessage()
            val startedEvent = decodeCodeExecutionEvent(startedResponse.content)
            startedEvent.shouldBeInstanceOf<CodeExecutionStartedEvent>()
            startedEvent.id shouldBe codeExecutionId
            startedEvent.executionLogsPath shouldBe "${codeExecutionId.value}/$RESULTS_FILE"
            responseQueueIn.markProcessed(startedResponse.deliveryId)

            shouldReceiveCorrectExecutionFinishedEvent(responseQueueIn, codeExecutionId)
            shouldStoreCorrectExecutionLogsToObjectStorage(minioStorage, startedEvent, expectedLogs)
        }
    }
})

private suspend fun shouldReceiveCorrectExecutionFinishedEvent(
    responseQueue: ReceiveQueue,
    codeExecutionId: CodeExecutionId
) {
    val finishedResponse = responseQueue.receiveMessage()
    val finishedEvent = decodeCodeExecutionEvent(finishedResponse.content)

    finishedEvent.shouldBeInstanceOf<CodeExecutionFinishedEvent>()
    finishedEvent.id shouldBe codeExecutionId
    finishedEvent.state shouldBe COMPLETED
    responseQueue.markProcessed(finishedResponse.deliveryId)
}

private suspend fun StringSpec.shouldStoreCorrectExecutionLogsToObjectStorage(
    minioStorage: ObjectStorage,
    startedEvent: CodeExecutionStartedEvent,
    expectedLogs: String
) {
    val resultPath = tempdir().absolutePath + DOWNLOADED_SUFFIX
    val logs =
        minioStorage.downloadFile(config.codeExecutionBucketName, startedEvent.executionLogsPath, resultPath)
    logs.readText() shouldBe expectedLogs

    logs.delete()
}

private fun requestInQueue(connector: RabbitmqConnector) = RabbitReceiveQueue(
    config.codeExecutionRequestQueue.name, connector
)

private fun requestOutQueue(connector: RabbitmqConnector) = RabbitSendQueue(
    config.codeExecutionRequestQueue.name, connector
)

private fun responseInQueue(connector: RabbitmqConnector) = RabbitReceiveQueue(
    config.codeExecutionResponseQueue.name, connector
)

private fun responseOutQueue(connector: RabbitmqConnector) = RabbitSendQueue(
    config.codeExecutionResponseQueue.name, connector
)

private fun minioStorage() = MinioStorage(
    MinioAsyncClient.builder()
        .endpoint(config.minio.endpoint)
        .credentials(config.minio.accessKey, config.minio.secretKey)
        .build()
)

private const val DOCKERFILE = "Dockerfile"
private const val ENTRY_POINT = "entrypoint.sh"

private const val BASIC_SCRIPT = "basic_script.cs"
private const val BASIC_SCRIPT_EXPECTED_OUTCOME = "Hello from runner container"

private const val FOUR_SECONDS_SCRIPT = "four_seconds_script.cs"
private const val FOUR_SECONDS_SCRIPT_EXPECTED_OUTCOME = "line 0\nline 1\nline 2\nline 3\n"

private const val SOURCE_FILE = "source"
private const val RESULTS_FILE = "results"
private const val DOWNLOADED_SUFFIX = "_downloaded"