package com.ces.domain.events

import com.ces.domain.types.CodeExecutionId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
abstract class CodeExecutionEvent {
    abstract val id: CodeExecutionId
    abstract val createdAt: Instant
}