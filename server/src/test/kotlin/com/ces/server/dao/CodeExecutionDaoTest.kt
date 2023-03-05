package com.ces.server.dao

import com.ces.domain.entities.CodeExecution
import com.ces.domain.events.DomainTestData.Companion.aCompletedCodeExecution
import com.ces.domain.events.DomainTestData.Companion.aCreatedCodeExecution
import com.ces.domain.events.DomainTestData.Companion.aStartedCodeExecution
import com.ces.infrastructure.database.DatabaseExtension
import com.ces.infrastructure.database.DatabaseFactory
import com.ces.server.config.ServerConfig
import com.typesafe.config.ConfigFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

val config = ServerConfig.from(HoconApplicationConfig(ConfigFactory.load()))

class CodeExecutionDaoTest : StringSpec({

    extension(DatabaseExtension(config.database))

    lateinit var codeExecutionDao: CodeExecutionDao

    beforeSpec {
        DatabaseFactory.init(config.database)
        transaction { SchemaUtils.create(CodeExecutions) }
        codeExecutionDao = CodeExecutionDao()
    }

    "should create and get" {
        val codeExecution = aCompletedCodeExecution().build()

        codeExecutionDao.insert(codeExecution)
        val storedCodeExecution = codeExecutionDao.get(codeExecution.id)

        shouldBeEqual(storedCodeExecution, codeExecution)
    }

    "should update and get" {
        val createdExecution = aCreatedCodeExecution().build()

        codeExecutionDao.insert(createdExecution)
        val startedExecution = aStartedCodeExecution().apply { id = createdExecution.id }.build()
        codeExecutionDao.update(startedExecution)

        val updatedCodeExecution = codeExecutionDao.get(startedExecution.id)

        shouldBeEqual(updatedCodeExecution, startedExecution)
    }
})

// TODO timestamp precision is truncated by Postgres,
// need a better solution than just ignoring timestamps
private fun shouldBeEqual(first: CodeExecution, second: CodeExecution) {
    first.id shouldBe second.id
    first.state shouldBe second.state
    first.sourceCodePath shouldBe second.sourceCodePath
    first.language shouldBe second.language
    first.compiler shouldBe second.compiler
    first.exitCode shouldBe second.exitCode
    first.logsPath shouldBe second.logsPath
    first.failureReason shouldBe second.failureReason
}