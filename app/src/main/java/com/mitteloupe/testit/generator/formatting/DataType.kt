package com.mitteloupe.testit.generator.formatting

import com.mitteloupe.testit.model.DataType

fun DataType.toNonNullableKotlinString() = when (this) {
    is DataType.Specific -> name
    is DataType.Generic -> "$name<${genericTypes.toNonNullableKotlinString()}>"
    is DataType.Lambda -> "(${inputParameterTypes.toNonNullableKotlinString()}) -> $name"
}

private fun Array<out DataType>.toNonNullableKotlinString(): String =
    joinToString(", ") { dataType ->
        dataType.toNonNullableKotlinString()
    }

fun DataType.toKotlinString() = when (this) {
    is DataType.Specific -> name
    is DataType.Generic -> "$name<${genericTypes.toKotlinString()}>"
    is DataType.Lambda -> "(${inputParameterTypes.toKotlinString()}) -> $name"
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
