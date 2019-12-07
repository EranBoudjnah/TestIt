package com.mitteloupe.testit.terminal.mapper

import com.mitteloupe.testit.terminal.model.RunParameters

class ArgsToRunParameters {
    fun toParameters(args: Array<String>) =
        RunParameters(
            filePath = getFilePath(args),
            parameterized = getParameterizedFlag(args)
        )

    private fun getParameterizedFlag(args: Array<String>) =
        if (args.size > 1) {
            args.drop(1)
                .any { it == "-p" }
        } else {
            false
        }

    private fun getFilePath(args: Array<String>) =
        if (args.isNotEmpty()) {
            args[0].trim()
        } else {
            null
        }
}
