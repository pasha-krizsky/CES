package com.ces.server.models

import com.ces.domain.types.CodeCompilerType
import com.ces.domain.types.ProgrammingLanguage
import kotlinx.serialization.Serializable

@Serializable
data class CodeExecutionRequest(
    val language: ProgrammingLanguage,
    val compiler: CodeCompilerType,
    val sourceCode: String,
)