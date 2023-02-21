package com.ces.domain.events

import com.ces.domain.types.CodeCompilerType
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.ProgrammingLanguage
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CodeExecutionRequestedEvent(
    val id: CodeExecutionId,
    val createdAt: Instant,
    val language: ProgrammingLanguage,
    val compiler: CodeCompilerType,
    val sourceCodePath: String,
)