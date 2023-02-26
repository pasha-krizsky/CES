package com.ces.server.routes

import com.ces.domain.types.CodeExecutionId
import com.ces.server.flow.CodeExecutionCreateFlow
import com.ces.server.models.CodeExecutionRequest
import com.ces.server.models.CodeExecutionView
import com.ces.server.storage.CodeExecutionDao
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.util.*

fun Route.codeExecutionRouting() {
    val database by inject<CodeExecutionDao>()
    val codeExecutionCreateFlow by inject<CodeExecutionCreateFlow>()

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
        post {
            val request = call.receive<CodeExecutionRequest>()
            val codeExecution = codeExecutionCreateFlow.run(request)
            call.respond(CodeExecutionCreatedResponse(codeExecution.id))
        }
    }
}

private const val MISSING_ID_PARAMETER = "Missing id parameter"
private const val WRONG_ID_FORMAT = "Failed to parse id parameter to UUID format"

@Serializable
data class CodeExecutionCreatedResponse(val id: CodeExecutionId)