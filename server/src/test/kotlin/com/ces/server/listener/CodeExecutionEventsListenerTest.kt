package com.ces.server.listener

import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionCompletedEvent
import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionFailedEvent
import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionStartedEvent
import com.ces.domain.events.DomainTestData.Companion.aCreatedCodeExecution
import com.ces.domain.events.DomainTestData.Companion.aStartedCodeExecution
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeExecutionFailureReason.NONE
import com.ces.domain.types.CodeExecutionState.*
import com.ces.infrastructure.database.DatabaseExtension
import com.ces.infrastructure.database.DatabaseFactory
import com.ces.infrastructure.rabbitmq.*
import com.ces.server.config.ServerConfig
import com.ces.server.dao.CodeExecutionDao
import com.ces.server.dao.CodeExecutions
import com.typesafe.config.ConfigFactory
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.config.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration.Companion.seconds

val config = ServerConfig.from(HoconApplicationConfig(ConfigFactory.load()))

class CodeExecutionEventsListenerTest : StringSpec({
    extension(DatabaseExtension(config.database))
    extension(RabbitmqExtension())

    lateinit var rabbitConnector: RabbitmqConnector

    lateinit var responseQueueOut: SendQueue
    lateinit var responseQueueIn: ReceiveQueue
    lateinit var codeExecutionDao: CodeExecutionDao

    lateinit var listener: CodeExecutionEventsListener

    beforeSpec {
        DatabaseFactory.init(config.database)
        transaction { SchemaUtils.create(CodeExecutions) }

        rabbitConnector = RabbitmqConnector(config.rabbitmq)

        responseQueueIn = responseInQueue(rabbitConnector)
        responseQueueOut = responseOutQueue(rabbitConnector)
        codeExecutionDao = CodeExecutionDao()

        listener = CodeExecutionEventsListener(responseQueueIn, codeExecutionDao)
    }

    "should mark code execution as started" {
        val codeExecution = aCreatedCodeExecution().build()
        codeExecutionDao.insert(codeExecution)

        val startedEvent = aCodeExecutionStartedEvent().apply { id = codeExecution.id }.build()
        responseQueueOut.sendMessage(Message(encodeCodeExecutionEvent(startedEvent)))

        val listenerJob = launch { listener.start() }

        eventually(5.seconds) {
            val updated = codeExecutionDao.get(codeExecution.id)
            updated.state shouldBe STARTED
            updated.logsPath shouldBe startedEvent.logsPath
        }

        listenerJob.cancelAndJoin()
    }

    "should mark code execution as completed" {
        val codeExecution = aStartedCodeExecution().build()
        codeExecutionDao.insert(codeExecution)

        val completedEvent = aCodeExecutionCompletedEvent().apply { id = codeExecution.id }.build()
        responseQueueOut.sendMessage(Message(encodeCodeExecutionEvent(completedEvent)))

        val listenerJob = launch { listener.start() }

        eventually(5.seconds) {
            val updated = codeExecutionDao.get(codeExecution.id)
            updated.state shouldBe COMPLETED
            updated.exitCode shouldBe completedEvent.exitCode
            updated.failureReason shouldBe NONE
        }

        listenerJob.cancelAndJoin()
    }

    "should mark code execution as failed" {
        val codeExecution = aStartedCodeExecution().build()
        codeExecutionDao.insert(codeExecution)

        val failedEvent = aCodeExecutionFailedEvent().apply { id = codeExecution.id }.build()
        responseQueueOut.sendMessage(Message(encodeCodeExecutionEvent(failedEvent)))

        val listenerJob = launch { listener.start() }

        eventually(5.seconds) {
            val updated = codeExecutionDao.get(codeExecution.id)
            updated.state shouldBe FAILED
            updated.exitCode shouldBe failedEvent.exitCode
            updated.failureReason shouldBe failedEvent.failureReason
        }

        listenerJob.cancelAndJoin()
    }
})

private fun responseInQueue(connector: RabbitmqConnector) = RabbitReceiveQueue(
    config.codeExecutionResponseQueue.name, connector
)

private fun responseOutQueue(connector: RabbitmqConnector) = RabbitSendQueue(
    config.codeExecutionResponseQueue.name, connector
)
