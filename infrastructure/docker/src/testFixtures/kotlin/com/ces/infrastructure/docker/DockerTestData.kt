package com.ces.infrastructure.docker

import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient.Request
import com.github.dockerjava.transport.DockerHttpClient.Request.Method.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.apache.commons.lang3.RandomStringUtils.randomAlphabetic
import java.io.File
import java.io.File.separator
import java.net.URI
import java.time.Duration

class DockerTestData {

    companion object {
        const val RUNNER_TEST_IMAGE_NAME = "test-runner-image"
        const val RUNNER_HOME_DIR = "/home/runner"
        const val CODE_EXECUTION_TIMEOUT_MILLIS = 5_000L
        const val LOGS_POLL_INTERVAL_MILLIS = 100L

        const val RUNNER_CAP_DROP = "ALL"
        const val RUNNER_CGROUPNS_MODE = "private"
        const val RUNNER_NETWORK_MODE = "none"
        const val RUNNER_CPUSET_CPUS = "1"
        const val RUNNER_CPU_QUOTA = 10000000L
        const val RUNNER_MEMORY = 100000000L
        const val RUNNER_MEMORY_SWAP = 500000000L

        val httpDockerClient: ApacheDockerHttpClient =
            ApacheDockerHttpClient.Builder()
                .dockerHost(URI(DockerConfig.socket))
                .maxConnections(10)
                .connectionTimeout(connectionTimeout)
                .responseTimeout(responseTimeout)
                .build()

        fun createTestImage(vararg files: File) {
            val tmpTarPath = "$TMP_PATH$separator${randomAlphabetic(10)}"
            compress(tmpTarPath, *files)
            val tmpTarFile = File(tmpTarPath)
            val request: Request = Request.builder()
                .method(POST)
                .path("/build?t=$RUNNER_TEST_IMAGE_NAME")
                .body(tmpTarFile.inputStream())
                .build()
            httpDockerClient.execute(request).let { response ->
                val readAllBytes = response.body.readAllBytes()
                println("Read ${readAllBytes.size} bytes")
            }
            tmpTarFile.delete()
        }

        fun removeContainer(id: ContainerId) {
            val request: Request = Request.builder()
                .method(DELETE)
                .path("/containers/$id?force=true")
                .build()
            httpDockerClient.execute(request)
        }

        data class ContainerInfo(val status: String)

        fun containerInfo(id: ContainerId): ContainerInfo {
            val request: Request = Request.builder()
                .method(GET)
                .path("/containers/$id/json")
                .build()

            val response = httpDockerClient.execute(request)

            val state = Json.parseToJsonElement(response.body.bufferedReader().use { it.readText() })
                .jsonObject["State"]
            val status = state?.jsonObject?.get("Status")?.toString()?.trim('"')
            return ContainerInfo(status!!)
        }

        fun loadResource(name: String): File {
            val resource = this::class.java.getResource("/$name")!!.file
            return File(resource)
        }
    }
}

val TMP_PATH: String = System.getProperty("java.io.tmpdir")

private val connectionTimeout = Duration.ofSeconds(10)
private val responseTimeout = Duration.ofMinutes(10)