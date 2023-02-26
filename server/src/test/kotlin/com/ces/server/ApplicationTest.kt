package com.ces.server

import com.ces.domain.types.CodeExecutionId
import com.ces.server.plugins.configureRouting
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO fixme
class ApplicationTest {
//    @Test
//    fun testRoot() = testApplication {
//        application {
//            configureRouting()
//        }
//        client.get("/code-execution/${CodeExecutionId.random().value}").apply {
//            assertEquals(NotFound, status)
//        }
//    }
}