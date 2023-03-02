package com.ces.infrastructure.docker

import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_TEST_IMAGE_NAME
import com.ces.infrastructure.docker.DockerTestData.Companion.containerInfo
import com.ces.infrastructure.docker.DockerTestData.Companion.createTestImage
import com.ces.infrastructure.docker.DockerTestData.Companion.httpDockerClient
import com.ces.infrastructure.docker.DockerTestData.Companion.loadResource
import com.ces.infrastructure.docker.DockerTestData.Companion.removeContainer
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import java.time.Instant.EPOCH
import java.util.UUID.randomUUID
import kotlin.time.Duration.Companion.seconds

class DockerClientTest : StringSpec({

    timeout = 600_000

    val docker = DockerClient(httpDockerClient)

    val dockerfile = loadResource(DOCKERFILE)
    val entrypoint = loadResource(ENTRY_POINT)
    val sourceCode = loadResource(SOURCE_CODE)

    beforeSpec {
        createTestImage(dockerfile, entrypoint, sourceCode)
    }

    "should respond to ping" {
        val response = docker.ping()

        response shouldBe PingResponse(200, "OK")
    }

    "should build and remove image" {
        val imageName = RUNNER_TEST_IMAGE_NAME + "_${randomUUID()}"
        val response = docker.buildImage(imageName, dockerfile, entrypoint, sourceCode)

        response.status shouldBe 200
        docker.removeImage(imageName).status shouldBe 200
        docker.removeImage(imageName).status shouldBe 404
    }

    "should create container" {
        val response = docker.createContainer(RUNNER_TEST_IMAGE_NAME, createContainerParams)
        val containerId = response.containerId

        response.status shouldBe 201
        containerId.shouldNotBeEmpty()
        containerInfo(containerId).status shouldBe "created"

        removeContainer(containerId)
    }

    "should start container" {
        val containerId = docker.createContainer(RUNNER_TEST_IMAGE_NAME, createContainerParams).containerId
        val response = docker.startContainer(containerId)

        response.status shouldBe 204
        containerInfo(containerId).status shouldBe "running"

        removeContainer(containerId)
    }

    "should copy file to container" {
        val containerId = docker.createContainer(RUNNER_TEST_IMAGE_NAME, createContainerParams).containerId
        val sourceFile = loadResource("a_file.tar")
        val response = docker.copyFile(containerId, sourceFile.toPath(), "/home/runner")

        response.status shouldBe 200

        removeContainer(containerId)
    }

    "should read logs" {
        val containerId = docker.createContainer(RUNNER_TEST_IMAGE_NAME, createContainerParams).containerId
        docker.startContainer(containerId)

        eventually(3.seconds) {
            val logs = docker.containerLogs(containerId, EPOCH)
            logs.status shouldBe 200
            logs.stderr.shouldBeEmpty()
            logs.stdout.shouldNotBeEmpty()
            logs.stdout[0].content shouldBe "Hello World\n"
            logs.allContent() shouldBe "Hello World\n"
        }

        removeContainer(containerId)
    }

    "should remove container" {
        val containerId = docker.createContainer(RUNNER_TEST_IMAGE_NAME, createContainerParams).containerId

        docker.removeContainer(containerId).status shouldBe 204
        docker.removeContainer(containerId).status shouldBe 404
    }
})

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
    memoryBytes = 100000000,
    memorySwapBytes = 500000000,
    kernelMemoryTcpBytes = 100000000,
    pidsLimit = 8,
    ipcMode = "none",
    nofileSoft = 128,
    nofileHard = 256,
    nprocSoft = 8,
    nprocHard = 16,
)