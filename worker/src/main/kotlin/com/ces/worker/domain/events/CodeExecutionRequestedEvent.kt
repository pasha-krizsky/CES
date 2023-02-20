package com.ces.worker.domain.events

import com.ces.worker.domain.types.CodeExecutionId
import com.ces.worker.domain.types.CodeCompilerType
import com.ces.worker.domain.types.ProgrammingLanguage
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