package com.ces.domain.types

import kotlinx.serialization.Serializable

@Serializable
data class CodeExecutionLogsPath(val allPath: String, val stdoutPath: String, val stderrPath: String)