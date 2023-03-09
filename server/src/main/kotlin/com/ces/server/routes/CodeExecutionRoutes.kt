package com.ces.server.routes

import com.ces.domain.entities.CodeExecution
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState.CREATED
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.server.config.ServerConfig
import com.ces.server.dao.CodeExecutionDao
import com.ces.server.flow.CodeExecutionSubmitFlow
import com.ces.server.models.CodeExecutionRequest
import com.ces.server.models.CodeExecutionView
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.io.File
import java.io.File.separator
import java.util.*
import java.util.UUID.randomUUID

@Serializable
data class CodeExecutionCreatedResponse(val id: CodeExecutionId)

fun Route.codeExecutionRouting() {
    val database by inject<CodeExecutionDao>()
    val codeExecutionSubmitFlow by inject<CodeExecutionSubmitFlow>()
    val storage by inject<ObjectStorage>()
    val config by inject<ServerConfig>()

    route("/code-execution") {
        get("{id?}") {
            val id =
                call.parameters["id"] ?: return@get call.respondText(MISSING_ID_PARAMETER_ERROR, status = BadRequest)

            try {
                val codeExecution = fetchCodeExecution(id, database)
                call.respond(CodeExecutionView.from(codeExecution))
            } catch (e: IllegalArgumentException) {
                call.respondText(WRONG_ID_FORMAT_ERROR, status = BadRequest)
            }
        }
        get("{id?}/logs") {
            val id =
                call.parameters["id"] ?: return@get call.respondText(MISSING_ID_PARAMETER_ERROR, status = BadRequest)
            val stdout = call.request.queryParameters["stdout"]?.toBooleanStrict() ?: false
            val stderr = call.request.queryParameters["stderr"]?.toBooleanStrict() ?: false

            if (!stdout && !stderr)
                return@get call.respondText(MISSING_LOGS_STREAM_ERROR, status = BadRequest)

            try {
                val codeExecution = fetchCodeExecution(id, database)
                val state = codeExecution.state
                if (state == CREATED)
                    return@get call.respondText(LOGS_NOT_FOUND_ERROR, status = NotFound)

                val logs = downloadLogs(config, codeExecution, storage, stdout, stderr)
                    ?: return@get call.respondText(LOGS_NOT_FOUND_ILLEGAL_STATE_ERROR, status = InternalServerError)

                call.respondFile(logs)
                logs.delete()
            } catch (e: IllegalArgumentException) {
                call.respondText(WRONG_ID_FORMAT_ERROR, status = BadRequest)
            }
        }
        post {
            val request = call.receive<CodeExecutionRequest>()
            val codeExecution = codeExecutionSubmitFlow.run(request)
            call.respond(status = Accepted, CodeExecutionCreatedResponse(codeExecution.id))
        }
    }
}

private suspend fun fetchCodeExecution(id: String, database: CodeExecutionDao): CodeExecution {
    val codeExecutionId = CodeExecutionId(UUID.fromString(id))
    return database.get(codeExecutionId)
}

private suspend fun downloadLogs(
    config: ServerConfig,
    codeExecution: CodeExecution,
    storage: ObjectStorage,
    stdout: Boolean,
    stderr: Boolean,
): File? {
    val tmpLocalDestination = ServerConfig.tmpDir + separator + randomUUID()
    val logs = codeExecution.logsPath!!
    val logsPath = if (stdout && stderr) logs.allPath else if (stdout) logs.stdoutPath else logs.stderrPath
    return storage.find(
        config.codeExecutionBucketName,
        logsPath,
        tmpLocalDestination
    )
}

private const val MISSING_ID_PARAMETER_ERROR = "Missing id parameter"
private const val WRONG_ID_FORMAT_ERROR = "Failed to parse id parameter to UUID format"
private const val MISSING_LOGS_STREAM_ERROR = "At least one 'stdout' or 'stderr' query parameter must be set to true"
private const val LOGS_NOT_FOUND_ERROR = "Logs are not available for code execution in CREATED state"
private const val LOGS_NOT_FOUND_ILLEGAL_STATE_ERROR = "Logs should exist for code execution in not CREATED state"