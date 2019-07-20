package com.mitteloupe.testit.model

data class TypedParameter(val name: String, val type: DataType)

sealed class DataType(open val name: String) {
    data class Specific(override val name: String): DataType(name)
    class Generic(override val name: String, vararg val genericTypes: DataType): DataType(name)
}