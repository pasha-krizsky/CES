package com.ces.server

import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.json.JsonConfig.Companion.decodeCodeExecutionEvent
import com.ces.domain.types.CodeCompilerType.MONO
import com.ces.domain.types.ProgrammingLanguage.C_SHARP
import com.ces.infrastructure.minio.MinioExtension
import com.ces.infrastructure.minio.MinioStorage
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.RabbitReceiveQueue
import com.ces.infrastructure.rabbitmq.RabbitmqConnector
import com.ces.infrastructure.rabbitmq.RabbitmqExtension
import com.ces.infrastructure.rabbitmq.ReceiveQueue
import com.ces.server.config.ServerConfig
import com.ces.server.models.CodeExecutionRequest
import com.ces.server.models.CodeExecutionView
import com.ces.server.routes.CodeExecutionCreatedResponse
import com.typesafe.config.ConfigFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.minio.MinioAsyncClient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import java.util.UUID.randomUUID

val config = ServerConfig.from(HoconApplicationConfig(ConfigFactory.load()))

class ApplicationTest : StringSpec({

    extension(MinioExtension(config.minio.accessKey, config.minio.secretKey))
    extension(RabbitmqExtension())

    lateinit var rabbitConnector: RabbitmqConnector
    lateinit var requestQueue: ReceiveQueue
    lateinit var minioStorage: ObjectStorage

    beforeSpec {
        rabbitConnector = RabbitmqConnector(config.rabbitmq)
        requestQueue = requestQueue(rabbitConnector)
        minioStorage = minioStorage()

        minioStorage.createBucket(config.codeExecutionBucketName)
    }

    afterTest {
        stopKoin()
    }

    "should submit code execution request" {
        testApplication {
            val request = CodeExecutionRequest(C_SHARP, MONO, "source code")
            val response = client.post("/code-execution") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(request))
            }

            response.status shouldBe Accepted

            val createdResponse = Json.decodeFromString<CodeExecutionCreatedResponse>(response.bodyAsText())
            val codeExecutionId = createdResponse.id
            val resultPath = tempdir().absolutePath + "_tmp"
            val sourceCode =
                minioStorage.downloadFile(config.codeExecutionBucketName, "${codeExecutionId.value}/source", resultPath)

            sourceCode.readText() shouldBe "source code"

            val message = requestQueue.receiveMessage()
            requestQueue.markProcessed(message.deliveryId)
            val requestedEvent = decodeCodeExecutionEvent(message.content)

            requestedEvent.shouldBeInstanceOf<CodeExecutionRequestedEvent>()
            requestedEvent.id shouldBe codeExecutionId
        }
    }

    "should retrieve code execution" {
        testApplication {
            val request = CodeExecutionRequest(C_SHARP, MONO, "source code")
            val response = client.post("/code-execution") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(request))
            }

            response.status shouldBe Accepted

            val createdResponse = Json.decodeFromString<CodeExecutionCreatedResponse>(response.bodyAsText())
            val codeExecutionId = createdResponse.id
            val result = client.get("/code-execution/${codeExecutionId.value}")

            result.status shouldBe OK

            val codeExecution = Json.decodeFromString<CodeExecutionView>(result.bodyAsText())

            codeExecution.id shouldBe codeExecutionId
            codeExecution.language shouldBe C_SHARP
            codeExecution.compiler shouldBe MONO
        }
    }

    "should return 400 error when code execution id is not valid UUID" {
        testApplication {
            val response = client.get("/code-execution/wrong-id-format")
            response.status shouldBe BadRequest
        }
    }

    "should return 404 error when code execution does not exist" {
        testApplication {
            val response = client.get("/code-execution/${randomUUID()}")
            response.status shouldBe NotFound
        }
    }
})

private fun requestQueue(connector: RabbitmqConnector) = RabbitReceiveQueue(
    config.codeExecutionRequestQueue.name, connector
)

private fun minioStorage() = MinioStorage(
    MinioAsyncClient.builder()
        .endpoint(config.minio.endpoint)
        .credentials(config.minio.accessKey, config.minio.secretKey)
        .build()
)