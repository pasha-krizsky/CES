package com.ces.server.storage

import com.ces.domain.entities.CodeExecution
import com.ces.domain.types.CodeExecutionId
import io.ktor.server.plugins.*
import java.util.concurrent.ConcurrentHashMap

// TODO Consider using real database
class CodeExecutionInMemoryDao : CodeExecutionDao {

    private val database = ConcurrentHashMap<CodeExecutionId, CodeExecution>()

    override suspend fun upsert(codeExecution: CodeExecution) {
        database[codeExecution.id] = codeExecution
    }

    override suspend fun get(id: CodeExecutionId) = database[id] ?: throw NotFoundException()
}