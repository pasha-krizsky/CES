package com.ces.worker.domain.events

import com.ces.worker.domain.types.CodeExecutionId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CodeExecutionStartedEvent(
    val id: CodeExecutionId,
    val createdAt: Instant,
    val executionLogsPath: String,
)