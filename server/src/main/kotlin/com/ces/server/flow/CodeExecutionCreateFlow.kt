package com.ces.server.flow

import com.ces.domain.entities.CodeExecution
import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.json.JsonConfig.Companion.encodeCodeExecutionEvent
import com.ces.domain.types.CodeExecutionId
import com.ces.domain.types.CodeExecutionState.CREATED
import com.ces.infrastructure.minio.ObjectStorage
import com.ces.infrastructure.rabbitmq.Message
import com.ces.infrastructure.rabbitmq.MessageQueue
import com.ces.server.config.ServerConfig
import com.ces.server.models.CodeExecutionRequest
import com.ces.server.storage.CodeExecutionDao
import kotlinx.datetime.Clock.System.now
import java.io.File
import java.io.File.separator

class CodeExecutionCreateFlow(
    private val config: ServerConfig,
    private val database: CodeExecutionDao,
    private val storage: ObjectStorage,
    private val requestQueue: MessageQueue,
) {

    suspend fun run(request: CodeExecutionRequest): CodeExecution {

        val codeExecutionId = CodeExecutionId.random()

        val tmpLocalPath = TMP_DIR + separator + codeExecutionId.value
        val tmpLocalFile = File(tmpLocalPath)
        tmpLocalFile.appendText(request.sourceCode)

        val storagePath = "${codeExecutionId.value}/$SOURCE_CODE_FILE_NAME"
        storage.uploadFile(config.codeExecutionBucketName, tmpLocalPath, storagePath)
        tmpLocalFile.delete()

        val event = CodeExecutionRequestedEvent(codeExecutionId, now(), request.language, request.compiler, storagePath)
        requestQueue.sendMessage(Message(encodeCodeExecutionEvent(event)))

        val codeExecution = CodeExecution(
            id = codeExecutionId,
            now(),
            CREATED,
            storagePath,
            request.language,
            request.compiler,
        )

        database.upsert(codeExecution)
        return codeExecution
    }

    companion object {
        private const val SOURCE_CODE_FILE_NAME = "source"
        private val TMP_DIR: String = System.getProperty("java.io.tmpdir")
    }
}