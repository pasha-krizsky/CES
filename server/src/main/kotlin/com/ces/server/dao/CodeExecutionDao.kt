package com.ces.server.dao

import com.ces.domain.entities.CodeExecution
import com.ces.domain.types.*
import io.ktor.server.plugins.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.sql.Connection.TRANSACTION_REPEATABLE_READ

class CodeExecutionDao {

    suspend fun insert(codeExecution: CodeExecution) = query {
        CodeExecutions.insert {
            it[id] = codeExecution.id.value
            it[createdAt] = codeExecution.createdAt
            it[state] = codeExecution.state.name
            it[sourceCodePath] = codeExecution.sourceCodePath
            it[language] = codeExecution.language.name
            it[compiler] = codeExecution.compiler.name
            it[finishedAt] = codeExecution.finishedAt
            it[exitCode] = codeExecution.exitCode
            it[allLogsPath] = codeExecution.logsPath?.allPath
            it[stdoutLogsPath] = codeExecution.logsPath?.stdoutPath
            it[stderrLogsPath] = codeExecution.logsPath?.stderrPath
            it[failureReason] = codeExecution.failureReason.name
        }
    }

    suspend fun update(codeExecution: CodeExecution) = query {
        CodeExecutions.update({ CodeExecutions.id eq codeExecution.id.value }) {
            it[id] = codeExecution.id.value
            it[createdAt] = codeExecution.createdAt
            it[state] = codeExecution.state.name
            it[sourceCodePath] = codeExecution.sourceCodePath
            it[language] = codeExecution.language.name
            it[compiler] = codeExecution.compiler.name
            it[finishedAt] = codeExecution.finishedAt
            it[exitCode] = codeExecution.exitCode
            it[allLogsPath] = codeExecution.logsPath?.allPath
            it[stdoutLogsPath] = codeExecution.logsPath?.stdoutPath
            it[stderrLogsPath] = codeExecution.logsPath?.stderrPath
            it[failureReason] = codeExecution.failureReason.name
        }
    }

    suspend fun get(id: CodeExecutionId): CodeExecution = query {
        val row = CodeExecutions.select { CodeExecutions.id eq id.value }.singleOrNull() ?: throw NotFoundException()

        val allPath = row[CodeExecutions.allLogsPath]
        val stdoutPath = row[CodeExecutions.stdoutLogsPath]
        val stderrPath = row[CodeExecutions.stderrLogsPath]

        return@query CodeExecution(
            CodeExecutionId(row[CodeExecutions.id]),
            row[CodeExecutions.createdAt],
            CodeExecutionState.valueOf(row[CodeExecutions.state]),
            row[CodeExecutions.sourceCodePath],
            ProgrammingLanguage.valueOf(row[CodeExecutions.language]),
            CodeCompilerType.valueOf(row[CodeExecutions.compiler]),
            row[CodeExecutions.finishedAt],
            row[CodeExecutions.exitCode],
            if (allPath != null) CodeExecutionLogsPath(allPath, stdoutPath!!, stderrPath!!) else null,
            CodeExecutionFailureReason.valueOf(row[CodeExecutions.failureReason])
        )
    }

    private suspend fun <T> query(block: () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, transactionIsolation = TRANSACTION_REPEATABLE_READ) { block() }
}

object CodeExecutions : Table() {
    val id = uuid("id")

    val createdAt = timestamp("createdAt")
    val state = varchar("state", 30)
    val sourceCodePath = text("sourceCodePath")
    val language = varchar("language", 30)
    val compiler = varchar("compiler", 30)

    val finishedAt = timestamp("finishedAt").nullable()
    val exitCode = integer("exitCode").nullable()
    val allLogsPath = text("allLogsPath").nullable()
    val stdoutLogsPath = text("stdoutLogsPath").nullable()
    val stderrLogsPath = text("stderrLogsPath").nullable()

    val failureReason = varchar("failureReason", 30)

    override val primaryKey = PrimaryKey(id)
}