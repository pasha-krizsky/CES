package com.ces.domain.events

import com.ces.domain.events.CodeExecutionRequestedEvent.Companion.builder
import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionRequestedEvent
import com.ces.domain.events.DomainTestData.Companion.aCompiler
import com.ces.domain.events.DomainTestData.Companion.aPath
import com.ces.domain.events.DomainTestData.Companion.aProgrammingLanguage
import com.ces.domain.json.JsonConfig.Companion.decodeCodeExecutionEvent
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeExecutionId
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.datetime.Clock.System.now

class CodeExecutionRequestedEventTest : StringSpec({

    "should create from constructor" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val language = aProgrammingLanguage()
        val compiler = aCompiler()
        val sourceCodePath = aPath()

        val event = CodeExecutionRequestedEvent(codeExecutionId, createdAt, language, compiler, sourceCodePath)

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.language shouldBe language
        event.compiler shouldBe compiler
        event.sourceCodePath shouldBe sourceCodePath
    }

    "should create from builder" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val language = aProgrammingLanguage()
        val compiler = aCompiler()
        val sourceCodePath = aPath()

        val event = builder {
            this.id = codeExecutionId
            this.createdAt = createdAt
            this.language = language
            this.compiler = compiler
            this.sourceCodePath = sourceCodePath
        }.build()

        event.id shouldBe codeExecutionId
        event.createdAt shouldBe createdAt
        event.language shouldBe language
        event.compiler shouldBe compiler
        event.sourceCodePath shouldBe sourceCodePath
    }

    "should copy" {
        val event = aCodeExecutionRequestedEvent().build()

        val copied = event.copy().build()

        copied.shouldNotBeSameInstanceAs(event)
        copied shouldBe event
    }

    "should serialize" {
        val event = aCodeExecutionRequestedEvent().build()

        val encoded = encodeCodeExecutionEvent(event)

        encoded shouldEqualJson """
        {
            "type": "CodeExecutionRequestedEvent",
            "id": "${event.id.value}",
            "createdAt": "${event.createdAt}",
            "language": "${event.language.name}",
            "compiler": "${event.compiler.name}",
            "sourceCodePath": "${event.sourceCodePath}"
        }
        """.trimIndent()
    }

    "serialize and deserialize should be symmetrical" {
        val source = aCodeExecutionRequestedEvent().build()

        val decoded = decodeCodeExecutionEvent(encodeCodeExecutionEvent(source))

        decoded.shouldBeInstanceOf<CodeExecutionRequestedEvent>()
        decoded shouldBe source
    }
})
