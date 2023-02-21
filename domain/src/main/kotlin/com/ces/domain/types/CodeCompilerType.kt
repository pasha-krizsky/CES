package com.ces.domain.types

import com.ces.domain.types.ProgrammingLanguage.C_SHARP

enum class CodeCompilerType(
    private val supportedLanguages: List<ProgrammingLanguage>
) {
    MONO(listOf(C_SHARP));

    fun supports(language: ProgrammingLanguage) = language in supportedLanguages
}