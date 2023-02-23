package com.ces.domain.events

import com.ces.domain.types.CodeCompilerType
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.ProgrammingLanguage
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CodeExecutionRequestedEvent")
data class CodeExecutionRequestedEvent(
    override val id: CodeExecutionId,
    override val createdAt: Instant,
    val language: ProgrammingLanguage,
    val compiler: CodeCompilerType,
    val sourceCodePath: String,
) : CodeExecutionEvent() {

    private constructor(builder: Builder) : this(
        builder.id,
        builder.createdAt,
        builder.language,
        builder.compiler,
        builder.sourceCodePath
    )

    fun copy() = Builder().apply {
        id = this@CodeExecutionRequestedEvent.id
        createdAt = this@CodeExecutionRequestedEvent.createdAt
        language = this@CodeExecutionRequestedEvent.language
        compiler = this@CodeExecutionRequestedEvent.compiler
        sourceCodePath = this@CodeExecutionRequestedEvent.sourceCodePath
    }

    class Builder {
        lateinit var id: CodeExecutionId
        lateinit var createdAt: Instant
        lateinit var language: ProgrammingLanguage
        lateinit var compiler: CodeCompilerType
        lateinit var sourceCodePath: String

        fun build() = CodeExecutionRequestedEvent(this)
    }

    companion object {
        inline fun builder(block: Builder.() -> Unit) = Builder().apply(block)
    }
}