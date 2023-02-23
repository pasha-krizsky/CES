package com.ces.worker

import com.ces.domain.events.CodeExecutionFinishedEvent
import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.events.CodeExecutionStartedEvent
import com.ces.domain.json.JsonConfig.Companion.decodeCodeExecutionEvent
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeExecutionFailureReason
import com.ces.domain.types.CodeExecutionFailureReason.*
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState
import com.ces.domain.types.CodeExecutionState.COMPLETED
import com.ces.domain.types.CodeExecutionState.FAILED
import com.ces.infrastructure.docker.*
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.DeliveryId
import com.ces.infrastructure.rabbitmq.Message
import com.ces.infrastructure.rabbitmq.MessageQueue
import com.ces.worker.config.ApplicationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock.System.now
import java.io.File
import java.io.File.separator
import java.time.Instant.EPOCH
import java.util.UUID.randomUUID

class CodeExecutionFlow(
    private val config: ApplicationConfig,
    private val docker: Docker,
    private val requestQueue: MessageQueue,
    private val responseQueue: MessageQueue,
    private val storage: ObjectStorage,
) {
    suspend fun run() {
        val (messageId, request) = fetchCodeExecutionRequest()
        try {
            processRequest(request)
            requestQueue.markProcessed(messageId)
        } catch (e: Exception) {
            e.printStackTrace()
            requestQueue.markUnprocessed(messageId, false)
            sendExecutionFailedEvent(request.id, INTERNAL_ERROR)
        }
    }

    private suspend fun processRequest(request: CodeExecutionRequestedEvent) {
        val codeExecutionId = request.id
        val resultsPath = "${codeExecutionId.value}/$RESULTS_FILE_NAME"
        sendExecutionStartedEvent(codeExecutionId, resultsPath)

        val sourceCodeFile = downloadSourceCode(codeExecutionId, request.sourceCodePath)
        val containerId = createContainer(sourceCodeFile.name)
        val sourceCodeTar = createTar(sourceCodeFile)

        copyCodeToContainer(containerId, sourceCodeTar)
        cleanupLocalFiles(sourceCodeFile, sourceCodeTar)
        startContainer(containerId)

        val results = streamExecutionLogs(resultsPath, containerId)
        sendExecutionFinishedEvent(codeExecutionId, results)

        docker.removeContainer(containerId)
    }

    private suspend fun sendExecutionStartedEvent(codeExecutionId: CodeExecutionId, resultsPath: String) {
        val startedEvent = CodeExecutionStartedEvent.builder {
            id = codeExecutionId
            createdAt = now()
            executionLogsPath = resultsPath
        }.build()
        responseQueue.sendMessage(Message(encodeCodeExecutionEvent(startedEvent)))
    }

    private suspend inline fun fetchCodeExecutionRequest(): Pair<DeliveryId, CodeExecutionRequestedEvent> {
        val message = requestQueue.receiveMessage()
        val request = decodeCodeExecutionEvent(message.content) as CodeExecutionRequestedEvent
        return Pair(message.deliveryId, request)
    }

    private suspend inline fun downloadSourceCode(id: CodeExecutionId, scriptRemotePath: String) =
        withContext(Dispatchers.IO) {
            val localDestination = WORKER_TMP_DIR + separator + id.value
            storage.downloadFile(config.bucketName, scriptRemotePath, localDestination)
            return@withContext File(localDestination)
        }

    private suspend fun createContainer(scriptName: String): ContainerId {
        val response = docker.createContainer(config.runner.imageName, createContainerParams(scriptName))
        if (response.isSuccessful())
            return response.containerId
        throw CodeExecutionException("Failed to create container, got ${response.status} response status")
    }

    private fun createContainerParams(sourceCodePath: String) = run {
        with(config.runner.container) {
            return@with CreateContainerParams(
                cmd = sourceCodePath,
                capDrop = capDrop,
                cgroupnsMode = cgroupnsMode,
                networkMode = networkMode,
                cpusetCpus = cpusetCpus,
                cpuQuota = cpuQuota,
                memory = memory,
                memorySwap = memorySwap,
            )
        }
    }

    private fun createTar(sourceCodeFile: File): File {
        val sourceCodeTarPath = sourceCodeFile.absolutePath + TAR_SUFFIX
        compress(sourceCodeTarPath, sourceCodeFile)
        return File(sourceCodeTarPath)
    }

    private suspend fun copyCodeToContainer(containerId: ContainerId, sourceCodeTar: File) {
        val response = docker.copyFile(containerId, sourceCodeTar.toPath(), config.runner.workDir)
        if (!response.isSuccessful())
            throw CodeExecutionException("Failed to copy file to container, got ${response.status} response status")
    }

    private suspend fun startContainer(containerId: ContainerId) {
        val response = docker.startContainer(containerId)
        if (!response.isSuccessful())
            throw CodeExecutionException("Failed to start container, got ${response.status} response status")
    }

    private suspend fun streamExecutionLogs(
        resultsPath: String,
        containerId: ContainerId
    ): CodeExecutionResults {

        val result = withTimeoutOrNull(config.runner.codeExecutionTimeoutMillis) {
            var since = EPOCH
            do {
                val inspection = docker.inspectContainer(containerId)
                val containerStatus = inspection.containerStatus
                val newLogs = docker.containerLogs(containerId, since)

                storeLogs(since == EPOCH, resultsPath, newLogs)

                since = newLogs.lastTimestamp
                delay(config.runner.logsPollIntervalMillis)
            } while (containerStatus.isNotFinal())
        }
        if (result == null) {
            docker.killContainer(containerId)
        }

        return CodeExecutionResults(
            state = if (result == null) FAILED else COMPLETED,
            exitCode = 0, // TODO set proper exit code
            failureReason = if (result == null) TIME_LIMIT_EXCEEDED else NONE
        )
    }

    private suspend fun storeLogs(firstChunk: Boolean, resultsPath: String, logs: ContainerLogsResponse) =
        withContext(Dispatchers.IO) {
            if (logs.stdout.isEmpty() && logs.stderr.isEmpty())
                return@withContext

            val tmpLocalDestination = WORKER_TMP_DIR + separator + randomUUID()
            val file = if (!firstChunk)
                storage.downloadFile(config.bucketName, resultsPath, tmpLocalDestination)
            else File(tmpLocalDestination)
            val textLogs = logs.mergeToString()
            file.appendText(textLogs)

            storage.uploadFile(config.bucketName, tmpLocalDestination, resultsPath)
            file.delete()
        }

    private suspend fun sendExecutionFinishedEvent(id: CodeExecutionId, results: CodeExecutionResults) {
        val event = CodeExecutionFinishedEvent(id, now(), results.state, results.exitCode, results.failureReason)
        responseQueue.sendMessage(Message(encodeCodeExecutionEvent(event)))
    }

    private suspend fun sendExecutionFailedEvent(id: CodeExecutionId, reason: CodeExecutionFailureReason) {
        val event = CodeExecutionFinishedEvent(id, now(), FAILED, -1, reason)
        responseQueue.sendMessage(Message(encodeCodeExecutionEvent(event)))
    }

    private fun cleanupLocalFiles(vararg files: File) {
        files.forEach { it.delete() }
    }

    companion object {
        const val TAR_SUFFIX: String = "_tar"
        const val RESULTS_FILE_NAME = "results"
        val WORKER_TMP_DIR: String = System.getProperty("java.io.tmpdir")
    }
}

class CodeExecutionResults(
    val state: CodeExecutionState,
    val exitCode: Int,
    val failureReason: CodeExecutionFailureReason,
)

class CodeExecutionException(message: String) : RuntimeException(message)