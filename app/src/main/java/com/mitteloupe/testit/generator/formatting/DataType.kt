package com.mitteloupe.testit.generator.formatting

import com.mitteloupe.testit.model.DataType

fun DataType.toKotlinString() = when (this) {
    is DataType.Specific -> name
    is DataType.Generic -> "$name<${genericTypes.toKotlinString()}>"
}

private fun Array<out DataType>.toKotlinString(): String =
    joinToString(", ") { dataType ->
        dataType.toKotlinString()
    }
