package com.ces.worker

import com.ces.worker.domain.events.CodeExecutionRequestedEvent
import com.ces.worker.domain.types.CodeExecutionId
import com.ces.worker.infra.docker.NettyDockerImpl
import com.ces.worker.infra.docker.NettySocketClient
import com.ces.worker.infra.queue.Message
import com.ces.worker.infra.queue.RabbitMessageQueue
import com.ces.worker.infra.storage.MinioStorage
import com.ces.worker.infra.tar.compress
import io.minio.MinioAsyncClient
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock.System.now
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

const val MIN_IO_ENDPOINT = "http://127.0.0.1:9000"

const val ACCESS_KEY = "minioadmin"
const val SECRET_KEY = "minioadmin"

const val BUCKET_NAME = "code_executions"

const val OBJECT_PATH = "test/commands.txt"
const val FILE_TO_UPLOAD = "E:\\projects\\CES\\worker\\commands.txt"
const val FILE_TO_UPLOAD_2 = "E:\\projects\\CES\\worker\\commands-2.txt"
const val DOWNLOADED_FILE = "E:\\projects\\CES\\worker\\commands-output.txt"

const val SCRIPT_EXTENSION = ".cs"

const val CODE_EXECUTION_TIMEOUT = 5_000L

const val DOCKER_SOCKET = "/var/run/docker.sock"

const val RUNNER_IMAGE_NAME = "runner-mono"
const val RUNNER_HOME_PATH = "/home/newuser"

const val WORKER_DIR = "/tmp"
const val WORKER_SCRIPT_TAR_PATH = "$WORKER_DIR/out.tar"

val SCRIPT_SOURCE_CODE = """
namespace HelloWorld
{
    class Hello {
        static void Main(string[] args)
        {
            for (int i = 0; i < 10; i++) {
                System.Console.WriteLine("Hello World " + i);
                System.Console.Error.WriteLine("Hello Error " + i);
            }
        }
    }
}
""".trimIndent()

class Test {
    suspend fun test(sourceCode: String) {
        val codeExecutionId = CodeExecutionId.random()

        // 1. Create tar
        println("Preparing source code...")
        val sourcePath = "E:\\projects\\CES\\worker\\src\\main\\resources\\${codeExecutionId.value}.cs"
        val sourceCodeFile = File(sourcePath)
        sourceCodeFile.printWriter().use { out ->
            out.print(sourceCode)
        }

        val tarLocalPath = "E:\\projects\\CES\\worker\\src\\main\\resources\\${codeExecutionId.value}.tar"
        compress(tarLocalPath, sourceCodeFile)

        // 2. Store tar
        println("Uploading source code to Object Storage...")
        val minioClient: MinioAsyncClient = MinioAsyncClient.builder()
            .endpoint("http://127.0.0.1:9000")
            .credentials("minioadmin", "minioadmin")
            .build()
        val minioStorage = MinioStorage(minioClient)
        val bucketName = "code-executions"
        minioStorage.createBucket(bucketName)
        val sourceCodePath = "$codeExecutionId/source.cs"
        minioStorage.uploadFile(bucketName, tarLocalPath, sourceCodePath)

        // 3. Sending CodeExecutionRequestedEvent to RabbitMQ
        println("Sending CodeExecutionRequestedEvent to RabbitMQ")
        val queueName = "code-execution-requests"
        val connectionName = "amqp://guest:guest@localhost:5672"
        val queue = RabbitMessageQueue(queueName, connectionName)
        val codeExecutionRequestedEvent = CodeExecutionRequestedEvent(codeExecutionId, now(), sourceCodePath)
        queue.sendMessage(Message(Json.encodeToString(codeExecutionRequestedEvent)))

        // 4. Receiving CodeExecutionRequestedEvent from RabbitMQ
        println("Receiving CodeExecutionRequestedEvent from RabbitMQ")
        val message = queue.receiveMessage()
        val deserialized = Json.decodeFromString<CodeExecutionRequestedEvent>(message.content)
        println("Got message: $deserialized")

        // 5. Downloading file with source code
        println("Downloading file with source code")
        val downloadedPath = "E:\\projects\\CES\\worker\\src\\main\\resources\\${codeExecutionId.value}-downloaded.tar"
        minioStorage.downloadFile(bucketName, deserialized.sourceCodePath, downloadedPath)

        // 6. Creating Docker container
        val nettySocketClient = NettySocketClient(DOCKER_SOCKET)
        val docker = NettyDockerImpl(nettySocketClient)

        val createContainerResponse = docker.createContainer(RUNNER_IMAGE_NAME, "${codeExecutionId.value}.cs")
        println(createContainerResponse)

        // TODO

        // Cleanup
        queue.close()
    }
}

fun main(): Unit = runBlocking {
    Test().test(SCRIPT_SOURCE_CODE)
}