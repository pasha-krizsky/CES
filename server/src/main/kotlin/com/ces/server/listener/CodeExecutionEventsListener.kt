package com.ces.server.listener

import com.ces.domain.events.CodeExecutionEvent
import com.ces.domain.events.CodeExecutionFinishedEvent
import com.ces.domain.events.CodeExecutionStartedEvent
import com.ces.domain.json.JsonConfig
import com.ces.domain.types.CodeExecutionState.STARTED
import com.ces.infrastructure.rabbitmq.DeliveryId
import com.ces.infrastructure.rabbitmq.MessageQueue
import com.ces.server.storage.CodeExecutionDao
import mu.KotlinLogging

class CodeExecutionEventsListener(
    private val responseQueue: MessageQueue,
    private val database: CodeExecutionDao,
) {

    private val log = KotlinLogging.logger {}

    suspend fun run() {
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
        val event = JsonConfig.decodeCodeExecutionEvent(message.content)
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
        val updated = codeExecution.copy().apply {
            state = STARTED
            executionLogsPath = event.executionLogsPath
        }.build()
        log.debug { "Upsert code execution: $updated" }
        database.upsert(updated)
    }

    private suspend fun processFinishedEvent(event: CodeExecutionFinishedEvent) {
        val codeExecutionId = event.id
        val codeExecution = database.get(codeExecutionId)
        val updated = codeExecution.copy().apply {
            state = event.state
            exitCode = event.exitCode
            failureReason = event.failureReason
            finishedAt = event.createdAt
        }.build()
        log.debug { "Upsert code execution: $updated" }
        database.upsert(updated)
    }
}