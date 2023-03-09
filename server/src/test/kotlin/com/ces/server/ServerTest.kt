package com.ces.server

import com.ces.domain.entities.CodeExecution.Companion.ALL_LOGS_FILE_NAME
import com.ces.domain.entities.CodeExecution.Companion.SOURCE_FILE_NAME
import com.ces.domain.entities.CodeExecution.Companion.STDERR_LOGS_FILE_NAME
import com.ces.domain.entities.CodeExecution.Companion.STDOUT_LOGS_FILE_NAME
import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.events.CodeExecutionStartedEvent
import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionLogsPath
import com.ces.domain.events.DomainTestData.Companion.aCompiler
import com.ces.domain.events.DomainTestData.Companion.aLogs
import com.ces.domain.events.DomainTestData.Companion.aProgrammingLanguage
import com.ces.domain.events.DomainTestData.Companion.aSourceCode
import com.ces.domain.json.JsonConfig.Companion.decodeCodeExecutionEvent
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeCompilerType
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionLogsPath
import com.ces.domain.types.ProgrammingLanguage
import com.ces.infrastructure.database.DatabaseExtension
import com.ces.infrastructure.minio.MinioExtension
import com.ces.infrastructure.minio.MinioStorage
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.*
import com.ces.server.config.ServerConfig
import com.ces.server.models.CodeExecutionRequest
import com.ces.server.models.CodeExecutionView
import com.ces.server.routes.CodeExecutionCreatedResponse
import com.typesafe.config.ConfigFactory
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.minio.MinioAsyncClient
import kotlinx.datetime.Clock.System.now
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.stopKoin
import java.io.File.separator
import java.util.UUID.randomUUID
import kotlin.time.Duration.Companion.seconds

val config = ServerConfig.from(HoconApplicationConfig(ConfigFactory.load()))

class ServerTest : StringSpec({

    extension(DatabaseExtension(config.database))
    extension(MinioExtension(config.minio.accessKey, config.minio.secretKey))
    extension(RabbitmqExtension())

    lateinit var rabbitConnector: RabbitmqConnector
    lateinit var requestQueue: ReceiveQueue
    lateinit var responseQueue: SendQueue

    lateinit var minioStorage: ObjectStorage

    beforeSpec {
        rabbitConnector = RabbitmqConnector(config.rabbitmq)
        requestQueue = requestQueue(rabbitConnector)
        responseQueue = responseQueue(rabbitConnector)

        minioStorage = minioStorage()

        minioStorage.createBucket(config.codeExecutionBucketName)
    }

    afterTest {
        stopKoin()
    }

    "POST /code-execution should submit code execution request" {
        testApplication {
            val sourceCode = aSourceCode()
            val submitRequest = CodeExecutionRequest(aProgrammingLanguage(), aCompiler(), sourceCode)
            val httpResponse = client.post("/code-execution") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(submitRequest))
            }

            httpResponse.status shouldBe Accepted

            val submitResponse = Json.decodeFromString<CodeExecutionCreatedResponse>(httpResponse.bodyAsText())
            val codeExecutionId = submitResponse.id

            val fromPath = "${codeExecutionId.value}/$SOURCE_FILE_NAME"
            val resultPath = tempdir().absolutePath + separator + codeExecutionId.value
            val savedSourceCode = minioStorage.get(config.codeExecutionBucketName, fromPath, resultPath)

            savedSourceCode.readText() shouldBe sourceCode

            val message = requestQueue.receiveMessage()
            requestQueue.markProcessed(message.deliveryId)
            val requestedEvent = decodeCodeExecutionEvent(message.content)

            requestedEvent.shouldBeInstanceOf<CodeExecutionRequestedEvent>()
            requestedEvent.id shouldBe codeExecutionId
        }
    }

    "GET /code-execution/{id} should retrieve code execution information" {
        testApplication {
            val language = aProgrammingLanguage()
            val compiler = aCompiler()
            val submitRequest = CodeExecutionRequest(language, compiler, aSourceCode())
            val httpResponse = client.post("/code-execution") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(submitRequest))
            }

            httpResponse.status shouldBe Accepted

            val submitResponse = Json.decodeFromString<CodeExecutionCreatedResponse>(httpResponse.bodyAsText())
            val codeExecutionId = submitResponse.id
            val result = client.get("/code-execution/${codeExecutionId.value}")

            result.status shouldBe OK

            val codeExecution = Json.decodeFromString<CodeExecutionView>(result.bodyAsText())

            codeExecution.id shouldBe codeExecutionId
            codeExecution.language shouldBe language
            codeExecution.compiler shouldBe compiler
        }
    }

    "GET /code-execution/{id}/logs?stdout=true should return stdout logs" {
        testApplication {
            val codeExecutionId = submitCodeExecution(aProgrammingLanguage(), aCompiler(), aSourceCode())

            val logsPath = codeExecutionLogsPathFor(codeExecutionId)

            val stdoutLogs = aLogs()
            val stderrLogs = aLogs()
            storeLogs(minioStorage, stdoutLogs, logsPath.stdoutPath)
            storeLogs(minioStorage, stderrLogs, logsPath.stderrPath)
            storeLogs(minioStorage, stdoutLogs + stderrLogs, logsPath.allPath)
            val startedEvent = CodeExecutionStartedEvent(codeExecutionId, now(), logsPath)
            responseQueue.sendMessage(Message(encodeCodeExecutionEvent(startedEvent)))

            eventually(2.seconds) {
                val logsHttpResponse = client.get("/code-execution/${codeExecutionId.value}/logs?stdout=true")

                logsHttpResponse.status shouldBe OK
                logsHttpResponse.bodyAsText() shouldBe stdoutLogs
            }
        }
    }

    "GET /code-execution/{id}/logs?stderr=true should return stderr logs" {
        testApplication {
            val codeExecutionId = submitCodeExecution(aProgrammingLanguage(), aCompiler(), aSourceCode())

            val logsPath = codeExecutionLogsPathFor(codeExecutionId)

            val stdoutLogs = aLogs()
            val stderrLogs = aLogs()
            storeLogs(minioStorage, stdoutLogs, logsPath.stdoutPath)
            storeLogs(minioStorage, stderrLogs, logsPath.stderrPath)
            storeLogs(minioStorage, stdoutLogs + stderrLogs, logsPath.allPath)
            val startedEvent = CodeExecutionStartedEvent(codeExecutionId, now(), logsPath)
            responseQueue.sendMessage(Message(encodeCodeExecutionEvent(startedEvent)))

            eventually(2.seconds) {
                val logsHttpResponse = client.get("/code-execution/${codeExecutionId.value}/logs?stderr=true")

                logsHttpResponse.status shouldBe OK
                logsHttpResponse.bodyAsText() shouldBe stderrLogs
            }
        }
    }

    "GET /code-execution/{id}/logs?stdout=true&stderr=true should return all" {
        testApplication {
            val codeExecutionId = submitCodeExecution(aProgrammingLanguage(), aCompiler(), aSourceCode())

            val logsPath = codeExecutionLogsPathFor(codeExecutionId)

            val stdoutLogs = aLogs()
            val stderrLogs = aLogs()
            storeLogs(minioStorage, stdoutLogs, logsPath.stdoutPath)
            storeLogs(minioStorage, stderrLogs, logsPath.stderrPath)
            storeLogs(minioStorage, stdoutLogs + stderrLogs, logsPath.allPath)
            val startedEvent = CodeExecutionStartedEvent(codeExecutionId, now(), logsPath)
            responseQueue.sendMessage(Message(encodeCodeExecutionEvent(startedEvent)))

            eventually(2.seconds) {
                val logsHttpResponse =
                    client.get("/code-execution/${codeExecutionId.value}/logs?stdout=true&stderr=true")

                logsHttpResponse.status shouldBe OK
                logsHttpResponse.bodyAsText() shouldBe stdoutLogs + stderrLogs
            }
        }
    }

    "GET /code-execution/{id} should return 400 error when code execution id is not valid UUID" {
        testApplication {
            val response = client.get("/code-execution/wrong-id-format")
            response.status shouldBe BadRequest
        }
    }

    "GET /code-execution/{id} should return 404 error when code execution does not exist" {
        testApplication {
            val response = client.get("/code-execution/${randomUUID()}")
            response.status shouldBe NotFound
        }
    }

    "GET /code-execution/{id}/logs?stdout=true should return 400 error when code execution id is not valid UUID" {
        testApplication {
            val response = client.get("/code-execution/wrong-id-format/logs?stdout=true")
            response.status shouldBe BadRequest
            response.bodyAsText() shouldBe "Failed to parse id parameter to UUID format"
        }
    }

    "GET /code-execution/{id}/logs?stdout=true should return 404 error when code execution does not exist" {
        testApplication {
            val response = client.get("/code-execution/${randomUUID()}/logs?stdout=true")
            response.status shouldBe NotFound
        }
    }

    "GET /code-execution/{id}/logs should return 400 error when logs stream not specified" {
        testApplication {
            val response = client.get("/code-execution/${randomUUID()}/logs")
            response.status shouldBe BadRequest
            response.bodyAsText() shouldBe "At least one 'stdout' or 'stderr' query parameter must be set to true"
        }
    }

    "GET /code-execution/{id}/logs?stdout=true should return 404 error when code execution is in CREATED state" {
        testApplication {
            val codeExecutionId = submitCodeExecution(aProgrammingLanguage(), aCompiler(), aSourceCode())

            val response = client.get("/code-execution/${codeExecutionId.value}/logs?stdout=true")
            response.status shouldBe NotFound
            response.bodyAsText() shouldBe "Logs are not available for code execution in CREATED state"
        }
    }

    "GET /code-execution/{id}/logs?stdout=true should return 500 error when logs not found" {
        testApplication {
            val codeExecutionId = submitCodeExecution(aProgrammingLanguage(), aCompiler(), aSourceCode())

            val startedEvent = CodeExecutionStartedEvent(codeExecutionId, now(), aCodeExecutionLogsPath())
            responseQueue.sendMessage(Message(encodeCodeExecutionEvent(startedEvent)))

            eventually(2.seconds) {
                val response = client.get("/code-execution/${codeExecutionId.value}/logs?stdout=true")
                response.status shouldBe InternalServerError
                response.bodyAsText() shouldBe "Logs should exist for code execution in not CREATED state"
            }
        }
    }
})

private suspend fun ApplicationTestBuilder.submitCodeExecution(
    language: ProgrammingLanguage,
    compiler: CodeCompilerType,
    sourceCode: String,
): CodeExecutionId {
    val submitRequest = CodeExecutionRequest(language, compiler, sourceCode)
    val httpResponse = client.post("/code-execution") {
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(submitRequest))
    }

    httpResponse.status shouldBe Accepted

    val submitResponse = Json.decodeFromString<CodeExecutionCreatedResponse>(httpResponse.bodyAsText())
    return submitResponse.id
}

private suspend fun StringSpec.storeLogs(storage: ObjectStorage, logs: String, path: String) {
    val tmpFile = tempfile()
    tmpFile.appendText(logs)
    storage.upload(config.codeExecutionBucketName, tmpFile.absolutePath, path)
}

private fun requestQueue(connector: RabbitmqConnector) = RabbitReceiveQueue(
    config.codeExecutionRequestQueue.name, connector
)

private fun responseQueue(connector: RabbitmqConnector) = RabbitSendQueue(
    config.codeExecutionResponseQueue.name, connector
)

private fun minioStorage() = MinioStorage(
    MinioAsyncClient.builder()
        .endpoint(config.minio.endpoint)
        .credentials(config.minio.accessKey, config.minio.secretKey)
        .build()
)

private fun codeExecutionLogsPathFor(codeExecutionId: CodeExecutionId): CodeExecutionLogsPath {
    val allPath = "${codeExecutionId.value}/$ALL_LOGS_FILE_NAME"
    val stdoutPath = "${codeExecutionId.value}/$STDOUT_LOGS_FILE_NAME"
    val stderrPath = "${codeExecutionId.value}/$STDERR_LOGS_FILE_NAME"
    return CodeExecutionLogsPath(allPath, stdoutPath, stderrPath)
}