package com.ces.infrastructure.docker

import java.nio.file.Path
import java.time.Instant

typealias ImageName = String
typealias ContainerId = String
typealias ResponseStatus = Int

interface Docker {
    suspend fun ping(): PingResponse

    suspend fun createContainer(image: ImageName, params: CreateContainerParams): CreateContainerResponse

    suspend fun startContainer(containerId: ContainerId): StartContainerResponse

    suspend fun copyFile(containerId: ContainerId, sourceTar: Path, destination: String): CopyFileResponse

    suspend fun containerLogs(containerId: ContainerId, since: Instant): ContainerLogsResponse

    suspend fun inspectContainer(containerId: ContainerId): InspectContainerResponse

    suspend fun killContainer(containerId: ContainerId): KillContainerResponse

    suspend fun removeContainer(containerId: ContainerId): RemoveContainerResponse
}

// TODO the list of params is not full, fix it
data class CreateContainerParams(
    val cmd: String,
    val capDrop: String,
    val cgroupnsMode: String,
    val networkMode: String,
    val cpusetCpus: String,
    val cpuQuota: Long,
    val memory: Long,
    val memorySwap: Long,
)

enum class ContainerStatus {
    CREATED, RESTARTING, RUNNING, REMOVING, PAUSED, EXITED, DEAD, NOT_FOUND;

    fun isNotFinal() = !isFinal()

    private fun isFinal() = this == EXITED || this == DEAD || this == NOT_FOUND
}

abstract class AbstractResponse {
    abstract val status: ResponseStatus

    fun isInformational() = status in 100..199
    fun isSuccessful() = status in 200..299
    fun isRedirection() = status in 300..399
    fun isClientError() = status in 400..499
    fun isServerError() = status in 500..599
}

data class PingResponse(
    override val status: ResponseStatus,
    val pingResponse: String,
) : AbstractResponse()

class CreateContainerResponse(
    override val status: ResponseStatus,
    val containerId: ContainerId,
) : AbstractResponse()

class StartContainerResponse(
    override val status: ResponseStatus,
) : AbstractResponse()

class CopyFileResponse(
    override val status: ResponseStatus,
) : AbstractResponse()

data class LogChunk(val timestamp: Instant, val content: String)

data class ContainerLogsResponse(
    val responseStatus: ResponseStatus,
    val stdout: List<LogChunk>,
    val stderr: List<LogChunk>,
    val lastTimestamp: Instant,
) {
    // TODO refactor me
    fun mergeToString(): String {
        var stdoutPtr = 0
        var stderrPtr = 0
        val builder = StringBuilder()
        while (stdoutPtr != stdout.size || stderrPtr != stderr.size) {
            if (stdoutPtr == stdout.size) {
                while (stderrPtr != stderr.size) {
                    val logChunk = stderr[stderrPtr++]
                    builder.append(logChunk.content)
                }
            } else if (stderrPtr == stderr.size) {
                while (stdoutPtr != stdout.size) {
                    val logChunk = stdout[stdoutPtr++]
                    builder.append(logChunk.content)
                }
            } else {
                val outChunk = stdout[stdoutPtr]
                val errChunk = stderr[stderrPtr]
                val nextChunk = if (outChunk.timestamp > errChunk.timestamp) {
                    stderrPtr++
                    errChunk
                } else {
                    stdoutPtr++
                    outChunk
                }
                builder.append(nextChunk.content)
            }
        }
        return builder.toString()
    }
}

data class InspectContainerResponse(
    val responseStatus: ResponseStatus,
    val containerStatus: ContainerStatus,
)

data class KillContainerResponse(
    val responseStatus: ResponseStatus,
)

data class RemoveContainerResponse(
    val responseStatus: ResponseStatus,
)
