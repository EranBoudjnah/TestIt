package com.mitteloupe.testit.generator

class MockitoCodeGenerator : MockerCodeGenerator {
    private val requiredImports = mutableSetOf<String>()

    override val testClassAnnotation: String = "@RunWith(MockitoJUnitRunner::class)"

    override val usedImports = emptyMap<String, String>()

    override val knownImports = mapOf(
        "RunWith" to "org.junit.runner.RunWith",
        "MockitoJUnitRunner" to "org.mockito.junit.MockitoJUnitRunner",
        "Mock" to "org.mockito.Mock",
        "mock" to "com.nhaarman.mockitokotlin2.mock"
    )

    override val setUpStatements: String? = null

    override fun getConstructorMock(parameterName: String, parameterType: String) =
        "$INDENT@Mock\n" +
                "${INDENT}lateinit var $parameterName: $parameterType"

    override fun getMockedInstance(variableType: String) = "mock<$variableType>()"

    override fun setHasMockedConstructorParameters() {
        requiredImports.add("RunWith")
        requiredImports.add("MockitoJUnitRunner")
        requiredImports.add("Mock")
    }

    override fun setHasMockedFunctionParameters() {
        requiredImports.add("mock")
    }

    override fun getRequiredImports() = requiredImports
}