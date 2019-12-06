package com.mitteloupe.testit.generator.formatting

import com.mitteloupe.testit.model.DataType

fun DataType.toKotlinString() = when (this) {
    is DataType.Specific -> name
    is DataType.Generic -> "$name<${genericTypes.toKotlinString()}>"
}.makeNullableIfTrue(isNullable)

private fun Array<out DataType>.toKotlinString(): String =
    joinToString(", ") { dataType ->
        dataType.toKotlinString()
    }

private fun String.makeNullableIfTrue(isNullable: Boolean) =
    if (isNullable) {
        "$this?"
    } else {
        this
    }