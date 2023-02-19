package com.ces.worker

import com.ces.worker.infra.docker.ContainerLogsResponse
import com.ces.worker.infra.docker.NettyDockerImpl
import com.ces.worker.infra.docker.NettySocketClient
import com.ces.worker.infra.tar.compress
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.file.Path
import java.time.Instant.EPOCH
import java.util.UUID.randomUUID
import kotlin.io.path.deleteIfExists


val SCRIPT_SOURCE_CODE_2 = """
namespace HelloWorld
{
    class Hello {
        static void Main(string[] args)
        {
            for (int i = 0; i < 10; i++) {
                System.Console.WriteLine("Hello World " + i);
                System.Console.Error.WriteLine("Hello Error " + i);
                System.Threading.Thread.Sleep(1000);
            }
        }
    }
}
""".trimIndent()

fun main(): Unit = runBlocking {
    executeCode(SCRIPT_SOURCE_CODE)
}

suspend fun executeCode(sourceCode: String) {

    println("Start execution...")
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

    val result = withTimeoutOrNull(CODE_EXECUTION_TIMEOUT) {
        var logsTimestamp = EPOCH
        do {
            val inspectContainerResponse = docker.inspectContainer(containerId)
            val containerStatus = inspectContainerResponse.containerStatus
            val logs = docker.containerLogs(containerId, logsTimestamp)

            sendLogs(logs)
            logsTimestamp = logs.lastTimestamp
            delay(50)
        } while (containerStatus.isNotFinal())
    }
    if (result == null) {
        val killContainerResponse = docker.killContainer(containerId)
        println(killContainerResponse)
    }

    val removeContainerResponse = docker.removeContainer(containerId)
    println(removeContainerResponse)

    client.close()
}

fun sendLogs(logs: ContainerLogsResponse) {
    if (logs.stdout.isEmpty() && logs.stderr.isEmpty())
        return
    println(logs.mergeToString())
}
