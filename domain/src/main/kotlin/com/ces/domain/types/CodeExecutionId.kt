package com.ces.domain.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*
import java.util.UUID.randomUUID

@Serializable(with = CodeExecutionIdSerializer::class)
data class CodeExecutionId(val value: UUID) {
    companion object {
        fun random() = CodeExecutionId(randomUUID())
    }
}

object CodeExecutionIdSerializer : KSerializer<CodeExecutionId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CodeExecutionId", STRING)
    override fun serialize(encoder: Encoder, value: CodeExecutionId) = encoder.encodeString(value.value.toString())
    override fun deserialize(decoder: Decoder) = CodeExecutionId(UUID.fromString(decoder.decodeString()))
}