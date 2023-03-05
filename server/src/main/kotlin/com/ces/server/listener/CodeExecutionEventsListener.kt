package com.ces.server.listener

import com.ces.domain.entities.CodeExecution
import com.ces.domain.events.CodeExecutionEvent
import com.ces.domain.events.CodeExecutionFinishedEvent
import com.ces.domain.events.CodeExecutionStartedEvent
import com.ces.domain.json.JsonConfig.Companion.decodeCodeExecutionEvent
import com.ces.domain.types.CodeExecutionState.STARTED
import com.ces.infrastructure.rabbitmq.DeliveryId
import com.ces.infrastructure.rabbitmq.ReceiveQueue
import com.ces.server.dao.CodeExecutionDao
import mu.KotlinLogging

class CodeExecutionEventsListener(
    private val responseQueue: ReceiveQueue,
    private val database: CodeExecutionDao,
) {

    private val log = KotlinLogging.logger {}

    suspend fun start() {
        while (true) {
            val (messageId, request) = fetchCodeExecutionEvent()
            log.debug { "Start processing code execution event, messageId=$messageId" }
            try {
                processEvent(request)
                responseQueue.markProcessed(messageId)
                log.debug { "Finished processing code execution event, messageId=$messageId" }
            } catch (e: Exception) {
                log.error(e) { "Failed to process code execution event" }
                responseQueue.markUnprocessed(messageId, false)
            }
        }
    }

    private suspend inline fun fetchCodeExecutionEvent(): Pair<DeliveryId, CodeExecutionEvent> {
        val message = responseQueue.receiveMessage()
        val event = decodeCodeExecutionEvent(message.content)
        return Pair(message.deliveryId, event)
    }

    private suspend fun processEvent(event: CodeExecutionEvent) {
        when (event) {
            is CodeExecutionStartedEvent -> processStartedEvent(event)
            is CodeExecutionFinishedEvent -> processFinishedEvent(event)
            else -> throw IllegalStateException("Failed to process event of ${event.javaClass.simpleName} type")
        }
    }

    private suspend fun processStartedEvent(event: CodeExecutionStartedEvent) {
        val codeExecutionId = event.id
        val codeExecution = database.get(codeExecutionId)
        val updatedCodeExecution = codeExecution.copy().apply {
            state = STARTED
            logsPath = event.logsPath
        }.build()

        update(updatedCodeExecution)
    }

    private suspend fun processFinishedEvent(event: CodeExecutionFinishedEvent) {
        val codeExecutionId = event.id
        val codeExecution = database.get(codeExecutionId)
        val updatedCodeExecution = codeExecution.copy().apply {
            state = event.state
            exitCode = event.exitCode
            failureReason = event.failureReason
            finishedAt = event.createdAt
        }.build()

        update(updatedCodeExecution)
    }

    private suspend fun update(codeExecution: CodeExecution) {
        log.debug { "Update code execution: $codeExecution" }
        database.update(codeExecution)
    }
}