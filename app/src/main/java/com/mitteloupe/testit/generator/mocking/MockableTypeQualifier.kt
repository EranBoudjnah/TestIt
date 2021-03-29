package com.mitteloupe.testit.generator.mocking

import com.mitteloupe.testit.generator.formatting.toKotlinString
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.TypedParameter

class MockableTypeQualifier {
    private val nonMockableTypes = listOf(
        ConcreteValue("Boolean") { _, _ -> "false" },
        ConcreteValue("Byte") { _, _ -> "0b0" },
        ConcreteValue("Class") { _, _ -> "Any::class.java" },
        ConcreteValue("Double") { _, _ -> "0.0" },
        ConcreteValue("Float") { _, _ -> "0f" },
        ConcreteValue("Int") { _, _ -> "0" },
        ConcreteValue("Integer") { _, _ -> "0 as Integer" },
        ConcreteValue("Long") { _, _ -> "0L" },
        ConcreteValue("Short") { _, _ -> "0.toShort()" },
        ConcreteValue("String") { parameterName, _ -> "\"$parameterName\"" },
        ConcreteValue("Array") { _, parameterType ->
            getCodeForCollectionOf(
                "arrayOf",
                parameterType
            )
        },
        ConcreteValue("List") { _, parameterType ->
            getCodeForCollectionOf(
                "listOf",
                parameterType
            )
        },
        ConcreteValue("MutableList") { _, parameterType ->
            getCodeForCollectionOf(
                "mutableListOf",
                parameterType
            )
        },
        ConcreteValue("Map") { _, parameterType ->
            getCodeForCollectionOf(
                "mapOf",
                parameterType
            )
        },
        ConcreteValue("MutableMap") { _, parameterType ->
            getCodeForCollectionOf(
                "mutableMapOf",
                parameterType
            )
        },
        ConcreteValue("Set") { _, parameterType ->
            getCodeForCollectionOf(
                "setOf",
                parameterType
            )
        },
        ConcreteValue("MutableSet") { _, parameterType ->
            getCodeForCollectionOf(
                "mutableSetOf",
                parameterType
            )
        },
        ConcreteValue("Unit") { _, _ -> "Unit" }
    )

    fun getNonMockableType(dataType: DataType) = if (dataType is DataType.Lambda) {
        val lambdaArguments = dataType.lambdaArguments()
        val lambdaContent = if (lambdaArguments.isEmpty()) {
            ""
        } else {
            " $lambdaArguments -> "
        }
        ConcreteValue("(?)->?") { _, _ -> "{$lambdaContent}" }
    } else {
        nonMockableTypes.firstOrNull { type -> type.dataType == dataType.name }
    }

    fun isMockable(typedParameter: TypedParameter): Boolean = isMockable(typedParameter.type)

    fun isMockable(type: DataType) = nonMockableTypes.none { mockableType ->
        type.name == mockableType.dataType
    }

    private fun DataType.Lambda.lambdaArguments() = inputParameterTypes.joinToString(", ") {
        it.name
    }

    private fun getCodeForCollectionOf(functionName: String, parameterType: DataType): String {
        val genericType = when (parameterType) {
            is DataType.Specific -> "Any"
            is DataType.Generic -> parameterType.toKotlinString()
            is DataType.Lambda -> parameterType.toKotlinString()
        }
        return "$functionName<$genericType>()"
    }
}
