package com.ces.domain.types

import com.ces.domain.types.CodeExecutionState.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class CodeExecutionStateTest : StringSpec({

    "should correctly identify not final states" {
        listOf(CREATED, STARTED)
            .forAll {
                it.isFinal().shouldBeFalse()
                it.isNotFinal().shouldBeTrue()
            }
    }

    "should correctly identify final states" {
        listOf(COMPLETED, FAILED)
            .forAll {
                it.isFinal().shouldBeTrue()
                it.isNotFinal().shouldBeFalse()
            }
    }
})
