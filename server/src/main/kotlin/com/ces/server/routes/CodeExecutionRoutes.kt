package com.ces.server.routes

import com.ces.domain.entities.CodeExecution
import com.ces.domain.types.CodeExecutionId
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.server.config.ServerConfig
import com.ces.server.flow.CodeExecutionCreateFlow
import com.ces.server.models.CodeExecutionRequest
import com.ces.server.models.CodeExecutionView
import com.ces.server.storage.CodeExecutionDao
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.io.File
import java.io.File.separator
import java.util.*

fun Route.codeExecutionRouting() {
    val database by inject<CodeExecutionDao>()
    val codeExecutionCreateFlow by inject<CodeExecutionCreateFlow>()
    val storage by inject<ObjectStorage>()
    val config by inject<ServerConfig>()

    route("/code-execution") {
        get("{id?}") {
            val id = call.parameters["id"] ?: return@get call.respondText(MISSING_ID_PARAMETER, status = BadRequest)

            try {
                val codeExecutionId = CodeExecutionId(UUID.fromString(id))
                val codeExecution = database.get(codeExecutionId)

                call.respond(CodeExecutionView.from(codeExecution))
            } catch (e: IllegalArgumentException) {
                call.respondText(WRONG_ID_FORMAT, status = BadRequest)
            }
        }
        get("{id?}/logs") {
            val id = call.parameters["id"] ?: return@get call.respondText(MISSING_ID_PARAMETER, status = BadRequest)

            try {
                val codeExecutionId = CodeExecutionId(UUID.fromString(id))
                val codeExecution = database.get(codeExecutionId)

                if (codeExecution.executionLogsPath == null)
                    throw NotFoundException("Execution logs not found")

                val logs = downloadLogs(config, codeExecution, storage)

                call.respondFile(logs)
                logs.delete()
            } catch (e: IllegalArgumentException) {
                call.respondText(WRONG_ID_FORMAT, status = BadRequest)
            }
        }
        post {
            val request = call.receive<CodeExecutionRequest>()
            val codeExecution = codeExecutionCreateFlow.run(request)
            call.respond(status = Accepted, CodeExecutionCreatedResponse(codeExecution.id))
        }
    }
}

private suspend fun downloadLogs(config: ServerConfig, codeExecution: CodeExecution, storage: ObjectStorage): File {
    val tmpLocalDestination = TMP_DIR + separator + UUID.randomUUID()
    return storage.downloadFile(config.codeExecutionBucketName, codeExecution.executionLogsPath!!, tmpLocalDestination)
}

private const val MISSING_ID_PARAMETER = "Missing id parameter"
private const val WRONG_ID_FORMAT = "Failed to parse id parameter to UUID format"
private val TMP_DIR: String = System.getProperty("java.io.tmpdir")

@Serializable
data class CodeExecutionCreatedResponse(val id: CodeExecutionId)