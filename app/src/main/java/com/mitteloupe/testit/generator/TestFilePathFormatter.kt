package com.mitteloupe.testit.generator

import java.io.File

class TestFilePathFormatter(
    private val fileSeparator: String = File.separator
) {
    private val pathRegex by lazy {
        val separator = Regex.escape(fileSeparator)
        Regex("($separator)src\\1[a-zA-Z]+\\1(?:java|kotlin)\\1")
    }

    fun getTestFilePath(sourceFileName: String) =
        if (pathRegex.containsMatchIn(sourceFileName)) {
            val sourcePath = sourceFileName.substringBeforeLast(fileSeparator) + fileSeparator
            pathRegex.replace(sourcePath) { matchResult ->
                "${fileSeparator}src${fileSeparator}test$fileSeparator${matchResult.groupValues[1]}$fileSeparator"
            }
        } else {
            null
        }
}
