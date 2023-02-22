package com.ces.infrastructure.docker

import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient.Request
import com.github.dockerjava.transport.DockerHttpClient.Request.Method.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.apache.commons.lang3.RandomStringUtils.randomAlphabetic
import java.io.File
import java.net.URI
import java.time.Duration

class DockerTestFixtures {

    companion object {
        const val TEST_IMAGE_NAME = "test-runner-image"

        private val dockerHost = with(OS_NAME) {
            when {
                contains(WINDOWS, ignoreCase = true) -> DOCKER_HOST_WINDOWS
                else -> DOCKER_HOST_UNIX
            }
        }

        val httpDockerClient: ApacheDockerHttpClient =
            ApacheDockerHttpClient.Builder()
                .dockerHost(URI(dockerHost))
                .maxConnections(10)
                .connectionTimeout(connectionTimeout)
                .responseTimeout(responseTimeout)
                .build()

        fun createTestImage(dockerfile: File, entrypoint: File, sourceCode: File) {
            val tmpTarPath = "$TMP_PATH$SEPARATOR${randomAlphabetic(10)}"
            compress(tmpTarPath, dockerfile, entrypoint, sourceCode)
            val tmpTarFile = File(tmpTarPath)
            val request: Request = Request.builder()
                .method(POST)
                .path("/build?t=$TEST_IMAGE_NAME")
                .body(tmpTarFile.inputStream())
                .build()
            httpDockerClient.execute(request)
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

private const val WINDOWS = "windows"

val OS_NAME: String = System.getProperty("os.name")
val TMP_PATH: String = System.getProperty("java.io.tmpdir")
val SEPARATOR: String = File.separator

private const val DOCKER_HOST_WINDOWS = "npipe:////./pipe/docker_engine"
private const val DOCKER_HOST_UNIX = "unix:///var/run/docker.sock"

private val connectionTimeout = Duration.ofSeconds(10)
private val responseTimeout = Duration.ofMinutes(10)