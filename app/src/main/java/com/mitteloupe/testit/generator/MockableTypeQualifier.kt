package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.TypedParameter

/**
 * Created by Eran Boudjnah on 2019-08-03.
 */
class MockableTypeQualifier {
    private val nonMockableTypes = listOf(
        ConcreteValue("Boolean") { _, _ -> "false" },
        ConcreteValue("Byte") { _, _ -> "0b0" },
        ConcreteValue("Class") { _, _ -> "Any::class.java" },
        ConcreteValue("Double") { _, _ -> "0.0" },
        ConcreteValue("Float") { _, _ -> "0f" },
        ConcreteValue("Int") { _, _ -> "0" },
        ConcreteValue("Long") { _, _ -> "0L" },
        ConcreteValue("Short") { _, _ -> "0.toShort()" },
        ConcreteValue("String") { parameterName, _ -> "\"$parameterName\"" },
        ConcreteValue("List") { _, parameterType -> getCodeForListOf("listOf", parameterType) },
        ConcreteValue("MutableList") { _, parameterType -> getCodeForListOf("mutableListOf", parameterType) },
        ConcreteValue("Map") { _, parameterType -> getCodeForListOf("mapOf", parameterType) },
        ConcreteValue("MutableMap") { _, parameterType -> getCodeForListOf("mutableMapOf", parameterType) },
        ConcreteValue("Set") { _, parameterType -> getCodeForListOf("setOf", parameterType) },
        ConcreteValue("MutableSet") { _, parameterType -> getCodeForListOf("mutableSetOf", parameterType) },
        ConcreteValue("Unit") { _, _ -> "Unit" }
    )

    fun getNonMockableType(variableType: String) =
        nonMockableTypes.firstOrNull { type -> type.dataType == variableType }

    fun isMockable(type: TypedParameter) = nonMockableTypes.none { mockableType ->
        type.name == mockableType.dataType
    }

    private fun getCodeForListOf(functionName: String, parameterType: DataType): String {
        val genericType = when (parameterType) {
            is DataType.Specific -> "Any"
            is DataType.Generic -> formatGenericsType(parameterType.genericTypes[0].name)
        }
        return "$functionName<$genericType>()"
    }

    private fun formatGenericsType(genericsType: String) = genericsType.replace(",", ", ")
}