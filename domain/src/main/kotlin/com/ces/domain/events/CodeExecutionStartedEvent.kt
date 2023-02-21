package com.ces.domain.events

import com.ces.domain.types.CodeExecutionId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CodeExecutionStartedEvent(
    val id: CodeExecutionId,
    val createdAt: Instant,
    val executionLogsPath: String,
)