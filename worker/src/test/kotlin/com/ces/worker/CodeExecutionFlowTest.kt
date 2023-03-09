package com.ces.worker

import com.ces.domain.entities.CodeExecution.Companion.SOURCE_FILE_NAME
import com.ces.domain.entities.CodeExecution.Companion.STDERR_LOGS_FILE_NAME
import com.ces.domain.entities.CodeExecution.Companion.STDOUT_LOGS_FILE_NAME
import com.ces.domain.events.CodeExecutionFinishedEvent
import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.events.CodeExecutionStartedEvent
import com.ces.domain.json.JsonConfig.Companion.decodeCodeExecutionEvent
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeCompilerType.MONO
import com.ces.domain.types.CodeExecutionFailureReason.NON_ZERO_EXIT_CODE
import com.ces.domain.types.CodeExecutionFailureReason.TIME_LIMIT_EXCEEDED
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState.COMPLETED
import com.ces.domain.types.CodeExecutionState.FAILED
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
import io.kotest.engine.spec.tempfile
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.minio.MinioAsyncClient
import kotlinx.datetime.Clock.System.now
import java.io.File
import java.io.File.separator
import java.lang.System.lineSeparator
import java.util.UUID.randomUUID
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString

val config = applicationConfig()

class CodeExecutionFlowTest : StringSpec({

    timeout = 600_000

    extension(MinioExtension(config.minio.accessKey, config.minio.secretKey))
    extension(RabbitmqExtension())

    val runnerDockerfile = loadResource(DOCKERFILE_RESOURCE)
    val runnerEntrypoint = loadResource(ENTRY_POINT_RESOURCE)

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

    val testData = loadTestData()
    "should run code execution flow" {
        testData.forAll { test ->
            val codeExecutionId = CodeExecutionId.random()
            val storagePath = "${codeExecutionId.value}/$SOURCE_FILE_NAME"

            uploadSourceCode(minioStorage, test.source, storagePath)

            val request = CodeExecutionRequestedEvent(codeExecutionId, now(), C_SHARP, MONO, storagePath)
            requestQueueOut.sendMessage(Message(encodeCodeExecutionEvent(request)))

            flow.run()

            val startedResponse = responseQueueIn.receiveMessage()
            val startedEvent = decodeCodeExecutionEvent(startedResponse.content)
            startedEvent.shouldBeInstanceOf<CodeExecutionStartedEvent>()
            startedEvent.id shouldBe codeExecutionId

            shouldHaveCorrectLogsPath(startedEvent)

            responseQueueIn.markProcessed(startedResponse.deliveryId)

            shouldReceiveCorrectExecutionFinishedEvent(responseQueueIn, codeExecutionId)

            shouldGenerateExpectedLogs(minioStorage, startedEvent.logsPath.stdoutPath, test.stdout)
            shouldGenerateExpectedLogs(minioStorage, startedEvent.logsPath.stderrPath, test.stderr)
        }
    }

    "should limit execution time" {
        val sourceCode = loadResource(TLE_ERROR_RESOURCE).readText()
        val codeExecutionId = CodeExecutionId.random()
        val storagePath = "${codeExecutionId.value}/$SOURCE_FILE_NAME"

        uploadSourceCode(minioStorage, sourceCode, storagePath)

        val request = CodeExecutionRequestedEvent(codeExecutionId, now(), C_SHARP, MONO, storagePath)
        requestQueueOut.sendMessage(Message(encodeCodeExecutionEvent(request)))

        flow.run()

        val startedResponse = responseQueueIn.receiveMessage()
        responseQueueIn.markProcessed(startedResponse.deliveryId)

        val finishedResponse = responseQueueIn.receiveMessage()
        val finishedEvent = decodeCodeExecutionEvent(finishedResponse.content)

        finishedEvent.shouldBeInstanceOf<CodeExecutionFinishedEvent>()
        finishedEvent.id shouldBe codeExecutionId
        finishedEvent.state shouldBe FAILED
        finishedEvent.failureReason shouldBe TIME_LIMIT_EXCEEDED
        finishedEvent.exitCode shouldBe 137
        responseQueueIn.markProcessed(finishedResponse.deliveryId)
    }

    "should handle non zero exit code" {
        val sourceCode = loadResource(NON_ZERO_EXIT_CODE_ERROR_RESOURCE).readText()
        val codeExecutionId = CodeExecutionId.random()
        val storagePath = "${codeExecutionId.value}/$SOURCE_FILE_NAME"

        uploadSourceCode(minioStorage, sourceCode, storagePath)

        val request = CodeExecutionRequestedEvent(codeExecutionId, now(), C_SHARP, MONO, storagePath)
        requestQueueOut.sendMessage(Message(encodeCodeExecutionEvent(request)))

        flow.run()

        val startedResponse = responseQueueIn.receiveMessage()
        responseQueueIn.markProcessed(startedResponse.deliveryId)

        val finishedResponse = responseQueueIn.receiveMessage()
        val finishedEvent = decodeCodeExecutionEvent(finishedResponse.content)

        finishedEvent.shouldBeInstanceOf<CodeExecutionFinishedEvent>()
        finishedEvent.id shouldBe codeExecutionId
        finishedEvent.state shouldBe FAILED
        finishedEvent.failureReason shouldBe NON_ZERO_EXIT_CODE
        finishedEvent.exitCode shouldBe 10
        responseQueueIn.markProcessed(finishedResponse.deliveryId)
    }
})

private suspend fun StringSpec.uploadSourceCode(
    storage: ObjectStorage,
    sourceCode: String,
    storagePath: String
) {
    val tmpFile = tempfile()
    tmpFile.appendText(sourceCode)
    storage.upload(config.codeExecutionBucketName, tmpFile.absolutePath, storagePath)
}

private fun shouldHaveCorrectLogsPath(startedEvent: CodeExecutionStartedEvent) {
    val id = startedEvent.id
    startedEvent.logsPath.stdoutPath shouldBe "${id.value}/$STDOUT_LOGS_FILE_NAME"
    startedEvent.logsPath.stderrPath shouldBe "${id.value}/$STDERR_LOGS_FILE_NAME"
}

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

private suspend fun StringSpec.shouldGenerateExpectedLogs(
    minioStorage: ObjectStorage, logsPath: String, expectedContent: String
) {
    val logsLocalPath = tempdir().absolutePath + separator + randomUUID()
    val bucket = config.codeExecutionBucketName
    val logs = minioStorage.get(bucket, logsPath, logsLocalPath)

    unifyLineBreaks(logs.readText()) shouldBe unifyLineBreaks(expectedContent)

    logs.delete()
}

private fun unifyLineBreaks(content: String) = content.replace(Regex("(\\r\\n|\\r|\\n)"), lineSeparator())

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

private data class TestData(val source: String, val stdout: String, val stderr: String)

private fun loadTestData(): List<TestData> {
    val testDataResource = loadResource(TEST_DATA_RESOURCE).toPath().listDirectoryEntries()
    return testDataResource.map {
        val prefix = it.pathString + separator

        val source = File(prefix + SOURCE_FILE_NAME).readText()
        val stdoutLogs = File(prefix + STDOUT_LOGS_FILE_NAME).readText()
        val stderrLogs = File(prefix + STDERR_LOGS_FILE_NAME).readText()

        return@map TestData(source, stdoutLogs, stderrLogs)
    }
}

private const val DOCKERFILE_RESOURCE = "Dockerfile"
private const val ENTRY_POINT_RESOURCE = "entrypoint.sh"
private const val TEST_DATA_RESOURCE = "test_data"
private const val TLE_ERROR_RESOURCE = "error_test_data/time_limit_exceeded"
private const val NON_ZERO_EXIT_CODE_ERROR_RESOURCE = "error_test_data/non_zero_exit_code"