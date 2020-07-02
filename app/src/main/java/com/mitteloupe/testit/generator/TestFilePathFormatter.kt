package com.mitteloupe.testit.generator

import java.io.File

class TestFilePathFormatter {
    private val pathRegex by lazy {
        val separator = Regex.escape(File.separator)
        Regex("${separator}src${separator}[a-z]+${separator}(java|kotlin)$separator")
    }

    fun getTestFilePath(sourceFileName: String) =
        if (pathRegex.containsMatchIn(sourceFileName)) {
            val separator = File.separator
            val sourcePath = sourceFileName.substringBeforeLast(separator) + separator
            pathRegex.replace(sourcePath) { matchResult ->
                "${separator}src${separator}test${separator}${matchResult.groupValues[1]}$separator"
            }
        } else {
            null
        }
}
