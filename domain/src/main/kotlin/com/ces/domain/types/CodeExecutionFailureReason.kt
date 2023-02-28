package com.ces.domain.types

enum class CodeExecutionFailureReason {
    NONE,
    TIME_LIMIT_EXCEEDED,
    NON_ZERO_EXIT_CODE,
    INTERNAL_ERROR,
}