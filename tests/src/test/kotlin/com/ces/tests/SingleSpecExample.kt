package com.ces.tests

import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import kotlin.math.max

class StringSpecExample : StringSpec({
    "maximum of two numbers" {
        forAll(
            row(1, 5, 5),
            row(1, 0, 1),
            row(0, 0, 1)
        ) { a, b, max ->
            max(a, b) shouldBe max
        }
    }
})