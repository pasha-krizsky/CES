package com.ces.domain.entities

import com.ces.domain.types.*
import com.ces.domain.types.CodeExecutionFailureReason.NONE
import com.ces.domain.types.CodeExecutionState.CREATED
import com.ces.domain.types.CodeExecutionState.FAILED
import kotlinx.datetime.Instant

data class CodeExecution(
    val id: CodeExecutionId,
    val createdAt: Instant,
    val state: CodeExecutionState,
    val sourceCodePath: String,
    val language: ProgrammingLanguage,
    val compiler: CodeCompilerType,

    val finishedAt: Instant? = null,
    val exitCode: Int? = null,
    val logsPath: CodeExecutionLogsPath? = null,
    val failureReason: CodeExecutionFailureReason = NONE,
) {

    init {
        validate()
    }

    private fun validate() {
        check(compiler.supports(language)) { "compiler '$compiler' does not support '$language' language" }

        check(state != FAILED || failureReason != NONE) { "failureReason must be set for failed state" }
        check(state == FAILED || failureReason == NONE) { "failureReason must be empty for not failed state" }

        check(state.isNotFinal() || finishedAt != null) { "finishedAt must be set for final state" }
        check(state.isFinal() || finishedAt == null) { "finishedAt must be empty for not final state" }

        check(state == CREATED || logsPath != null) { "logsPath must be set for $state state" }
        check(state != CREATED || logsPath == null) { "logsPath must be empty for $state state" }
    }

    private constructor(builder: Builder) : this(
        builder.id,
        builder.createdAt,
        builder.state,
        builder.sourceCodePath,
        builder.language,
        builder.compiler,
        builder.finishedAt,
        builder.exitCode,
        builder.logsPath,
        builder.failureReason,
    )

    fun copy() = Builder().apply {
        id = this@CodeExecution.id
        createdAt = this@CodeExecution.createdAt
        state = this@CodeExecution.state
        sourceCodePath = this@CodeExecution.sourceCodePath
        language = this@CodeExecution.language
        compiler = this@CodeExecution.compiler
        finishedAt = this@CodeExecution.finishedAt
        exitCode = this@CodeExecution.exitCode
        logsPath = this@CodeExecution.logsPath
        failureReason = this@CodeExecution.failureReason
    }

    class Builder {
        lateinit var id: CodeExecutionId
        lateinit var createdAt: Instant
        lateinit var state: CodeExecutionState
        lateinit var sourceCodePath: String
        lateinit var language: ProgrammingLanguage
        lateinit var compiler: CodeCompilerType

        var finishedAt: Instant? = null
        var exitCode: Int? = null
        var logsPath: CodeExecutionLogsPath? = null
        var failureReason: CodeExecutionFailureReason = NONE

        fun build() = CodeExecution(this)
    }

    companion object {
        inline fun builder(block: Builder.() -> Unit) = Builder().apply(block)

        const val SOURCE_FILE_NAME = "source"
        const val ALL_LOGS_FILE_NAME = "all_logs"
        const val STDOUT_LOGS_FILE_NAME = "stdout_logs"
        const val STDERR_LOGS_FILE_NAME = "stderr_logs"
    }
}