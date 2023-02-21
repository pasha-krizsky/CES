package com.ces.infrastructure.docker

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient.Request
import com.github.dockerjava.transport.DockerHttpClient.Request.Method.GET
import com.github.dockerjava.transport.DockerHttpClient.Request.Method.POST
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_STAR
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU
import org.apache.commons.compress.utils.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Duration
import java.time.Instant.EPOCH
import kotlin.time.Duration.Companion.seconds

class DockerClientTest : StringSpec({

    val dockerHost = with(OPERATING_SYSTEM_NAME) {
        when {
            contains(WINDOWS_MARK, ignoreCase = true) -> DOCKER_HOST_WINDOWS
            else -> DOCKER_HOST_UNIX
        }
    }
    val dockerfile = loadResource(DOCKERFILE)
    val entrypoint = loadResource(ENTRY_POINT)
    val sourceCode = loadResource(SOURCE_CODE)

    val httpClient = httpDockerClient(
        DefaultDockerClientConfig
            .createDefaultConfigBuilder()
            .withDockerHost(dockerHost)
            .build()
    )
    val docker = DockerClient(httpClient)

    beforeSpec {
        createImageForTests(dockerfile, entrypoint, sourceCode, httpClient)
    }

    "should respond to ping" {
        val response = docker.ping()

        response shouldBe PingResponse(200, "OK")
    }

    "should create container" {
        val response = docker.createContainer(IMAGE_NAME, createContainerParams)
        val containerId = response.containerId

        response.status shouldBe 201
        containerId.shouldNotBeEmpty()
        containerInfo(containerId, httpClient).status shouldBe "created"

        removeContainer(containerId, httpClient)
    }

    "should start container" {
        val containerId = docker.createContainer(IMAGE_NAME, createContainerParams).containerId
        val response = docker.startContainer(containerId)

        response.status shouldBe 204
        containerInfo(containerId, httpClient).status shouldBe "running"

        removeContainer(containerId, httpClient)
    }

    "should copy file to container" {
        val containerId = docker.createContainer(IMAGE_NAME, createContainerParams).containerId
        val sourceFile = loadResource("a_file.tar")
        val response = docker.copyFile(containerId, sourceFile.toPath(), "/home/runner")

        response.status shouldBe 200

        removeContainer(containerId, httpClient)
    }

    "should read logs" {
        val containerId = docker.createContainer(IMAGE_NAME, createContainerParams).containerId
        docker.startContainer(containerId)

        eventually(3.seconds) {
            val logs = docker.containerLogs(containerId, EPOCH)
            logs.responseStatus shouldBe 200
            logs.stdout.shouldNotBeEmpty()
            logs.stdout[0].content shouldBe "Hello World\n"
        }

        removeContainer(containerId, httpClient)
    }

    "should remove container" {
        val containerId = docker.createContainer(IMAGE_NAME, createContainerParams).containerId

        docker.removeContainer(containerId).responseStatus shouldBe 204
        docker.removeContainer(containerId).responseStatus shouldBe 404
    }
})

val OPERATING_SYSTEM_NAME: String = System.getProperty("os.name")

private const val IMAGE_NAME = "test-runner-image"

private const val DOCKER_HOST_WINDOWS = "npipe:////./pipe/docker_engine"
private const val DOCKER_HOST_UNIX = "unix:///var/run/docker.sock"
private const val WINDOWS_MARK = "windows"

private const val DOCKERFILE = "Dockerfile"
private const val ENTRY_POINT = "entrypoint.sh"
private const val SOURCE_CODE = "code.cs"

private val createContainerParams = CreateContainerParams(
    cmd = SOURCE_CODE,
    capDrop = "ALL",
    cgroupnsMode = "private",
    networkMode = "none",
    cpusetCpus = "1",
    cpuQuota = 10000000,
    memory = 100000000,
    memorySwap = 500000000,
)

private fun StringSpec.loadResource(name: String) =
    File(this.javaClass.getResource("/$name")!!.toURI())

private fun createImageForTests(
    testDockerfile: File,
    testRunner: File,
    testScript: File,
    client: ApacheDockerHttpClient
) {
    val tmpTarPath = "${testDockerfile.absolutePath}-tar"
    compress(tmpTarPath, testDockerfile, testRunner, testScript)
    val tmpTarFile = File(tmpTarPath)
    val request: Request = Request.builder()
        .method(POST)
        .path("/build?t=$IMAGE_NAME")
        .headers(mapOf("Content-type" to "application/x-tar"))
        .body(tmpTarFile.inputStream())
        .build()
    client.execute(request)
    tmpTarFile.delete()
}

private data class ContainerInfo(val status: String)

private fun containerInfo(id: ContainerId, client: ApacheDockerHttpClient): ContainerInfo {
    val request: Request = Request.builder()
        .method(GET)
        .path("/containers/$id/json")
        .build()

    val response = client.execute(request)

    val state = Json.parseToJsonElement(response.body.bufferedReader().use { it.readText() }).jsonObject["State"]
    val status = state?.jsonObject?.get("Status")?.toString()?.trim('"')
    return ContainerInfo(status!!)
}

private fun removeContainer(id: ContainerId, client: ApacheDockerHttpClient) {
    val request: Request = Request.builder()
        .method(Request.Method.DELETE)
        .path("/containers/$id?force=true")
        .build()
    client.execute(request)
}

private fun httpDockerClient(dockerConfig: DefaultDockerClientConfig) =
    ApacheDockerHttpClient.Builder()
        .dockerHost(dockerConfig.dockerHost)
        .maxConnections(10)
        .connectionTimeout(Duration.ofSeconds(10))
        .responseTimeout(Duration.ofMinutes(5))
        .build()

private fun compress(tarName: String, vararg filesToCompress: File) {
    tarArchiveOutputStream(tarName).use { out ->
        filesToCompress.forEach {
            addCompressed(out, it)
        }
    }
}

private fun tarArchiveOutputStream(name: String): TarArchiveOutputStream {
    val stream = TarArchiveOutputStream(FileOutputStream(name))
    stream.setBigNumberMode(BIGNUMBER_STAR)
    stream.setLongFileMode(LONGFILE_GNU)
    stream.setAddPaxHeadersForNonAsciiNames(true)
    return stream
}

private fun addCompressed(out: TarArchiveOutputStream, file: File) {
    val entry = file.name
    out.putArchiveEntry(TarArchiveEntry(file, entry))
    FileInputStream(file).use { `in` -> IOUtils.copy(`in`, out) }
    out.closeArchiveEntry()
}