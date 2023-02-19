package com.ces.worker.domain.entities

import com.ces.worker.domain.types.CodeExecutionFailureReason
import com.ces.worker.domain.types.CodeExecutionFailureReason.NONE
import com.ces.worker.domain.types.CodeExecutionId
import com.ces.worker.domain.types.CodeExecutionState
import com.ces.worker.domain.types.CodeExecutionState.*
import kotlinx.datetime.Instant

data class CodeExecution(
    val id: CodeExecutionId,
    val createdAt: Instant,
    val state: CodeExecutionState,
    val sourceCodePath: String,

    val finishedAt: Instant? = null,
    val exitCode: Int? = null,
    val executionLogsPath: String? = null,
    val failureReason: CodeExecutionFailureReason = NONE,
) {

    init {
        validate()
    }

    private fun validate() {
        check(state != FAILED || failureReason != NONE) { "failureReason must be set for failed state" }
        check(state != COMPLETED || failureReason == NONE) { "failureReason must be empty for completed state" }

        check(state.isNotFinal() || finishedAt != null) { "finishedAt must be set for final state" }
        check(state.isFinal() || finishedAt == null) { "finishedAt must be empty for not final state" }

        check(state.isNotFinal() || exitCode != null) { "exitCode must be set for final state" }
        check(state.isFinal() || exitCode == null) { "exitCode must be empty for not final state" }

        check(state == CREATED || executionLogsPath != null) { "executionLogsPath must be set for $state state" }
    }
}