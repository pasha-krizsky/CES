package com.ces.server.listener

import com.ces.domain.events.CodeExecutionEvent
import com.ces.domain.events.CodeExecutionFinishedEvent
import com.ces.domain.events.CodeExecutionStartedEvent
import com.ces.domain.json.JsonConfig
import com.ces.domain.types.CodeExecutionState.STARTED
import com.ces.infrastructure.rabbitmq.DeliveryId
import com.ces.infrastructure.rabbitmq.MessageQueue
import com.ces.server.storage.CodeExecutionDao

class CodeExecutionEventsListener(
    private val responseQueue: MessageQueue,
    private val database: CodeExecutionDao,
) {
    suspend fun run() {
        while (true) {
            val (messageId, request) = fetchCodeExecutionEvent()
            try {
                processEvent(request)
                responseQueue.markProcessed(messageId)
            } catch (e: Exception) {
                e.printStackTrace()
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
        database.upsert(updated)
    }
}