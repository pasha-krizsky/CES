package com.ces.domain.events

import com.ces.domain.types.CodeExecutionId
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock.System.now
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CodeExecutionStartedEventTest : StringSpec({
    "should create" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val executionLogsPath = "/some/path"

        val event = CodeExecutionStartedEvent(codeExecutionId, createdAt, executionLogsPath)

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.executionLogsPath shouldBe executionLogsPath
    }

    "should serialize" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val executionLogsPath = "/some/path"
        val event = CodeExecutionStartedEvent(codeExecutionId, createdAt, executionLogsPath)

        val encoded = Json.encodeToString(event)

        encoded shouldEqualJson """
        {
            "id": "${codeExecutionId.value}",
            "createdAt": "$createdAt",
            "executionLogsPath": "$executionLogsPath"
        }
        """.trimIndent()
    }

    "serialize and deserialize should be symmetrical" {
        val source = CodeExecutionStartedEvent(CodeExecutionId.random(), now(), "/some/path")

        val decoded = Json.decodeFromString<CodeExecutionStartedEvent>(Json.encodeToString(source))

        decoded shouldBe source
    }
})