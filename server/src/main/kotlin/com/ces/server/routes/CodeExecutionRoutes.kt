package com.ces.server.routes

import com.ces.domain.types.CodeExecutionId
import com.ces.server.flow.CodeExecutionCreateFlow
import com.ces.server.models.CodeExecutionRequest
import com.ces.server.models.CodeExecutionView
import com.ces.server.storage.CodeExecutionStorage
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.util.*

fun Route.codeExecutionRouting() {
    val database by inject<CodeExecutionStorage>()
    val codeExecutionCreateFlow by inject<CodeExecutionCreateFlow>()

    route("/code-execution") {
        get("{id?}") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id parameter",
                status = BadRequest
            )

            val codeExecutionId = CodeExecutionId(UUID.fromString(id))
            val codeExecution = database.get(codeExecutionId)
            call.respond(CodeExecutionView.from(codeExecution))
        }
        post {
            val request = call.receive<CodeExecutionRequest>()
            val codeExecution = codeExecutionCreateFlow.run(request)
            call.respond(CodeExecutionCreatedResponse(codeExecution.id))
        }
    }
}

@Serializable
data class CodeExecutionCreatedResponse(val id: CodeExecutionId)