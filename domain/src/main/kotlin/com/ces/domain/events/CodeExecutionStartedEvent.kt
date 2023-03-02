package com.ces.domain.events

import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionLogsPath
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CodeExecutionStartedEvent")
data class CodeExecutionStartedEvent(
    override val id: CodeExecutionId,
    override val createdAt: Instant,
    val logsPath: CodeExecutionLogsPath,
) : CodeExecutionEvent() {

    private constructor(builder: Builder) : this(
        builder.id,
        builder.createdAt,
        builder.logsPath,
    )

    fun copy() = Builder().apply {
        id = this@CodeExecutionStartedEvent.id
        createdAt = this@CodeExecutionStartedEvent.createdAt
        logsPath = this@CodeExecutionStartedEvent.logsPath
    }

    class Builder {
        lateinit var id: CodeExecutionId
        lateinit var createdAt: Instant
        lateinit var logsPath: CodeExecutionLogsPath

        fun build() = CodeExecutionStartedEvent(this)
    }

    companion object {
        inline fun builder(block: Builder.() -> Unit) = Builder().apply(block)
    }
}
