package com.ces.server.models

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
)