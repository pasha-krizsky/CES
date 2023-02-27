package com.ces.server.listener

import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionCompletedEvent
import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionFailedEvent
import com.ces.domain.events.DomainTestData.Companion.aCodeExecutionStartedEvent
import com.ces.domain.events.DomainTestData.Companion.aCreatedCodeExecution
import com.ces.domain.events.DomainTestData.Companion.aStartedCodeExecution
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeExecutionFailureReason.NONE
import com.ces.domain.types.CodeExecutionState.*
import com.ces.infrastructure.rabbitmq.Message
import com.ces.infrastructure.rabbitmq.MessageQueue
import com.ces.infrastructure.rabbitmq.RabbitMessageQueue
import com.ces.infrastructure.rabbitmq.RabbitmqExtension
import com.ces.server.config.ServerConfig
import com.ces.server.storage.CodeExecutionDao
import com.ces.server.storage.CodeExecutionInMemoryDao
import com.typesafe.config.ConfigFactory
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.config.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

val config = ServerConfig.from(HoconApplicationConfig(ConfigFactory.load()))

class CodeExecutionEventsListenerTest : StringSpec({
    extension(RabbitmqExtension())

    lateinit var responseQueue: MessageQueue
    lateinit var database: CodeExecutionDao
    lateinit var listener: CodeExecutionEventsListener

    beforeSpec {
        responseQueue = responseQueue()
        database = CodeExecutionInMemoryDao()
        listener = CodeExecutionEventsListener(responseQueue, database)
    }

    "should mark code execution as started" {
        val codeExecution = aCreatedCodeExecution().build()
        database.upsert(codeExecution)

        val startedEvent = aCodeExecutionStartedEvent().apply { id = codeExecution.id }.build()
        responseQueue.sendMessage(Message(encodeCodeExecutionEvent(startedEvent)))

        val listenerJob = launch {
            listener.run()
        }

        eventually(5.seconds) {
            val updated = database.get(codeExecution.id)
            updated.state shouldBe STARTED
            updated.executionLogsPath shouldBe startedEvent.executionLogsPath
        }

        listenerJob.cancelAndJoin()
    }

    "should mark code execution as completed" {
        val codeExecution = aStartedCodeExecution().build()
        database.upsert(codeExecution)

        val completedEvent = aCodeExecutionCompletedEvent().apply { id = codeExecution.id }.build()
        responseQueue.sendMessage(Message(encodeCodeExecutionEvent(completedEvent)))

        val listenerJob = launch {
            listener.run()
        }

        eventually(5.seconds) {
            val updated = database.get(codeExecution.id)
            updated.state shouldBe COMPLETED
            updated.finishedAt shouldBe completedEvent.createdAt
            updated.exitCode shouldBe completedEvent.exitCode
            updated.failureReason shouldBe NONE
        }

        listenerJob.cancelAndJoin()
    }

    "should mark code execution as failed" {
        val codeExecution = aStartedCodeExecution().build()
        database.upsert(codeExecution)

        val failedEvent = aCodeExecutionFailedEvent().apply { id = codeExecution.id }.build()
        responseQueue.sendMessage(Message(encodeCodeExecutionEvent(failedEvent)))

        val listenerJob = launch {
            listener.run()
        }

        eventually(5.seconds) {
            val updated = database.get(codeExecution.id)
            updated.state shouldBe FAILED
            updated.finishedAt shouldBe failedEvent.createdAt
            updated.exitCode shouldBe failedEvent.exitCode
            updated.failureReason shouldBe failedEvent.failureReason
        }

        listenerJob.cancelAndJoin()
    }
})

private fun responseQueue() = RabbitMessageQueue(
    config.codeExecutionResponseQueue.name,
    config.rabbitmq,
    config.codeExecutionResponseQueue.prefetchCount
)