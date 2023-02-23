package com.ces.domain.events

import com.ces.domain.types.CodeExecutionFailureReason
import com.ces.domain.types.CodeExecutionFailureReason.NONE
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState
import com.ces.domain.types.CodeExecutionState.COMPLETED
import com.ces.domain.types.CodeExecutionState.FAILED
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CodeExecutionFinishedEvent")
data class CodeExecutionFinishedEvent(
    override val id: CodeExecutionId,
    override val createdAt: Instant,
    val state: CodeExecutionState,
    val exitCode: Int,
    val failureReason: CodeExecutionFailureReason = NONE,
) : CodeExecutionEvent() {
    init {
        check(state.isFinal()) { "state must be final" }
        check(state != FAILED || failureReason != NONE) { "failureReason must be set for failed state" }
        check(state != COMPLETED || failureReason == NONE) { "failureReason must be empty for completed state" }
    }

    private constructor(builder: Builder) : this(
        builder.id,
        builder.createdAt,
        builder.state,
        builder.exitCode,
        builder.failureReason,
    )

    fun copy() = Builder().apply {
        id = this@CodeExecutionFinishedEvent.id
        createdAt = this@CodeExecutionFinishedEvent.createdAt
        state = this@CodeExecutionFinishedEvent.state
        exitCode = this@CodeExecutionFinishedEvent.exitCode
        failureReason = this@CodeExecutionFinishedEvent.failureReason
    }

    class Builder {
        lateinit var id: CodeExecutionId
        lateinit var createdAt: Instant
        lateinit var state: CodeExecutionState
        var exitCode: Int = 0
        var failureReason: CodeExecutionFailureReason = NONE

        fun build() = CodeExecutionFinishedEvent(this)
    }

    companion object {
        inline fun builder(block: Builder.() -> Unit) = Builder().apply(block)
    }
}