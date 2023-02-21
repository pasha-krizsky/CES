package com.ces.domain.types

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID.randomUUID

class CodeExecutionIdTest : StringSpec({
    "should create" {
        val randomId = randomUUID()

        val codeExecutionId = CodeExecutionId(randomId)

        codeExecutionId.value shouldBe randomId
    }

    "should create random" {
        val first = CodeExecutionId.random()
        val second = CodeExecutionId.random()

        first.value shouldNotBe second.value
    }

    "should serialize" {
        val source = CodeExecutionId.random()

        val encoded = Json.encodeToString(source)

        encoded shouldBe "\"${source.value}\""
    }

    "serialize and deserialize should be symmetrical" {
        val source = CodeExecutionId.random()

        val decoded = Json.decodeFromString<CodeExecutionId>(Json.encodeToString(source))

        decoded shouldBe source
    }
})
