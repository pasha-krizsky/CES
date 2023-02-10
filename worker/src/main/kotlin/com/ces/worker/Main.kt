package com.ces.worker

import com.ces.worker.docker.NettyDockerImpl
import com.ces.worker.docker.NettySocketClient
import com.ces.worker.tar.compress
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.UUID.randomUUID
import kotlin.io.path.deleteIfExists

const val DOCKER_SOCKET = "/var/run/docker.sock"

const val RUNNER_IMAGE_NAME = "mono-runner"
const val RUNNER_HOME_PATH = "/home/newuser"

const val WORKER_DIR = "/tmp"
const val WORKER_SCRIPT_TAR_PATH = "$WORKER_DIR/out.tar"

const val SCRIPT_EXTENSION = ".cs"
val SCRIPT_SOURCE_CODE = """
namespace HelloWorld
{
    class Hello {
        static void Main(string[] args)
        {
            for (int i = 0; i < 10; i++) {
                System.Console.WriteLine("Hello World...");
            }
        }
    }
}
""".trimIndent()

fun main(): Unit = runBlocking {
    executeCode(SCRIPT_SOURCE_CODE)
}

suspend fun executeCode(sourceCode: String) {

    val client = NettySocketClient(DOCKER_SOCKET)
    val docker = NettyDockerImpl(client)

    val pingResponse = docker.ping()
    println(pingResponse)

    val scriptName = randomUUID().toString() + SCRIPT_EXTENSION

    val createContainerResponse = docker.createContainer(RUNNER_IMAGE_NAME, scriptName)
    println(createContainerResponse)

    val containerId = createContainerResponse.containerId
    println("containerId = $containerId")

    val sourcePath = "$WORKER_DIR/$scriptName"
    val sourceCodeFile = File(sourcePath)
    sourceCodeFile.printWriter().use { out ->
        out.print(sourceCode)
    }

    compress(WORKER_SCRIPT_TAR_PATH, sourceCodeFile)

    val sourceCodeTar = Path.of(WORKER_SCRIPT_TAR_PATH)
    val copyFileResponse = docker.copyFile(
        containerId,
        sourceCodeTar,
        Path.of(RUNNER_HOME_PATH)
    )
    println(copyFileResponse)
    sourceCodeTar.deleteIfExists()
    sourceCodeFile.delete()

    val startContainerResponse = docker.startContainer(containerId)
    println(startContainerResponse)

    delay(3000)

    val containerLogs = docker.containerLogs(containerId, Instant.EPOCH)
    println(containerLogs)

    val killContainerResponse = docker.killContainer(containerId)
    println(killContainerResponse)

    val removeContainerResponse = docker.removeContainer(containerId)
    println(removeContainerResponse)

    client.close()
}