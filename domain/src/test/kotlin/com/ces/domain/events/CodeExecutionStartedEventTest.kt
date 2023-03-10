package com.ces.domain.events

import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionLogsPath
import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionStartedEvent
import com.ces.domain.json.JsonConfig.Companion.decodeCodeExecutionEvent
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeExecutionId
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.datetime.Clock.System.now

class CodeExecutionStartedEventTest : StringSpec({

    "should create from constructor" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val executionLogsPath = aCodeExecutionLogsPath()

        val event = CodeExecutionStartedEvent(codeExecutionId, createdAt, executionLogsPath)

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.logsPath shouldBe executionLogsPath
    }

    "should create from builder" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val executionLogsPath = aCodeExecutionLogsPath()

        val event = CodeExecutionStartedEvent.builder {
            this.id = codeExecutionId
            this.createdAt = createdAt
            this.logsPath = executionLogsPath
        }.build()

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.logsPath shouldBe executionLogsPath
    }

    "should copy" {
        val event = aCodeExecutionStartedEvent().build()

        val copied = event.copy().build()

        copied.shouldNotBeSameInstanceAs(event)
        copied shouldBe event
    }

    "should serialize" {
        val event = aCodeExecutionStartedEvent().build()

        val encoded = encodeCodeExecutionEvent(event)

        encoded shouldEqualJson """
        {
            "type": "CodeExecutionStartedEvent",
            "id": "${event.id.value}",
            "createdAt": "${event.createdAt}",
            "logsPath": {
              "allPath": "${event.logsPath.allPath}",
              "stdoutPath": "${event.logsPath.stdoutPath}",
              "stderrPath": "${event.logsPath.stderrPath}"
            }
        }
        """.trimIndent()
    }

    "serialize and deserialize should be symmetrical" {
        val source = aCodeExecutionStartedEvent().build()

        val decoded = decodeCodeExecutionEvent(encodeCodeExecutionEvent(source))

        decoded.shouldBeInstanceOf<CodeExecutionStartedEvent>()
        decoded shouldBe source
    }
})