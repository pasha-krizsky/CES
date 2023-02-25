package com.ces.server.storage

import com.ces.domain.entities.CodeExecution
import com.ces.domain.types.CodeExecutionId
import io.ktor.server.plugins.*
import java.util.concurrent.ConcurrentHashMap

class CodeExecutionStorage {

    private val storage = ConcurrentHashMap<CodeExecutionId, CodeExecution>()

    fun store(codeExecution: CodeExecution) {
        storage[codeExecution.id] = codeExecution
    }

    fun get(id: CodeExecutionId): CodeExecution = storage[id] ?: throw NotFoundException()
}