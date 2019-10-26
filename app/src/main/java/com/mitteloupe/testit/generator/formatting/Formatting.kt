package com.mitteloupe.testit.generator.formatting

private const val INDENT = "    "

class Formatting(
    private val indentationString: String = INDENT
) {
    fun getIndentation(indentation: Int = 1) = indentationString * indentation

    private operator fun String.times(multiplier: Int) = repeat(multiplier)
}
