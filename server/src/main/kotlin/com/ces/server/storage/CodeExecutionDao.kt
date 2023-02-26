package com.ces.server.storage

import com.ces.domain.entities.CodeExecution
import com.ces.domain.types.CodeExecutionId

interface CodeExecutionDao {

    suspend fun upsert(codeExecution: CodeExecution)

    suspend fun get(id: CodeExecutionId): CodeExecution
}