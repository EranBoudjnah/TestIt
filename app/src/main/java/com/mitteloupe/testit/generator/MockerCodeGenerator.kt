package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.TypedParameter

/**
 * Created by Eran Boudjnah on 2019-07-07.
 */
abstract class MockerCodeGenerator {
    private val nonMockableTypes = listOf(
        ConcreteValue("Boolean") { "false" },
        ConcreteValue("Byte") { "0b0" },
        ConcreteValue("Class") { "Any::class.java" },
        ConcreteValue("Double") { "0.0" },
        ConcreteValue("Float") { "0f" },
        ConcreteValue("Int") { "0" },
        ConcreteValue("Long") { "0L" },
        ConcreteValue("Short") { "0.toShort()" },
        ConcreteValue("String") { parameterName -> "\"$parameterName\"" },
        ConcreteValue("Unit") { "Unit" }
    )

    fun getMockedValue(variableType: String, variableName: String) =
        nonMockableTypes.firstOrNull { type -> type.dataType == variableType }?.let { type ->
            type.defaultValue(variableName)
        } ?: getMockedInstance(variableType)

    fun getMockedVariableDefinition(parameter: TypedParameter): String {
        val parameterName = parameter.name
        val parameterType = parameter.type
        return nonMockableTypes.firstOrNull { type -> type.dataType == parameterType }?.let { type ->
            "private val $parameterName = ${type.defaultValue(parameterName)}"
        } ?: getConstructorMock(parameterName, parameterType)
    }

    abstract val testClassAnnotation: String?

    abstract val usedImports: Map<String, String>

    abstract val knownImports: Map<String, String>

    abstract val setUpStatements: String?

    abstract fun getConstructorMock(parameterName: String, parameterType: String): String

    abstract fun getMockedInstance(variableType: String): String

    abstract fun getAbstractClassUnderTest(classUnderTest: ClassMetadata): String

    abstract fun getRequiredImports(): Set<String>

    abstract fun setHasMockedConstructorParameters()

    abstract fun setHasMockedFunctionParameters()

    abstract fun setIsAbstractClassUnderTest()

    fun TypedParameter.isMockable() =
        nonMockableTypes.none { mockableType ->
            type == mockableType.dataType
        }
}


data class ConcreteValue(
    val dataType: String,
    val defaultValue: (String) -> String
)