package com.ces.worker.docker

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.math.BigInteger
import java.nio.file.Path
import java.time.Instant
import java.time.Instant.EPOCH
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.text.Charsets.UTF_8

const val CAP_DROP = "ALL"
const val CGROUPNS_MODE = "private"
const val NETWORK_MODE = "none"
const val CPUSET_CPUS = "1"
const val CPU_QUOTA = 10000000
const val MEMORY = 100000000
const val MEMORY_SWAP = 500000000

class NettyDockerImpl(private val client: NettySocketClient) : Docker {

    override suspend fun ping(): PingResponse {
        val response = client.httpGet(v41("_ping"))
        return PingResponse(response.status, response.body.toString(UTF_8))
    }

    override suspend fun createContainer(image: ImageName, scriptName: String): CreateContainerResponse {
        val response = client.httpPost(
            v41("containers/create"),
            body = """
                { 
                   "Image": "$image",
                    "Cmd": [
                        "$scriptName"
                    ],
                   "HostConfig": {
                       "CapDrop": "$CAP_DROP",
                       "CgroupnsMode": "$CGROUPNS_MODE",
                       "NetworkMode": "$NETWORK_MODE",
                       "CpusetCpus": "$CPUSET_CPUS",
                       "CpuQuota": $CPU_QUOTA,
                       "Memory": $MEMORY,
                       "MemorySwap": $MEMORY_SWAP
                   }
                }
                """.trimIndent()
        )
        return CreateContainerResponse(response.status, response.body.asString().containerId())
    }

    override suspend fun startContainer(containerId: ContainerId): StartContainerResponse {
        val response = client.httpPost(v41("containers/$containerId/start"))
        return StartContainerResponse(response.status)
    }

    override suspend fun copyFile(
        containerId: ContainerId,
        sourceTar: Path,
        destination: Path,
    ): CopyFileResponse {
        val response = client.httpPut(
            v41("containers/$containerId/archive?path=${destination.pathString}"),
            headers = mapOf("content-type" to "application/x-tar"),
            body = sourceTar.readBytes()
        )
        return CopyFileResponse(response.status)
    }

    override suspend fun containerLogs(containerId: ContainerId, since: Instant): ContainerLogsResponse {
        val nano = since.epochSecond.toString() + "." + since.nano
        val response = client.httpGet(
            v41(
                "containers/$containerId/logs?tail=all&stdout=1&stderr=1&timestamps=true&since=$nano"
            )
        )
        val (stdout, stderr, lastTimestamp) = parseLogs(response.body)
        return ContainerLogsResponse(
            response.status,
            stdout.filter { it.timestamp > since },
            stderr.filter { it.timestamp > since },
            lastTimestamp
        )
    }

    override suspend fun inspectContainer(containerId: ContainerId): InspectContainerResponse {
        val response = client.httpGet(v41("containers/$containerId/json"))
        return InspectContainerResponse(response.status, response.body.asString().containerStatus())
    }

    override suspend fun killContainer(containerId: ContainerId): KillContainerResponse {
        val response = client.httpPost(v41("containers/$containerId/kill"))
        return KillContainerResponse(response.status)
    }

    override suspend fun removeContainer(containerId: ContainerId): RemoveContainerResponse {
        val response = client.httpDelete(v41("containers/$containerId"))
        return RemoveContainerResponse(response.status)
    }

    private fun v41(path: String) = "/v1.41/$path"

    private fun String.containerId() =
        Json.parseToJsonElement(this)
            .jsonObject["Id"]
            .toString()
            .trim('"')

    private fun String.containerStatus(): ContainerStatus =
        Json.parseToJsonElement(this)
            .jsonObject["State"]
            ?.jsonObject?.get("Status")
            ?.toString()
            ?.trim('"')
            ?.let { ContainerStatus.valueOf(it.toUpperCase()) }
            ?: ContainerStatus.NOT_FOUND

    data class ParsedLogs(val stdout: List<LogChunk>, val stderr: List<LogChunk>, val lastTimestamp: Instant)

    private fun parseLogs(body: ResponseBody): ParsedLogs {
        val inputStream = body.inputStream()
        val stdout = mutableListOf<LogChunk>()
        val stderr = mutableListOf<LogChunk>()
        var lastTimestamp = EPOCH
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