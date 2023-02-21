package com.ces.infrastructure.docker

import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient.Request
import com.github.dockerjava.transport.DockerHttpClient.Request.Method.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.apache.commons.io.IOUtils
import java.io.InputStream
import java.math.BigInteger
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.inputStream

class DockerClient(
    private val httpClient: ApacheDockerHttpClient,
) : Docker {

    override suspend fun ping(): PingResponse = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(GET)
            .path("/_ping")
            .build()

        httpClient.execute(request).let { response ->
            return@withContext PingResponse(response.statusCode, IOUtils.toString(response.body, "UTF-8"))
        }
    }

    override suspend fun createContainer(
        image: ImageName,
        params: CreateContainerParams,
    ): CreateContainerResponse = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(POST)
            .path("/containers/create")
            .putHeader("Content-Type", "application/json")
            .body(
                """
                  { 
                    "Image": "$image",
                     "Cmd": [
                       "${params.cmd}"
                     ],
                    "HostConfig": {
                      "CapDrop": "${params.capDrop}",
                      "CgroupnsMode": "${params.cgroupnsMode}",
                      "NetworkMode": "${params.networkMode}",
                      "CpusetCpus": "${params.cpusetCpus}",
                      "CpuQuota": ${params.cpuQuota},
                      "Memory": ${params.memory},
                      "MemorySwap": ${params.memorySwap}
                    }
                  }
                  """.trimIndent().byteInputStream()
            )
            .build()

        httpClient.execute(request).let { response ->
            val body = IOUtils.toString(response.body, "UTF-8")
            return@withContext CreateContainerResponse(
                response.statusCode,
                body.containerId()
            )
        }
    }

    override suspend fun startContainer(
        containerId: ContainerId,
    ): StartContainerResponse = withContext(Dispatchers.IO) {
        val request: Request = Request.builder()
            .method(POST)
            .path("/containers/$containerId/start")
            .build()

        httpClient.execute(request).let { response ->
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
            .headers(mapOf("content-type" to "application/x-tar"))
            .body(sourceTar.inputStream())
            .build()

        httpClient.execute(request).let { response ->
            val body = IOUtils.toString(response.body, "UTF-8")
            println(body)
            return@withContext CopyFileResponse(response.statusCode)
        }
    }

    override suspend fun containerLogs(
        containerId: ContainerId,
        since: Instant,
    ): ContainerLogsResponse = withContext(Dispatchers.IO) {
        val timestamp = since.epochSecond.toString() + "." + since.nano
        val request: Request = Request.builder()
            .method(GET)
            .path("/containers/$containerId/logs?tail=all&stdout=1&stderr=1&timestamps=true&since=$timestamp")
            .build()

        httpClient.execute(request).let { response ->
            val (stdout, stderr, lastTimestamp) = parseLogs(response.body)
            return@withContext ContainerLogsResponse(
                response.statusCode,
                stdout.filter { it.timestamp > since },
                stderr.filter { it.timestamp > since },
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

        httpClient.execute(request).let { response ->
            return@withContext InspectContainerResponse(
                response.statusCode,
                containerStatus(response.body.bufferedReader().use { it.readText() })
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
            ?.let { ContainerStatus.valueOf(it.toUpperCase()) }
            ?: ContainerStatus.NOT_FOUND

    data class ParsedLogs(val stdout: List<LogChunk>, val stderr: List<LogChunk>, val lastTimestamp: Instant)

    private fun parseLogs(inputStream: InputStream): ParsedLogs {
        val stdout = mutableListOf<LogChunk>()
        val stderr = mutableListOf<LogChunk>()
        var lastTimestamp = Instant.EPOCH
        while (true) {
            val header = inputStream.readNBytes(LOG_HEADER_LENGTH)
            if (header.isEmpty())
                break
            val streamType = header[STREAM_TYPE]
            val blockSize = BigInteger(header.copyOfRange(BLOCK_SIZE_START, BLOCK_SIZE_END)).toInt()

            var timestampBuffer = ByteArray(0)
            var byte: ByteArray
            var timestampLength = 0
            while (true) {
                byte = inputStream.readNBytes(1)
                if (byte[0] != 32.toByte())
                    timestampBuffer += byte
                else break
                timestampLength++
            }

            val timestamp = Instant.parse(String(timestampBuffer))
            if (timestamp > lastTimestamp)
                lastTimestamp = timestamp
            val block = inputStream.readNBytes(blockSize - timestampLength - 1)
            val logChunk = LogChunk(timestamp, String(block))
            when (streamType) {
                STDOUT -> stdout.add(logChunk)
                STDERR -> stderr.add(logChunk)
                else -> throw IllegalStateException("Failed to recognise stream type $streamType")
            }
        }
        return ParsedLogs(stdout, stderr, lastTimestamp)
    }

    companion object {
        private const val STREAM_TYPE = 0
        private const val BLOCK_SIZE_START = 4
        private const val BLOCK_SIZE_END = 8

        private const val STDOUT: Byte = 1
        private const val STDERR: Byte = 2

        private const val LOG_HEADER_LENGTH = 8
    }
}