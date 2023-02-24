package com.ces.server.plugins

import com.ces.server.routes.codeExecutionRouting
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        codeExecutionRouting()
    }
}
