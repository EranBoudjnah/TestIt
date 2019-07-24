package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType

class MockitoCodeGenerator : MockerCodeGenerator() {
    private val requiredImports = mutableSetOf<String>()

    override val testClassAnnotation: String = "@RunWith(MockitoJUnitRunner::class)"

    override val knownImports = mapOf(
        "RunWith" to "org.junit.runner.RunWith",
        "MockitoJUnitRunner" to "org.mockito.junit.MockitoJUnitRunner",
        "Mock" to "org.mockito.Mock",
        "mock" to "com.nhaarman.mockitokotlin2.mock",
        "Mockito" to "org.mockito.Mockito"
    )

    override val setUpStatements: String? = null

    override fun reset() {
        requiredImports.clear()
    }

    override fun getConstructorMock(parameterName: String, parameterType: DataType) =
        "$INDENT@Mock\n" +
                "${INDENT}lateinit var $parameterName: ${parameterType.name}"

    override fun getMockedInstance(variableType: DataType) = "mock<${variableType.name}>()"

    override fun getAbstractClassUnderTest(classUnderTest: ClassMetadata) =
        "mock(defaultAnswer = Mockito.CALLS_REAL_METHODS)"

    override fun getRequiredImports() = requiredImports

    override fun setHasMockedConstructorParameters() {
        requiredImports.add("RunWith")
        requiredImports.add("MockitoJUnitRunner")
        requiredImports.add("Mock")
    }

    override fun setHasMockedFunctionParameters() {
        requiredImports.add("mock")
    }

    override fun setIsAbstractClassUnderTest() {
        requiredImports.add("mock")
        requiredImports.add("Mockito")
    }
}