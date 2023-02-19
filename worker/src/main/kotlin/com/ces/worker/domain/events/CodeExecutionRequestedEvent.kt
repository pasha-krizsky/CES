package com.ces.worker.domain.events

import com.ces.worker.domain.types.CodeExecutionId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CodeExecutionRequestedEvent(
    val id: CodeExecutionId,
    val createdAt: Instant,
    val sourceCodePath: String,
)