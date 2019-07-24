package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.TypedParameter

/**
 * Created by Eran Boudjnah on 2019-07-07.
 */
abstract class MockerCodeGenerator {
    abstract val testClassAnnotation: String?

    abstract val knownImports: Map<String, String>

    abstract val setUpStatements: String?

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

    open fun reset() {
    }

    fun getMockedValue(variableName: String, variableType: DataType) =
        nonMockableTypes.firstOrNull { type -> type.dataType == variableType.name }?.let { type ->
            type.defaultValue(variableName, variableType)
        } ?: getMockedInstance(variableType)

    fun getMockedVariableDefinition(parameter: TypedParameter): String {
        val parameterName = parameter.name
        val parameterType = parameter.type
        return nonMockableTypes.firstOrNull { type -> type.dataType == parameterType.name }?.let { type ->
            "private val $parameterName = ${type.defaultValue(parameterName, parameterType)}"
        } ?: getConstructorMock(parameterName, parameterType)
    }

    abstract fun getConstructorMock(parameterName: String, parameterType: DataType): String

    abstract fun getMockedInstance(variableType: DataType): String

    abstract fun getAbstractClassUnderTest(classUnderTest: ClassMetadata): String

    abstract fun getRequiredImports(): Set<String>

    abstract fun setHasMockedConstructorParameters()

    abstract fun setHasMockedFunctionParameters()

    abstract fun setIsAbstractClassUnderTest()

    private fun getCodeForListOf(functionName: String, parameterType: DataType): String {
        val genericType = when (parameterType) {
            is DataType.Specific -> "Any"
            is DataType.Generic -> formatGenericsType(parameterType.genericTypes[0].name)
        }
        return "$functionName<$genericType>()"
    }

    private fun formatGenericsType(genericsType: String) = genericsType.replace(",", ", ")

    fun TypedParameter.isMockable() =
        nonMockableTypes.none { mockableType ->
            type.name == mockableType.dataType
        }
}


data class ConcreteValue(
    val dataType: String,
    val defaultValue: (String, DataType) -> String
)