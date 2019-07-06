package com.mitteloupe.testit.generator

class TestFilePathFormatter {
    private val pathRegex by lazy { Regex("/src/[a-z]+/(java|kotlin)/") }

    fun getTestFilePath(sourceFileName: String): String? {
        return if (pathRegex.containsMatchIn(sourceFileName)) {
            val sourcePath = sourceFileName.substringBeforeLast("/") + "/"
            pathRegex.replace(sourcePath) { matchResult ->
                "/src/test/${matchResult.groupValues[1]}/"
            }
        } else {
            null
        }
    }
}