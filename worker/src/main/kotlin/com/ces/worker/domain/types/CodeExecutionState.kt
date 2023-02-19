package com.ces.worker.domain.types

enum class CodeExecutionState {
    CREATED,
    STARTED,
    COMPLETED,
    FAILED,
    ;

    fun isFinal() = this == COMPLETED || this == FAILED
    fun isNotFinal() = !isFinal()
}