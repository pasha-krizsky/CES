package com.ces.domain.events

import com.ces.domain.types.CodeExecutionFailureReason.NONE
import com.ces.domain.types.CodeExecutionFailureReason.TIME_LIMIT_EXCEEDED
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState.COMPLETED
import com.ces.domain.types.CodeExecutionState.FAILED
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock.System.now
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CodeExecutionFinishedEventTest : StringSpec({
    "should create minimal" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()

        val event = CodeExecutionFinishedEvent(codeExecutionId, createdAt, COMPLETED, 0)

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.state shouldBe COMPLETED
        event.exitCode shouldBe 0
        event.failureReason shouldBe NONE
    }

    "should create full" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()

        val event = CodeExecutionFinishedEvent(codeExecutionId, createdAt, FAILED, 1, TIME_LIMIT_EXCEEDED)

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.state shouldBe FAILED
        event.exitCode shouldBe 1
        event.failureReason shouldBe TIME_LIMIT_EXCEEDED
    }

    "should fail to create with failed state and without failure reason" {
        val exception = shouldThrow<IllegalStateException> {
            CodeExecutionFinishedEvent(CodeExecutionId.random(), now(), FAILED, 1)
        }
        exception.message shouldBe "failureReason must be set for failed state"
    }

    "should fail to create with completed state and with failure reason" {
        val exception = shouldThrow<IllegalStateException> {
            CodeExecutionFinishedEvent(CodeExecutionId.random(), now(), COMPLETED, 0, TIME_LIMIT_EXCEEDED)
        }
        exception.message shouldBe "failureReason must be empty for completed state"
    }

    "should serialize minimal" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val event = CodeExecutionFinishedEvent(codeExecutionId, createdAt, COMPLETED, 0)

        val encoded = Json.encodeToString(event)

        encoded shouldEqualJson """
        {
            "id": "${codeExecutionId.value}",
            "createdAt": "$createdAt",
            "state": "COMPLETED",
            "exitCode": 0
        }
        """.trimIndent()
    }

    "should serialize full" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val event = CodeExecutionFinishedEvent(codeExecutionId, createdAt, FAILED, 1, TIME_LIMIT_EXCEEDED)
        val encoded = Json.encodeToString(event)

        encoded shouldEqualJson """
        {
            "id": "${codeExecutionId.value}",
            "createdAt": "$createdAt",
            "state": "FAILED",
            "exitCode": 1,
            "failureReason": "TIME_LIMIT_EXCEEDED"
        }
        """.trimIndent()
    }

    "serialize and deserialize should be symmetrical" {
        val source = CodeExecutionFinishedEvent(CodeExecutionId.random(), now(), FAILED, 1, TIME_LIMIT_EXCEEDED)

        val decoded = Json.decodeFromString<CodeExecutionFinishedEvent>(Json.encodeToString(source))

        decoded shouldBe source
    }
})
