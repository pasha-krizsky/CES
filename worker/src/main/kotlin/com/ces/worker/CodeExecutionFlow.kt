package com.ces.worker

import com.ces.domain.events.CodeExecutionFinishedEvent
import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.types.*
import com.ces.domain.types.CodeExecutionFailureReason.INTERNAL_ERROR
import com.ces.domain.types.CodeExecutionFailureReason.NONE
import com.ces.domain.types.CodeExecutionState.COMPLETED
import com.ces.domain.types.CodeExecutionState.FAILED
import com.ces.infrastructure.docker.ContainerId
import com.ces.infrastructure.docker.ContainerLogsResponse
import com.ces.infrastructure.docker.CreateContainerParams
import com.ces.infrastructure.docker.Docker
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.DeliveryId
import com.ces.infrastructure.rabbitmq.Message
import com.ces.infrastructure.rabbitmq.MessageQueue
import com.ces.worker.config.ApplicationConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock.System.now
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant.EPOCH

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
            requestQueue.markUnprocessed(messageId, false)
            sendExecutionFailedEvent(request.id, INTERNAL_ERROR)
        }
    }

    private suspend fun processRequest(request: CodeExecutionRequestedEvent) {
        val codeExecutionId = request.id
        val sourceCodeFile = downloadSourceCode(codeExecutionId, request.sourceCodePath)
        val containerId = createContainer(request.language, request.compiler, sourceCodeFile.name)
        val sourceCodeTar = createTar(codeExecutionId, sourceCodeFile)

        copyCodeToContainer(containerId, sourceCodeTar)
        cleanupLocalFiles(sourceCodeFile, sourceCodeTar)
        startContainer(containerId)

        val results = streamExecutionLogs(containerId)
        sendExecutionFinishedEvent(codeExecutionId, results)

        docker.removeContainer(containerId)
    }

    private suspend inline fun fetchCodeExecutionRequest(): Pair<DeliveryId, CodeExecutionRequestedEvent> {
        val message = requestQueue.receiveMessage()
        val request = Json.decodeFromString<CodeExecutionRequestedEvent>(message.content)
        return Pair(message.deliveryId, request)
    }

    private suspend inline fun downloadSourceCode(id: CodeExecutionId, scriptRemotePath: String): File {
        val scriptLocalPath = filePath(config.localStoragePath, id.value.toString(), SOURCE_CODE_FILE_NAME)
        storage.downloadFile(config.bucketName, scriptRemotePath, scriptLocalPath)
        return File(scriptLocalPath)
    }

    private suspend fun createContainer(
        language: ProgrammingLanguage,
        compiler: CodeCompilerType,
        scriptName: String,
    ): ContainerId {
        // TODO Use language and compiler to determine runner image
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

    private fun createTar(id: CodeExecutionId, sourceCodeFile: File): File {
        val sourceCodeTarPath = filePath(config.localStoragePath, id.value.toString(), SOURCE_CODE_TAR_NAME)
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

    private suspend fun streamExecutionLogs(containerId: ContainerId): CodeExecutionResults {
        val result = withTimeoutOrNull(config.runner.codeExecutionTimeoutMillis) {
            var since = EPOCH
            do {
                val inspection = docker.inspectContainer(containerId)
                val containerStatus = inspection.containerStatus
                val newLogs = docker.containerLogs(containerId, since)
                storeLogs(newLogs)
                since = newLogs.lastTimestamp
                delay(config.runner.logsPollIntervalMillis)
            } while (containerStatus.isNotFinal())
        }
        if (result == null) {
            docker.killContainer(containerId)
        }

        return CodeExecutionResults(
            state = COMPLETED, // TODO set proper state
            exitCode = 0, // TODO set proper exit code
            failureReason = NONE // TODO set proper failure reason
        )
    }

    private fun storeLogs(logs: ContainerLogsResponse) {
        if (logs.stdout.isEmpty() && logs.stderr.isEmpty())
            return
        // TODO store logs to object storage
        println(logs.mergeToString())
    }

    private suspend fun sendExecutionFinishedEvent(id: CodeExecutionId, results: CodeExecutionResults) {
        val event = CodeExecutionFinishedEvent(id, now(), results.state, results.exitCode, results.failureReason)
        responseQueue.sendMessage(Message(Json.encodeToString(event)))
    }

    private suspend fun sendExecutionFailedEvent(id: CodeExecutionId, reason: CodeExecutionFailureReason) {
        val event = CodeExecutionFinishedEvent(id, now(), FAILED, -1, reason)
        responseQueue.sendMessage(Message(Json.encodeToString(event)))
    }

    private fun cleanupLocalFiles(vararg files: File) {
        files.forEach { it.delete() }
    }

    private fun filePath(vararg parts: String) = parts.joinToString(separator = File.separator!!)

    companion object {
        const val SOURCE_CODE_FILE_NAME = "source"
        const val SOURCE_CODE_TAR_NAME = "source_tar"
    }
}

class CodeExecutionResults(
    val state: CodeExecutionState,
    val exitCode: Int,
    val failureReason: CodeExecutionFailureReason,
)

class CodeExecutionException(message: String) : RuntimeException(message)

private fun compress(tarName: String, vararg filesToCompress: File) {
    tarArchiveOutputStream(tarName).use { out ->
        filesToCompress.forEach {
            addCompressed(out, it)
        }
    }
}

private fun tarArchiveOutputStream(name: String): TarArchiveOutputStream {
    val stream = TarArchiveOutputStream(FileOutputStream(name))
    stream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR)
    stream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
    stream.setAddPaxHeadersForNonAsciiNames(true)
    return stream
}

private fun addCompressed(out: TarArchiveOutputStream, file: File) {
    val entry = file.name
    out.putArchiveEntry(TarArchiveEntry(file, entry))
    FileInputStream(file).use { `in` -> IOUtils.copy(`in`, out) }
    out.closeArchiveEntry()
}