package com.ces.domain.types

import com.ces.domain.json.JsonConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.UUID.randomUUID

class CodeExecutionIdTest : StringSpec({

    val json = JsonConfig.json

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

        val encoded = json.encodeToString(source)

        encoded shouldBe "\"${source.value}\""
    }

    "serialize and deserialize should be symmetrical" {
        val source = CodeExecutionId.random()

        val decoded = json.decodeFromString<CodeExecutionId>(json.encodeToString(source))

        decoded shouldBe source
    }
})
