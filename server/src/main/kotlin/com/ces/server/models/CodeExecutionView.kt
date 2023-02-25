package com.ces.server.models

import com.ces.domain.entities.CodeExecution
import com.ces.domain.types.*
import com.ces.domain.types.CodeExecutionFailureReason.NONE
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CodeExecutionView(
    val id: CodeExecutionId,
    val createdAt: Instant,
    val state: CodeExecutionState,
    val language: ProgrammingLanguage,
    val compiler: CodeCompilerType,

    val finishedAt: Instant? = null,
    val exitCode: Int? = null,
    val failureReason: CodeExecutionFailureReason = NONE,
) {
    companion object {
        fun from(codeExecution: CodeExecution) =
            CodeExecutionView(
                id = codeExecution.id,
                createdAt = codeExecution.createdAt,
                state = codeExecution.state,
                language = codeExecution.language,
                compiler = codeExecution.compiler,
                finishedAt = codeExecution.finishedAt,
                exitCode = codeExecution.exitCode,
                failureReason = codeExecution.failureReason,
            )
    }
}