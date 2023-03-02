package com.ces.infrastructure.docker

import java.io.File
import java.nio.file.Path
import java.time.Instant

typealias ImageName = String
typealias ContainerId = String
typealias ResponseStatus = Int

interface Docker {
    suspend fun ping(): PingResponse

    suspend fun buildImage(name: ImageName, vararg files: File): BuildImageResponse

    suspend fun removeImage(name: ImageName): RemoveImageResponse

    suspend fun createContainer(image: ImageName, params: CreateContainerParams): CreateContainerResponse

    suspend fun startContainer(containerId: ContainerId): StartContainerResponse

    suspend fun copyFile(containerId: ContainerId, sourceTar: Path, destination: String): CopyFileResponse

    suspend fun containerLogs(containerId: ContainerId, after: Instant): ContainerLogsResponse

    suspend fun inspectContainer(containerId: ContainerId): InspectContainerResponse

    suspend fun killContainer(containerId: ContainerId): KillContainerResponse

    suspend fun removeContainer(containerId: ContainerId): RemoveContainerResponse
}

// TODO --storage-opt should be added but it requires some pre-configuration of host (adding pquota to /etc/default/grub)
// TODO Consider limiting IO OPS for reading from/writing to device
data class CreateContainerParams(
    val cmd: String,
    val capDrop: String,
    val cgroupnsMode: String,
    val networkMode: String,
    val cpusetCpus: String,
    val cpuQuota: Long,
    val memoryBytes: Long,
    val memorySwapBytes: Long,
    val kernelMemoryTcpBytes: Long,
    val pidsLimit: Int,
    val ipcMode: String,
    val nofileSoft: Int,
    val nofileHard: Int,
    val nprocSoft: Int,
    val nprocHard: Int,
)

enum class ContainerStatus {
    CREATED, RESTARTING, RUNNING, REMOVING, PAUSED, EXITED, DEAD, NOT_FOUND;

    fun isNotFinal() = !isFinal()

    private fun isFinal() = this == EXITED || this == DEAD || this == NOT_FOUND
}

abstract class AbstractResponse {
    abstract val status: ResponseStatus
    fun isSuccessful() = status in 200..299
}

data class PingResponse(override val status: ResponseStatus, val pingResponse: String) : AbstractResponse()

class BuildImageResponse(override val status: ResponseStatus) : AbstractResponse()

data class RemoveImageResponse(val status: ResponseStatus)

class CreateContainerResponse(
    override val status: ResponseStatus,
    val containerId: ContainerId,
) : AbstractResponse()

class StartContainerResponse(override val status: ResponseStatus) : AbstractResponse()

class CopyFileResponse(override val status: ResponseStatus) : AbstractResponse()

data class LogChunk(val timestamp: Instant, val content: String)

data class ContainerLogsResponse(
    override val status: ResponseStatus,
    val all: List<LogChunk>,
    val stdout: List<LogChunk>,
    val stderr: List<LogChunk>,
    val lastTimestamp: Instant,
) : AbstractResponse() {
    fun allContent() = all.joinToString(separator = "") { it.content }
    fun stdoutContent() = stdout.joinToString(separator = "") { it.content }
    fun stderrContent() = stderr.joinToString(separator = "") { it.content }
}

data class InspectContainerResponse(
    override val status: ResponseStatus,
    val containerStatus: ContainerStatus,
    val exitCode: Int?,
) : AbstractResponse()

data class KillContainerResponse(override val status: ResponseStatus) : AbstractResponse()

data class RemoveContainerResponse(override val status: ResponseStatus) : AbstractResponse()