package com.ces.domain.events

import com.ces.domain.types.CodeExecutionId
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CodeExecutionStartedEvent")
data class CodeExecutionStartedEvent(
    override val id: CodeExecutionId,
    override val createdAt: Instant,
    val executionLogsPath: String,
) : CodeExecutionEvent() {

    private constructor(builder: Builder) : this(
        builder.id,
        builder.createdAt,
        builder.executionLogsPath,
    )

    fun copy() = Builder().apply {
        id = this@CodeExecutionStartedEvent.id
        createdAt = this@CodeExecutionStartedEvent.createdAt
        executionLogsPath = this@CodeExecutionStartedEvent.executionLogsPath
    }

    class Builder {
        lateinit var id: CodeExecutionId
        lateinit var createdAt: Instant
        lateinit var executionLogsPath: String

        fun build() = CodeExecutionStartedEvent(this)
    }

    companion object {
        inline fun builder(block: Builder.() -> Unit) = Builder().apply(block)
    }
}
