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

    suspend fun copyFile(containerId: ContainerId, source: Path, destination: Path): CopyFileResponse

    suspend fun containerLogs(containerId: ContainerId, since: Instant): ContainerLogsResponse

    suspend fun inspectContainer(containerId: ContainerId): InspectContainerResponse

    suspend fun killContainer(containerId: ContainerId): KillContainerResponse

    suspend fun removeContainer(containerId: ContainerId): RemoveContainerResponse
}

enum class ContainerStatus {
    CREATED, RESTARTING, RUNNING, REMOVING, PAUSED, EXITED, DEAD, NOT_FOUND
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

data class ContainerLogsResponse(
    val responseStatus: ResponseStatus,
    val body: String
)

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
