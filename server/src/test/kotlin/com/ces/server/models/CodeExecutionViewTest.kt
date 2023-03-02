package com.ces.server.models

import com.ces.domain.events.DomainTestData.Companion.aCompletedCodeExecution
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CodeExecutionViewTest : StringSpec({
    "should create code execution view from code execution entity" {
        val codeExecution = aCompletedCodeExecution().build()

        val view = CodeExecutionView.from(codeExecution)

        view.id shouldBe codeExecution.id
        view.createdAt shouldBe codeExecution.createdAt
        view.state shouldBe codeExecution.state
        view.language shouldBe codeExecution.language
        view.compiler shouldBe codeExecution.compiler
        view.finishedAt shouldBe codeExecution.finishedAt
        view.exitCode shouldBe codeExecution.exitCode
        view.failureReason shouldBe codeExecution.failureReason
    }
})