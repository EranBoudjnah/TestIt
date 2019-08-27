package com.mitteloupe.testit.generator.mocking

import com.mitteloupe.testit.generator.INDENT
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.TypedParameter

/**
 * Created by Eran Boudjnah on 2019-07-07.
 */
abstract class MockerCodeGenerator(private val mockableTypeQualifier: MockableTypeQualifier) {
    private val _requiredImports = mutableSetOf<String>()

    abstract val testClassBaseRunnerAnnotation: String?

    val testClassParameterizedRunnerAnnotation = "@RunWith(Parameterized::class)"

    open val knownImports: Map<String, String>
    get() = mapOf(
        "RunWith" to "org.junit.runner.RunWith",
        "Parameterized" to "org.junit.runners.Parameterized"
    )

    abstract val setUpStatements: String?

    open fun reset() {
    }

    fun getMockedValue(variableName: String, variableType: DataType) =
        mockableTypeQualifier.getNonMockableType(variableType.name)?.let { type ->
            type.defaultValue(variableName, variableType)
        } ?: getMockedInstance(variableType)

    fun getMockedVariableDefinition(parameter: TypedParameter): String {
        val parameterName = parameter.name
        val parameterType = parameter.type
        return mockableTypeQualifier.getNonMockableType(parameterType.name)?.let { type ->
            "${INDENT}private val $parameterName = ${type.defaultValue(parameterName, parameterType)}"
        } ?: getConstructorMock(parameterName, parameterType)
    }

    fun setIsParameterizedTest() {
        _requiredImports.add("RunWith")
        _requiredImports.add("Parameterized")
    }

    abstract fun getConstructorMock(parameterName: String, parameterType: DataType): String

    protected abstract fun getMockedInstance(variableType: DataType): String

    abstract fun getAbstractClassUnderTest(classUnderTest: ClassMetadata): String

    open fun getRequiredImports(): Set<String> = _requiredImports

    abstract fun setHasMockedConstructorParameters(classUnderTest: ClassMetadata)

    abstract fun setHasMockedFunctionParameters()

    abstract fun setIsAbstractClassUnderTest()

    fun TypedParameter.isMockable() = mockableTypeQualifier.isMockable(this)

    fun DataType.isMockable() = mockableTypeQualifier.isMockable(this)
}

data class ConcreteValue(
    val dataType: String,
    val defaultValue: (String, DataType) -> String
)
