package com.ces.server.routes

import com.ces.domain.types.CodeCompilerType.MONO
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState.CREATED
import com.ces.domain.types.ProgrammingLanguage.C_SHARP
import com.ces.server.models.CodeExecutionView
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock.System.now

fun Route.codeExecutionRouting() {
    route("/code-execution") {
        get("{id?}") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = BadRequest
            )

            println("Got request with id = $id")

            call.respond(
                CodeExecutionView(
                    id = CodeExecutionId.random(),
                    createdAt = now(),
                    state = CREATED,
                    language = C_SHARP,
                    compiler = MONO,
                )
            )
        }
        post {
            val codeExecution = call.receive<CodeExecutionView>()
            println("Received code execution request + $codeExecution")
            call.respondText("Received code execution request", status = Created)
        }
    }
}