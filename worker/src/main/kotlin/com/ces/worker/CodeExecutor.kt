package com.ces.worker

import com.ces.worker.flow.CodeExecutionFlow

class CodeExecutor(
    private val codeExecutionFlow: CodeExecutionFlow,
) {
    suspend fun run() {
        while (true) {
            codeExecutionFlow.run()
        }
    }
}