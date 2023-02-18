package com.ces.worker.docker

import java.nio.file.Path
import java.time.Instant

typealias ImageName = String
typealias ContainerId = String
typealias ResponseStatus = Int

interface Docker {
    suspend fun ping(): PingResponse

    suspend fun createContainer(image: ImageName, scriptName: String): CreateContainerResponse

    suspend fun startContainer(containerId: ContainerId): StartContainerResponse

    suspend fun copyFile(containerId: ContainerId, sourceTar: Path, destination: Path): CopyFileResponse

    suspend fun containerLogs(containerId: ContainerId, since: Instant): ContainerLogsResponse

    suspend fun inspectContainer(containerId: ContainerId): InspectContainerResponse

    suspend fun killContainer(containerId: ContainerId): KillContainerResponse

    suspend fun removeContainer(containerId: ContainerId): RemoveContainerResponse
}

enum class ContainerStatus {
    CREATED, RESTARTING, RUNNING, REMOVING, PAUSED, EXITED, DEAD, NOT_FOUND;

    fun isNotFinal() = !isFinal()

    private fun isFinal() = this == EXITED || this == DEAD || this == NOT_FOUND
}

data class PingResponse(
    val responseStatus: ResponseStatus,
    val pingResponse: String
)

data class CreateContainerResponse(
    val responseStatus: ResponseStatus,
    val containerId: ContainerId
)

data class StartContainerResponse(
    val responseStatus: ResponseStatus
)

data class CopyFileResponse(
    val responseStatus: ResponseStatus
)

data class LogChunk(val timestamp: Instant, val content: String)

data class ContainerLogsResponse(
    val responseStatus: ResponseStatus,
    val stdout: List<LogChunk>,
    val stderr: List<LogChunk>,
    val lastTimestamp: Instant
) {
    fun mergeToString(): String {
        var stdoutPtr = 0
        var stderrPtr = 0
        val builder = StringBuilder()
        while (stdoutPtr != stdout.size || stderrPtr != stderr.size) {
            if (stdoutPtr == stdout.size) {
                while (stderrPtr != stderr.size) {
                    val logChunk = stderr[stderrPtr++]
                    builder.append(logChunk.timestamp.toString() + " " + logChunk.content)
                }
            } else if (stderrPtr == stderr.size) {
                while (stdoutPtr != stdout.size) {
                    val logChunk = stdout[stdoutPtr++]
                    builder.append(logChunk.timestamp.toString() + " " + logChunk.content)
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
                builder.append(nextChunk.timestamp.toString() + " " + nextChunk.content)
            }
        }
        return builder.toString()
    }
}

data class InspectContainerResponse(
    val responseStatus: ResponseStatus,
    val containerStatus: ContainerStatus
)

data class KillContainerResponse(
    val responseStatus: ResponseStatus
)

data class RemoveContainerResponse(
    val responseStatus: ResponseStatus
)
