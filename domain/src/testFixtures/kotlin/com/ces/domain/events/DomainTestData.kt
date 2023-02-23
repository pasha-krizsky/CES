package com.ces.domain.events

import com.ces.domain.entities.CodeExecution
import com.ces.domain.types.*
import com.ces.domain.types.CodeExecutionFailureReason.NONE
import com.ces.domain.types.CodeExecutionState.*
import kotlinx.datetime.Clock.System.now
import org.apache.commons.lang3.RandomStringUtils.randomAlphabetic
import kotlin.random.Random

// TODO Consider moving test data into dedicated classes as the domain grows
class DomainTestData {

    companion object {

        fun aCreatedCodeExecution() = aMinimalCodeExecution().apply { state = CREATED }

        fun aStartedCodeExecution() = aMinimalCodeExecution().apply {
            state = STARTED
            executionLogsPath = aPath()
        }

        fun aCompletedCodeExecution() = aMinimalCodeExecution().apply {
            state = COMPLETED
            finishedAt = now()
            exitCode = anExitCode()
            executionLogsPath = aPath()
        }

        fun aFailedCodeExecution() = aMinimalCodeExecution().apply {
            state = FAILED
            finishedAt = now()
            exitCode = anExitCode()
            executionLogsPath = aPath()
            failureReason = aFailureReason()
        }

        private fun aMinimalCodeExecution() = CodeExecution.builder {
            id = CodeExecutionId.random()
            createdAt = now()
            state = aCodeExecutionState()
            sourceCodePath = aPath()
            language = aProgrammingLanguage()
            compiler = aCompiler()
        }

        fun aCodeExecutionRequestedEvent() = CodeExecutionRequestedEvent.builder {
            id = CodeExecutionId.random()
            createdAt = now()
            language = aProgrammingLanguage()
            compiler = aCompiler()
            sourceCodePath = aPath()
        }

        fun aCodeExecutionStartedEvent() = CodeExecutionStartedEvent.builder {
            id = CodeExecutionId.random()
            createdAt = now()
            executionLogsPath = aPath()
        }

        fun aCodeExecutionCompletedEvent() = CodeExecutionFinishedEvent.builder {
            id = CodeExecutionId.random()
            createdAt = now()
            state = COMPLETED
            exitCode = anExitCode()
        }

        fun aCodeExecutionFailedEvent() = aCodeExecutionCompletedEvent().apply {
            state = FAILED
            failureReason = aFailureReason()
        }

        fun aProgrammingLanguage() = ProgrammingLanguage.values().toList().shuffled()[0]
        fun aCompiler() = CodeCompilerType.values().toList().shuffled()[0]
        private fun aCodeExecutionState() = CodeExecutionState.values().toList().shuffled()[0]
        fun anExitCode() = Random.nextInt(0, 100)
        fun aFailureReason() = CodeExecutionFailureReason.values().filter { it != NONE }.shuffled()[0]
        fun aPath() = "/" + randomAlphabetic(10)
    }
}