package com.ces.domain.json

import com.ces.domain.events.CodeExecutionEvent
import com.ces.domain.events.CodeExecutionFinishedEvent
import com.ces.domain.events.CodeExecutionRequestedEvent
import com.ces.domain.events.CodeExecutionStartedEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class JsonConfig {
    companion object {

        val json = Json {
            serializersModule = SerializersModule {
                polymorphic(CodeExecutionEvent::class) {
                    subclass(CodeExecutionRequestedEvent::class)
                    subclass(CodeExecutionStartedEvent::class)
                    subclass(CodeExecutionFinishedEvent::class)
                }
            }
        }

        fun encodeCodeExecutionEvent(event: CodeExecutionEvent) = json.encodeToString(event)
        fun decodeCodeExecutionEvent(event: String) = json.decodeFromString<CodeExecutionEvent>(event)
    }
}