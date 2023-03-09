package com.ces.worker.flow

import com.ces.domain.entities.CodeExecution.Companion.ALL_LOGS_FILE_NAME
import com.ces.domain.entities.CodeExecution.Companion.STDERR_LOGS_FILE_NAME
import com.ces.domain.entities.CodeExecution.Companion.STDOUT_LOGS_FILE_NAME
import com.ces.domain.events.CodeExecutionFinishedEvent
import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.events.CodeExecutionStartedEvent
import com.ces.domain.json.JsonConfig.Companion.decodeCodeExecutionEvent
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeExecutionFailureReason
import com.ces.domain.types.CodeExecutionFailureReason.*
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionLogsPath
import com.ces.domain.types.CodeExecutionState
import com.ces.domain.types.CodeExecutionState.COMPLETED
import com.ces.domain.types.CodeExecutionState.FAILED
import com.ces.infrastructure.docker.*
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.DeliveryId
import com.ces.infrastructure.rabbitmq.Message
import com.ces.infrastructure.rabbitmq.ReceiveQueue
import com.ces.infrastructure.rabbitmq.SendQueue
import com.ces.worker.config.WorkerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock.System.now
import mu.KotlinLogging
import java.io.File
import java.io.File.separator
import java.time.Instant.EPOCH
import java.util.UUID.randomUUID

class CodeExecutionFlow(
    private val config: WorkerConfig,
    private val docker: Docker,
    private val requestQueue: ReceiveQueue,
    private val responseQueue: SendQueue,
    private val storage: ObjectStorage,
) {

    private val log = KotlinLogging.logger {}

    suspend fun run() {
        val (messageId, request) = fetchCodeExecutionRequest()
        log.debug { "Start processing code execution request, messageId=$$messageId" }
        try {
            processRequest(request)
            requestQueue.markProcessed(messageId)
            log.debug { "Finished processing code execution request, messageId=$messageId" }
        } catch (e: Exception) {
            log.error(e) { "Failed to process code execution request" }
            requestQueue.markUnprocessed(messageId, false)
            sendExecutionFailedEvent(request.id, INTERNAL_ERROR)
        }
    }

    private suspend fun processRequest(request: CodeExecutionRequestedEvent) {
        val codeExecutionId = request.id
        val logsPath = codeExecutionLogsPathFor(codeExecutionId)
        sendExecutionStartedEvent(codeExecutionId, logsPath)

        val sourceCodeFile = downloadSourceCode(codeExecutionId, request.sourceCodePath)
        val containerId = createContainer(sourceCodeFile.name)
        val sourceCodeTar = createTar(sourceCodeFile)

        copyCodeToContainer(containerId, sourceCodeTar)
        cleanupLocalFiles(sourceCodeFile, sourceCodeTar)
        startContainer(containerId)

        val results = streamExecutionLogs(logsPath, containerId)
        sendExecutionFinishedEvent(codeExecutionId, results)

        docker.removeContainer(containerId)
    }

    private suspend fun sendExecutionStartedEvent(
        codeExecutionId: CodeExecutionId,
        logsPath: CodeExecutionLogsPath
    ) {
        val startedEvent: CodeExecutionStartedEvent = CodeExecutionStartedEvent.builder {
            id = codeExecutionId
            createdAt = now()
            this.logsPath = logsPath
        }.build()
        responseQueue.sendMessage(Message(encodeCodeExecutionEvent(startedEvent)))
    }

    private suspend inline fun fetchCodeExecutionRequest(): Pair<DeliveryId, CodeExecutionRequestedEvent> {
        val message = requestQueue.receiveMessage()
        val request = decodeCodeExecutionEvent(message.content) as CodeExecutionRequestedEvent
        return Pair(message.deliveryId, request)
    }

    private suspend inline fun downloadSourceCode(id: CodeExecutionId, scriptRemotePath: String): File {
        val localDestination = WorkerConfig.tmpDir + separator + id.value
        return storage.get(config.codeExecutionBucketName, scriptRemotePath, localDestination)
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
                memoryBytes = memory,
                memorySwapBytes = memorySwap,
                kernelMemoryTcpBytes = kernelMemoryTcpBytes,
                pidsLimit = pidsLimit,
                ipcMode = ipcMode,
                nofileSoft = nofileSoft,
                nofileHard = nofileHard,
                nprocSoft = nprocSoft,
                nprocHard = nprocHard,
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
        logsPath: CodeExecutionLogsPath,
        containerId: ContainerId
    ): CodeExecutionResults = withContext(Dispatchers.IO) {
        val result = withTimeoutOrNull(config.runner.codeExecutionTimeoutMillis) {
            var after = EPOCH
            do {
                val inspection = docker.inspectContainer(containerId)
                val containerStatus = inspection.containerStatus
                val newLogs = docker.containerLogs(containerId, after)

                storeLogs(logsPath, newLogs)

                after = newLogs.lastTimestamp
                delay(config.runner.logsPollIntervalMillis)
            } while (containerStatus.isNotFinal())
        }
        val finishedInTime = result != null
        if (!finishedInTime) {
            docker.killContainer(containerId)
        }

        val exitCode = docker.inspectContainer(containerId).exitCode!!
        return@withContext CodeExecutionResults(
            state = if (!finishedInTime || exitCode != 0) FAILED else COMPLETED,
            failureReason = failureReasonFrom(finishedInTime, exitCode),
            exitCode = exitCode,
        )
    }

    private fun failureReasonFrom(finishedInTime: Boolean, exitCode: Int) =
        if (!finishedInTime)
            TIME_LIMIT_EXCEEDED
        else if (exitCode != 0)
            NON_ZERO_EXIT_CODE
        else
            NONE

    private suspend fun storeLogs(logsPath: CodeExecutionLogsPath, logs: ContainerLogsResponse) {
        if (logs.stdout.isEmpty() && logs.stderr.isEmpty())
            return

        storeLogs(logs.allContent(), logsPath.allPath)
        storeLogs(logs.stdoutContent(), logsPath.stdoutPath)
        storeLogs(logs.stderrContent(), logsPath.stderrPath)
    }

    private suspend fun storeLogs(logsContent: String, path: String) =
        withContext(Dispatchers.IO) {
            val tmpLocalPath = WorkerConfig.tmpDir + separator + randomUUID()
            val file = storage.get(config.codeExecutionBucketName, path, tmpLocalPath)
            file.appendText(logsContent)

            storage.upload(config.codeExecutionBucketName, tmpLocalPath, path)
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

    private suspend fun codeExecutionLogsPathFor(codeExecutionId: CodeExecutionId): CodeExecutionLogsPath {
        val allPath = "${codeExecutionId.value}/$ALL_LOGS_FILE_NAME"
        val stdoutPath = "${codeExecutionId.value}/$STDOUT_LOGS_FILE_NAME"
        val stderrPath = "${codeExecutionId.value}/$STDERR_LOGS_FILE_NAME"

        createEmptyObjects(allPath, stdoutPath, stderrPath)

        return CodeExecutionLogsPath(allPath, stdoutPath, stderrPath)
    }

    private suspend fun createEmptyObjects(vararg paths: String) {
        paths.forEach { path ->
            val tmpLocalPath = WorkerConfig.tmpDir + separator + randomUUID()
            val tmpFile = File(tmpLocalPath)
            tmpFile.appendText("")
            storage.upload(config.codeExecutionBucketName, tmpLocalPath, path)
            tmpFile.delete()
        }
    }

    companion object {
        const val TAR_SUFFIX: String = "_tar"
    }
}

class CodeExecutionResults(
    val state: CodeExecutionState,
    val failureReason: CodeExecutionFailureReason,
    val exitCode: Int,
)

class CodeExecutionException(message: String) : RuntimeException(message)