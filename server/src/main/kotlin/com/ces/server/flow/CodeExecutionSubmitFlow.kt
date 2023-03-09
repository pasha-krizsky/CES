package com.ces.server.flow

import com.ces.domain.entities.CodeExecution
import com.ces.domain.entities.CodeExecution.Companion.SOURCE_FILE_NAME
import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState.CREATED
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.Message
import com.ces.infrastructure.rabbitmq.SendQueue
import com.ces.server.config.ServerConfig
import com.ces.server.models.CodeExecutionRequest
import com.ces.server.dao.CodeExecutionDao
import kotlinx.datetime.Clock.System.now
import java.io.File
import java.io.File.separator

class CodeExecutionSubmitFlow(
    private val config: ServerConfig,
    private val database: CodeExecutionDao,
    private val storage: ObjectStorage,
    private val requestQueue: SendQueue,
) {

    suspend fun run(request: CodeExecutionRequest): CodeExecution {

        val codeExecutionId = CodeExecutionId.random()

        val tmpLocalPath = ServerConfig.tmpDir + separator + codeExecutionId.value
        val tmpLocalFile = File(tmpLocalPath)
        tmpLocalFile.appendText(request.sourceCode)

        val storagePath = "${codeExecutionId.value}/$SOURCE_FILE_NAME"
        storage.upload(config.codeExecutionBucketName, tmpLocalPath, storagePath)
        tmpLocalFile.delete()

        val codeExecution = CodeExecution(
            codeExecutionId,
            now(),
            CREATED,
            storagePath,
            request.language,
            request.compiler,
        )

        database.insert(codeExecution)

        val event = CodeExecutionRequestedEvent(codeExecutionId, now(), request.language, request.compiler, storagePath)
        requestQueue.sendMessage(Message(encodeCodeExecutionEvent(event)))

        return codeExecution
    }
}