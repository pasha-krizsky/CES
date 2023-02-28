package com.ces.infrastructure.docker

import com.ces.infrastructure.docker.ContainerStatus.NOT_FOUND
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient.Request
import com.github.dockerjava.transport.DockerHttpClient.Request.Method.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.File.separator
import java.io.InputStream
import java.math.BigInteger
import java.nio.file.Path
import java.time.Instant
import java.time.Instant.EPOCH
import java.util.UUID.randomUUID
import kotlin.io.path.inputStream
import kotlin.io.path.pathString

class DockerClient(
    private val httpClient: ApacheDockerHttpClient,
) : Docker {

    private val log = KotlinLogging.logger {}

    override suspend fun ping(): PingResponse = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(GET)
            .path("/_ping")
            .build()

        httpClient.execute(request).let { response ->
            return@withContext PingResponse(response.statusCode, IOUtils.toString(response.body, UTF_8))
        }
    }

    override suspend fun buildImage(name: ImageName, vararg files: File): BuildImageResponse =
        withContext(Dispatchers.IO) {
            val tmpTarPath = "$TMP_PATH$separator${randomUUID()}"
            compress(tmpTarPath, *files)
            val tmpTarFile = File(tmpTarPath)
            val request: Request = Request.builder()
                .method(POST)
                .path("/build?t=$name")
                .body(tmpTarFile.inputStream())
                .build()

            log.debug { "Building image: name=$name" }
            httpClient.execute(request).let { response ->
                tmpTarFile.delete()
                response.body.readAllBytes()
                return@withContext BuildImageResponse(response.statusCode)
            }
        }

    override suspend fun removeImage(name: ImageName): RemoveImageResponse = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(DELETE)
            .path("/images/$name")
            .build()

        log.debug { "Removing image: name=$name" }
        httpClient.execute(request).let { response ->
            return@withContext RemoveImageResponse(response.statusCode)
        }
    }

    override suspend fun createContainer(
        image: ImageName,
        params: CreateContainerParams,
    ): CreateContainerResponse = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(POST)
            .path("/containers/create")
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .body(createContainerRequest(image, params))
            .build()

        log.debug { "Creating container: image=$image" }
        httpClient.execute(request).let { response ->
            val body = IOUtils.toString(response.body, UTF_8)
            log.debug { "Create container response: $body" }
            return@withContext CreateContainerResponse(
                response.statusCode,
                body.containerId()
            )
        }
    }

    private fun createContainerRequest(image: ImageName, params: CreateContainerParams) =
        """
        { 
          "Image": "$image",
           "Cmd": [
             "${params.cmd}"
           ],
          "HostConfig": {
            "CapDrop": "${params.capDrop}",
            "CgroupnsMode": "${params.cgroupnsMode}",
            "SecurityOpt": [ "no-new-privileges" ],
            "NetworkMode": "${params.networkMode}",
            "CpusetCpus": "${params.cpusetCpus}",
            "CpuQuota": ${params.cpuQuota},
            "Memory": ${params.memoryBytes},
            "MemorySwap": ${params.memorySwapBytes},
            "KernelMemoryTCP": ${params.kernelMemoryTcpBytes},
            "PidsLimit": ${params.pidsLimit},
            "IpcMode": "${params.ipcMode}",
            "Ulimits": [
              {
                "Name": "nofile",
                "Soft": ${params.nofileSoft},
                "Hard": ${params.nofileHard}
              },
              {
                "Name": "nproc",
                "Soft": ${params.nprocSoft},
                "Hard": ${params.nprocHard}
              }
            ]
          }
        }
        """.trimIndent().byteInputStream()

    override suspend fun startContainer(
        containerId: ContainerId,
    ): StartContainerResponse = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(POST)
            .path("/containers/$containerId/start")
            .build()

        log.debug { "Starting container: containerId=${containerId}" }
        httpClient.execute(request).let { response ->
            log.debug { "Starting container response: ${IOUtils.toString(response.body, UTF_8)}" }
            return@withContext StartContainerResponse(response.statusCode)
        }
    }

    override suspend fun copyFile(
        containerId: ContainerId,
        sourceTar: Path,
        destination: String,
    ): CopyFileResponse = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(PUT)
            .path("/containers/$containerId/archive?path=$destination")
            .headers(mapOf(CONTENT_TYPE to APPLICATION_X_TAR))
            .body(sourceTar.inputStream())
            .build()

        log.debug { "Copying file to container: containerId=${containerId}, source=${sourceTar.pathString}, destination=$destination" }
        httpClient.execute(request).let { response ->
            return@withContext CopyFileResponse(response.statusCode)
        }
    }

    override suspend fun containerLogs(
        containerId: ContainerId,
        after: Instant,
    ): ContainerLogsResponse = withContext(Dispatchers.IO) {
        val timestamp = after.epochSecond.toString() + "." + after.nano
        val request: Request = Request.builder()
            .method(GET)
            .path("/containers/$containerId/logs?tail=all&stdout=1&stderr=1&timestamps=true&since=$timestamp")
            .build()

        log.debug { "Requesting container logs: containerId=${containerId}, after=$after" }
        httpClient.execute(request).let { response ->
            val (all, stdout, stderr, lastTimestamp) = parseLogs(response.body)
            return@withContext ContainerLogsResponse(
                response.statusCode,
                all.filter { it.timestamp > after },
                stdout.filter { it.timestamp > after },
                stderr.filter { it.timestamp > after },
                lastTimestamp
            )
        }
    }

    override suspend fun inspectContainer(
        containerId: ContainerId,
    ) = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(GET)
            .path("/containers/$containerId/json")
            .build()

        log.debug { "Inspecting container: containerId=${containerId}" }
        httpClient.execute(request).let { response ->
            val body = IOUtils.toString(response.body, UTF_8)
            return@withContext InspectContainerResponse(
                response.statusCode,
                containerStatus(body),
                containerExitCode(body)
            )
        }
    }

    override suspend fun killContainer(
        containerId: ContainerId
    ): KillContainerResponse = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(POST)
            .path("/containers/$containerId/kill")
            .build()

        log.debug { "Killing container: containerId=${containerId}" }
        httpClient.execute(request).let { response ->
            return@withContext KillContainerResponse(response.statusCode)
        }
    }

    override suspend fun removeContainer(
        containerId: ContainerId,
    ): RemoveContainerResponse = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(DELETE)
            .path("/containers/$containerId")
            .build()

        log.debug { "Removing container: containerId=${containerId}" }
        httpClient.execute(request).let { response ->
            return@withContext RemoveContainerResponse(response.statusCode)
        }
    }

    private fun String.containerId() =
        Json.parseToJsonElement(this)
            .jsonObject["Id"]
            .toString()
            .trim('"')

    private fun containerStatus(response: String): ContainerStatus =
        Json.parseToJsonElement(response)
            .jsonObject["State"]
            ?.jsonObject?.get("Status")
            ?.toString()
            ?.trim('"')
            ?.let { ContainerStatus.valueOf(it.uppercase()) }
            ?: NOT_FOUND

    private fun containerExitCode(response: String): Int? =
        Json.parseToJsonElement(response)
            .jsonObject["State"]?.jsonObject?.get("ExitCode")?.toString()?.toInt()

    data class ParsedLogs(
        val all: List<LogChunk>,
        val stdout: List<LogChunk>,
        val stderr: List<LogChunk>,
        val lastTimestamp: Instant
    )

    private fun parseLogs(inputStream: InputStream): ParsedLogs {
        val all = mutableListOf<LogChunk>()
        val stdout = mutableListOf<LogChunk>()
        val stderr = mutableListOf<LogChunk>()
        var lastTimestamp = EPOCH
        while (true) {
            val header = inputStream.readNBytes(LOG_HEADER_LENGTH)
            if (header.isEmpty())
                break
            val streamType = header[STREAM_TYPE]
            val blockSize = BigInteger(header.copyOfRange(BLOCK_SIZE_START, BLOCK_SIZE_END)).toInt()

            val timestamp = Instant.parse(String(inputStream.readNBytes(LOG_TIMESTAMP_LENGTH)))
            inputStream.readNBytes(1)
            if (timestamp > lastTimestamp)
                lastTimestamp = timestamp
            val block = inputStream.readNBytes(blockSize - LOG_TIMESTAMP_LENGTH - 1)
            val logChunk = LogChunk(timestamp, String(block))
            all.add(logChunk)
            when (streamType) {
                STDOUT -> stdout.add(logChunk)
                STDERR -> stderr.add(logChunk)
                else -> throw IllegalStateException("Failed to recognise stream type $streamType")
            }
        }
        return ParsedLogs(all, stdout, stderr, lastTimestamp)
    }

    companion object {
        private val TMP_PATH: String = System.getProperty("java.io.tmpdir")

        private const val CONTENT_TYPE = "Content-Type"
        private const val APPLICATION_JSON = "application/json"
        private const val APPLICATION_X_TAR = "application/x-tar"
        private const val UTF_8 = "UTF-8"

        private const val STREAM_TYPE = 0
        private const val STDOUT: Byte = 1
        private const val STDERR: Byte = 2

        private const val BLOCK_SIZE_START = 4
        private const val BLOCK_SIZE_END = 8

        private const val LOG_HEADER_LENGTH = 8
        private const val LOG_TIMESTAMP_LENGTH = 30
    }
}