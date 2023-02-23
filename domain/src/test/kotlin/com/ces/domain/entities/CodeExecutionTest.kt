package com.ces.domain.entities

import com.ces.domain.events.DomainTestData.Companion.aCompiler
import com.ces.domain.events.DomainTestData.Companion.aCompletedCodeExecution
import com.ces.domain.events.DomainTestData.Companion.aFailedCodeExecution
import com.ces.domain.events.DomainTestData.Companion.aFailureReason
import com.ces.domain.events.DomainTestData.Companion.aPath
import com.ces.domain.events.DomainTestData.Companion.aProgrammingLanguage
import com.ces.domain.events.DomainTestData.Companion.aStartedCodeExecution
import com.ces.domain.events.DomainTestData.Companion.anExitCode
import com.ces.domain.types.CodeExecutionFailureReason.NONE
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.datetime.Clock.System.now
import kotlin.time.Duration.Companion.seconds

class CodeExecutionTest : StringSpec({
    "should create minimal from constructor" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val sourceCodePath = aPath()
        val language = aProgrammingLanguage()
        val compiler = aCompiler()

        val codeExecution = CodeExecution(codeExecutionId, createdAt, CREATED, sourceCodePath, language, compiler)

        codeExecution.id shouldBe codeExecutionId
        codeExecution.createdAt shouldBe createdAt
        codeExecution.state shouldBe CREATED
        codeExecution.sourceCodePath shouldBe sourceCodePath
        codeExecution.language shouldBe language
        codeExecution.compiler shouldBe compiler

        codeExecution.finishedAt shouldBe null
        codeExecution.exitCode shouldBe null
        codeExecution.executionLogsPath shouldBe null
        codeExecution.failureReason shouldBe NONE
    }

    "should create minimal from builder" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val sourceCodePath = aPath()
        val language = aProgrammingLanguage()
        val compiler = aCompiler()

        val codeExecution = CodeExecution.builder {
            this.id = codeExecutionId
            this.createdAt = createdAt
            this.state = CREATED
            this.sourceCodePath = sourceCodePath
            this.language = language
            this.compiler = compiler
        }.build()

        codeExecution.id shouldBe codeExecutionId
        codeExecution.createdAt shouldBe createdAt
        codeExecution.state shouldBe CREATED
        codeExecution.sourceCodePath shouldBe sourceCodePath
        codeExecution.language shouldBe language
        codeExecution.compiler shouldBe compiler

        codeExecution.finishedAt shouldBe null
        codeExecution.exitCode shouldBe null
        codeExecution.executionLogsPath shouldBe null
        codeExecution.failureReason shouldBe NONE
    }

    "should create full from constructor" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val sourceCodePath = aPath()
        val language = aProgrammingLanguage()
        val compiler = aCompiler()
        val finishedAt = createdAt + 5.seconds
        val exitCode = anExitCode()
        val executionLogsPath = aPath()
        val failureReason = aFailureReason()

        val codeExecution = CodeExecution(
            codeExecutionId, createdAt, FAILED, sourceCodePath, language, compiler, finishedAt,
            exitCode, executionLogsPath, failureReason
        )

        codeExecution.id shouldBe codeExecutionId
        codeExecution.createdAt shouldBe createdAt
        codeExecution.state shouldBe FAILED
        codeExecution.sourceCodePath shouldBe sourceCodePath
        codeExecution.language shouldBe language
        codeExecution.compiler shouldBe compiler
        codeExecution.finishedAt shouldBe finishedAt
        codeExecution.exitCode shouldBe exitCode
        codeExecution.executionLogsPath shouldBe executionLogsPath
        codeExecution.failureReason shouldBe failureReason
    }

    "should create full from builder" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val sourceCodePath = aPath()
        val language = aProgrammingLanguage()
        val compiler = aCompiler()
        val finishedAt = createdAt + 5.seconds
        val exitCode = anExitCode()
        val executionLogsPath = aPath()
        val failureReason = aFailureReason()

        val codeExecution = CodeExecution.builder {
            this.id = codeExecutionId
            this.createdAt = createdAt
            this.state = FAILED
            this.sourceCodePath = sourceCodePath
            this.language = language
            this.compiler = compiler
            this.finishedAt = finishedAt
            this.exitCode = exitCode
            this.executionLogsPath = executionLogsPath
            this.failureReason = failureReason
        }.build()

        codeExecution.id shouldBe codeExecutionId
        codeExecution.createdAt shouldBe createdAt
        codeExecution.state shouldBe FAILED
        codeExecution.sourceCodePath shouldBe sourceCodePath
        codeExecution.language shouldBe language
        codeExecution.compiler shouldBe compiler
        codeExecution.finishedAt shouldBe finishedAt
        codeExecution.exitCode shouldBe exitCode
        codeExecution.executionLogsPath shouldBe executionLogsPath
        codeExecution.failureReason shouldBe failureReason
    }

    "should copy" {
        val event = aFailedCodeExecution().build()

        val copied = event.copy().build()

        copied.shouldNotBeSameInstanceAs(event)
        copied shouldBe event
    }

    "should fail to create with failed state and without failure reason" {
        val exception = shouldThrow<IllegalStateException> {
            aFailedCodeExecution().apply { failureReason = NONE }.build()
        }
        exception.message shouldBe "failureReason must be set for failed state"
    }

    "should fail to create with completed state and with failure reason" {
        val exception = shouldThrow<IllegalStateException> {
            aCompletedCodeExecution().apply { failureReason = aFailureReason() }.build()
        }
        exception.message shouldBe "failureReason must be empty for not failed state"
    }

    "should fail to create with completed state and with empty finishedAt timestamp" {
        val exception = shouldThrow<IllegalStateException> {
            aCompletedCodeExecution().apply { finishedAt = null }.build()
        }
        exception.message shouldBe "finishedAt must be set for final state"
    }

    "should fail to create with failed state and with empty finishedAt timestamp" {
        val exception = shouldThrow<IllegalStateException> {
            aFailedCodeExecution().apply { finishedAt = null }.build()
        }
        exception.message shouldBe "finishedAt must be set for final state"
    }

    "should fail to create with not final state and not empty finishedAt timestamp" {
        listOf(CREATED, STARTED).forAll {
            val exception = shouldThrow<IllegalStateException> {
                aStartedCodeExecution().apply {
                    state = it
                    finishedAt = now()
                }.build()
            }
            exception.message shouldBe "finishedAt must be empty for not final state"
        }
    }
})