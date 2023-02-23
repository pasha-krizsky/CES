package com.ces.domain.events

import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionCompletedEvent
import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionFailedEvent
import com.ces.domain.events.DomainTestData.Companion.aFailureReason
import com.ces.domain.events.DomainTestData.Companion.anExitCode
import com.ces.domain.events.CodeExecutionFinishedEvent.Companion.builder
import com.ces.domain.json.JsonConfig.Companion.decodeCodeExecutionEvent
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeExecutionFailureReason.NONE
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState
import com.ces.domain.types.CodeExecutionState.COMPLETED
import com.ces.domain.types.CodeExecutionState.FAILED
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.datetime.Clock.System.now

class CodeExecutionFinishedEventTest : StringSpec({

    "should create completed event from constructor" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()

        val event = CodeExecutionFinishedEvent(codeExecutionId, createdAt, COMPLETED, 0)

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.state shouldBe COMPLETED
        event.exitCode shouldBe 0
        event.failureReason shouldBe NONE
    }

    "should create completed event from builder" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()

        val event = builder {
            this.id = codeExecutionId
            this.createdAt = createdAt
            this.state = COMPLETED
            this.exitCode = 0
        }.build()

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.state shouldBe COMPLETED
        event.exitCode shouldBe 0
        event.failureReason shouldBe NONE
    }

    "should create failed event from constructor" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val exitCode = anExitCode()
        val failureReason = aFailureReason()

        val event = CodeExecutionFinishedEvent(codeExecutionId, createdAt, FAILED, exitCode, failureReason)

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.state shouldBe FAILED
        event.exitCode shouldBe exitCode
        event.failureReason shouldBe failureReason
    }

    "should create failed event from builder" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val exitCode = anExitCode()
        val failureReason = aFailureReason()

        val event = builder {
            this.id = codeExecutionId
            this.createdAt = createdAt
            this.state = FAILED
            this.exitCode = exitCode
            this.failureReason = failureReason
        }.build()

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.state shouldBe FAILED
        event.exitCode shouldBe exitCode
        event.failureReason shouldBe failureReason
    }

    "should copy" {
        val event = aCodeExecutionFailedEvent().build()

        val copied = event.copy().build()

        copied.shouldNotBeSameInstanceAs(event)
        copied shouldBe event
    }

    "should fail to create with non final state" {
        CodeExecutionState.values().filter { it.isNotFinal() }.forAll {
            val exception = shouldThrow<IllegalStateException> {
                aCodeExecutionCompletedEvent().apply { state = it }.build()
            }
            exception.message shouldBe "state must be final"
        }
    }

    "should fail to create with failed state and without failure reason" {
        val exception = shouldThrow<IllegalStateException> {
            aCodeExecutionFailedEvent().apply { failureReason = NONE }.build()
        }
        exception.message shouldBe "failureReason must be set for failed state"
    }

    "should fail to create with completed state and with failure reason" {
        val exception = shouldThrow<IllegalStateException> {
            aCodeExecutionCompletedEvent().apply { failureReason = aFailureReason() }.build()
        }
        exception.message shouldBe "failureReason must be empty for completed state"
    }

    "should serialize minimal" {
        val event = aCodeExecutionCompletedEvent().build()

        val encoded = encodeCodeExecutionEvent(event)

        encoded shouldEqualJson """
        {
            "type": "CodeExecutionFinishedEvent",
            "id": "${event.id.value}",
            "createdAt": "${event.createdAt}",
            "state": "${event.state.name}",
            "exitCode": ${event.exitCode}
        }
        """.trimIndent()
    }

    "should serialize full" {
        val event = aCodeExecutionFailedEvent().build()

        val encoded = encodeCodeExecutionEvent(event)

        encoded shouldEqualJson """
        {
            "type": "CodeExecutionFinishedEvent",
            "id": "${event.id.value}",
            "createdAt": "${event.createdAt}",
            "state": "${event.state.name}",
            "exitCode": ${event.exitCode},
            "failureReason": "${event.failureReason}"
        }
        """.trimIndent()
    }

    "serialize and deserialize should be symmetrical" {
        val source = aCodeExecutionFailedEvent().build()

        val decoded = decodeCodeExecutionEvent(encodeCodeExecutionEvent(source))

        decoded.shouldBeInstanceOf<CodeExecutionFinishedEvent>()
        decoded shouldBe source
    }
})
