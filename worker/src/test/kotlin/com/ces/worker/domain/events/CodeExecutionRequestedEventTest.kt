package com.ces.worker.domain.events

import com.ces.worker.domain.types.CodeExecutionId
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock.System.now
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CodeExecutionRequestedEventTest : StringSpec({
    "should create" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val sourceCodePath = "/some/path"

        val event = CodeExecutionRequestedEvent(codeExecutionId, createdAt, sourceCodePath)

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.sourceCodePath shouldBe sourceCodePath
    }

    "should serialize" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val sourceCodePath = "/some/path"
        val event = CodeExecutionRequestedEvent(codeExecutionId, createdAt, sourceCodePath)

        val encoded = Json.encodeToString(event)

        encoded shouldEqualJson """
        {
            "id": "${codeExecutionId.value}",
            "createdAt": "$createdAt",
            "sourceCodePath": "$sourceCodePath"
        }
        """.trimIndent()
    }

    "serialize and deserialize should be symmetrical" {
        val source = CodeExecutionRequestedEvent(CodeExecutionId.random(), now(), "/some/path")

        val decoded = Json.decodeFromString<CodeExecutionRequestedEvent>(Json.encodeToString(source))

        decoded shouldBe source
    }
})
