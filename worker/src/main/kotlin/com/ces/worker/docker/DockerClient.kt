package com.ces.worker.docker

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.nio.file.Path
import java.time.Instant
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
        source: Path,
        destination: Path,
    ): CopyFileResponse {
        val response = client.httpPut(
            v41("containers/$containerId/archive?path=${destination.pathString}"),
            headers = mapOf("content-type" to "application/x-tar"),
            body = source.readBytes()
        )
        return CopyFileResponse(response.status)
    }

    override suspend fun containerLogs(containerId: ContainerId, since: Instant): ContainerLogsResponse {
        val response = client.httpGet(v41("containers/$containerId/logs?stdout=1&stderr=1"))
        return ContainerLogsResponse(response.status, response.body.asString())
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
            ?.let { ContainerStatus.valueOf(it) }
            ?: ContainerStatus.NOT_FOUND
}

// TODO Parse logs
//val inputStream = response.body
//val stdin = StringBuilder()
//val stderr = StringBuilder()
//while (true) {
//    val header = inputStream.readNBytes(8)
//    if (header.isEmpty())
//        break
////                println("header: ${IOUtils.toString(header, "UTF-8")}")
//    val streamType = header[0]
////                println("streamType: $streamType")
//    val bytesToSkip = header.copyOfRange(1, 4)
////                println("bytesToSkip: $bytesToSkip")
//    val blockSize = BigInteger(header.copyOfRange(4, 8)).toInt()
////                println("blockSize: $blockSize")
//    val block = inputStream.readNBytes(blockSize)
////                println("block: $block")
////                println("convertedBlock: ${IOUtils.toString(block.inputStream(), CHARSET)}")
//    stdin.append(IOUtils.toString(block.inputStream(), CHARSET))
//}