package com.ces.infrastructure.docker

import com.ces.infrastructure.docker.DockerTestFixtures.Companion.TEST_IMAGE_NAME
import com.ces.infrastructure.docker.DockerTestFixtures.Companion.containerInfo
import com.ces.infrastructure.docker.DockerTestFixtures.Companion.createTestImage
import com.ces.infrastructure.docker.DockerTestFixtures.Companion.httpDockerClient
import com.ces.infrastructure.docker.DockerTestFixtures.Companion.loadResource
import com.ces.infrastructure.docker.DockerTestFixtures.Companion.removeContainer
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import java.time.Instant.EPOCH
import kotlin.time.Duration.Companion.seconds

class DockerClientTest : StringSpec({

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

    "should create container" {
        val response = docker.createContainer(TEST_IMAGE_NAME, createContainerParams)
        val containerId = response.containerId

        response.status shouldBe 201
        containerId.shouldNotBeEmpty()
        containerInfo(containerId).status shouldBe "created"

        removeContainer(containerId)
    }

    "should start container" {
        val containerId = docker.createContainer(TEST_IMAGE_NAME, createContainerParams).containerId
        val response = docker.startContainer(containerId)

        response.status shouldBe 204
        containerInfo(containerId).status shouldBe "running"

        removeContainer(containerId)
    }

    "should copy file to container" {
        val containerId = docker.createContainer(TEST_IMAGE_NAME, createContainerParams).containerId
        val sourceFile = loadResource("a_file.tar")
        val response = docker.copyFile(containerId, sourceFile.toPath(), "/home/runner")

        response.status shouldBe 200

        removeContainer(containerId)
    }

    "should read logs" {
        val containerId = docker.createContainer(TEST_IMAGE_NAME, createContainerParams).containerId
        docker.startContainer(containerId)

        eventually(3.seconds) {
            val logs = docker.containerLogs(containerId, EPOCH)
            logs.responseStatus shouldBe 200
            logs.stdout.shouldNotBeEmpty()
            logs.stdout[0].content shouldBe "Hello World\n"
        }

        removeContainer(containerId)
    }

    "should remove container" {
        val containerId = docker.createContainer(TEST_IMAGE_NAME, createContainerParams).containerId

        docker.removeContainer(containerId).responseStatus shouldBe 204
        docker.removeContainer(containerId).responseStatus shouldBe 404
    }
})

const val DOCKERFILE = "Dockerfile"
const val ENTRY_POINT = "entrypoint.sh"
const val SOURCE_CODE = "code.cs"

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