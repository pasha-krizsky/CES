package com.ces.worker.domain.events

import com.ces.worker.domain.types.CodeExecutionFailureReason
import com.ces.worker.domain.types.CodeExecutionFailureReason.NONE
import com.ces.worker.domain.types.CodeExecutionId
import com.ces.worker.domain.types.CodeExecutionState
import com.ces.worker.domain.types.CodeExecutionState.FAILED
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CodeExecutionFinishedEvent(
    val id: CodeExecutionId,
    val createdAt: Instant,
    val state: CodeExecutionState,
    val exitCode: Int,
    val failureReason: CodeExecutionFailureReason = NONE,
) {
    init {
        check(state.isFinal()) { "state must be final" }
        check(state != FAILED || failureReason != NONE) { "failureReason must be set for failed state" }
        check(state != CodeExecutionState.COMPLETED || failureReason == NONE) { "failureReason must be empty for completed state" }
    }
}