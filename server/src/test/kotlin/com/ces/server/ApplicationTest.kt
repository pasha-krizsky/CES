package com.ces.server

import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState.CREATED
import com.ces.server.models.CodeExecutionView
import com.ces.server.plugins.configureRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting()
        }
        client.get("/code-execution?id=${CodeExecutionId.random().value}").apply {
            assertEquals(OK, status)
            val codeExecution = Json.decodeFromString<CodeExecutionView>(bodyAsText())
            assertEquals(CREATED, codeExecution.state)
        }
    }
}