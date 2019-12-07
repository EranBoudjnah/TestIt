package com.mitteloupe.testit.model

data class TypedParameter(val name: String, val type: DataType)

sealed class DataType(open val name: String, open val isNullable: Boolean) {
    data class Specific(
        override val name: String,
        override val isNullable: Boolean
    ) : DataType(name, isNullable)

    class Generic(
        override val name: String,
        override val isNullable: Boolean,
        vararg val genericTypes: DataType
    ) : DataType(name, isNullable) {
        override fun equals(other: Any?): Boolean {
            if (!super.equals(other)) return false

            other as Generic

            return genericTypes.contentEquals(other.genericTypes)
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + genericTypes.contentHashCode()
            return result
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataType

        if (name != other.name) return false
        if (isNullable != other.isNullable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + isNullable.hashCode()
        return result
    }
}
