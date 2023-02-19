package com.ces.worker.domain.entities

import com.ces.worker.domain.types.CodeExecutionFailureReason.NONE
import com.ces.worker.domain.types.CodeExecutionFailureReason.TIME_LIMIT_EXCEEDED
import com.ces.worker.domain.types.CodeExecutionId
import com.ces.worker.domain.types.CodeExecutionState.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock.System.now
import kotlin.time.Duration.Companion.seconds

class CodeExecutionTest : StringSpec({
    "should create minimal" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val sourceCodePath = "/some/path"

        val codeExecution = CodeExecution(codeExecutionId, createdAt, CREATED, sourceCodePath)

        codeExecution.id shouldBe codeExecutionId
        codeExecution.createdAt shouldBe createdAt
        codeExecution.state shouldBe CREATED
        codeExecution.sourceCodePath shouldBe sourceCodePath

        codeExecution.finishedAt shouldBe null
        codeExecution.exitCode shouldBe null
        codeExecution.executionLogsPath shouldBe null
        codeExecution.failureReason shouldBe NONE
    }

    "should create full" {
        val codeExecutionId = CodeExecutionId.random()
        val createdAt = now()
        val sourceCodePath = "/some/path/source"
        val finishedAt = createdAt + 5.seconds
        val executionLogsPath = "/some/path/logs"

        val codeExecution = CodeExecution(
            codeExecutionId, createdAt, FAILED, sourceCodePath, finishedAt, 1, executionLogsPath, TIME_LIMIT_EXCEEDED
        )

        codeExecution.id shouldBe codeExecutionId
        codeExecution.createdAt shouldBe createdAt
        codeExecution.state shouldBe FAILED
        codeExecution.sourceCodePath shouldBe sourceCodePath
        codeExecution.finishedAt shouldBe finishedAt
        codeExecution.exitCode shouldBe 1
        codeExecution.executionLogsPath shouldBe executionLogsPath
        codeExecution.failureReason shouldBe TIME_LIMIT_EXCEEDED
    }

    "should fail to create with failed state and without failure reason" {
        val exception = shouldThrow<IllegalStateException> {
            CodeExecution(CodeExecutionId.random(), now(), FAILED, "/some/path")
        }
        exception.message shouldBe "failureReason must be set for failed state"
    }

    "should fail to create with completed state and with failure reason" {
        val exception = shouldThrow<IllegalStateException> {
            CodeExecution(
                CodeExecutionId.random(), now(), COMPLETED, "/some/path", failureReason = TIME_LIMIT_EXCEEDED
            )
        }
        exception.message shouldBe "failureReason must be empty for completed state"
    }

    "should fail to create with completed state and with empty finishedAt timestamp" {
        val exception = shouldThrow<IllegalStateException> {
            CodeExecution(CodeExecutionId.random(), now(), COMPLETED, "/some/path")
        }
        exception.message shouldBe "finishedAt must be set for final state"
    }

    "should fail to create with failed state and with empty finishedAt timestamp" {
        val exception = shouldThrow<IllegalStateException> {
            CodeExecution(CodeExecutionId.random(), now(), FAILED, "/some/path", failureReason = TIME_LIMIT_EXCEEDED)
        }
        exception.message shouldBe "finishedAt must be set for final state"
    }

    "should fail to create with not final state and not empty finishedAt timestamp" {
        listOf(CREATED, STARTED).forAll {
            val exception = shouldThrow<IllegalStateException> {
                CodeExecution(CodeExecutionId.random(), now(), it, "/some/path", finishedAt = now())
            }
            exception.message shouldBe "finishedAt must be empty for not final state"
        }
    }

    "should fail to create with completed state and with empty exitCode" {
        val exception = shouldThrow<IllegalStateException> {
            CodeExecution(CodeExecutionId.random(), now(), COMPLETED, "/some/path", finishedAt = now())
        }
        exception.message shouldBe "exitCode must be set for final state"
    }

    "should fail to create with failed state and with empty exitCode" {
        val exception = shouldThrow<IllegalStateException> {
            CodeExecution(
                id = CodeExecutionId.random(),
                createdAt = now(),
                state = FAILED,
                sourceCodePath = "/some/path",
                finishedAt = now(),
                failureReason = TIME_LIMIT_EXCEEDED
            )
        }
        exception.message shouldBe "exitCode must be set for final state"
    }

    "should fail to create with not final state and not empty exitCode" {
        listOf(CREATED, STARTED).forAll {
            val exception = shouldThrow<IllegalStateException> {
                CodeExecution(CodeExecutionId.random(), now(), it, "/some/path", exitCode = 0)
            }
            exception.message shouldBe "exitCode must be empty for not final state"
        }
    }
})