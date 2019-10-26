package com.mitteloupe.testit.generator.mocking

import com.mitteloupe.testit.generator.formatting.Formatting
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType

class MockitoCodeGenerator(
    mockableTypeQualifier: MockableTypeQualifier,
    formatting: Formatting
) : MockerCodeGenerator(mockableTypeQualifier, formatting) {
    private val requiredImports = mutableSetOf<String>()

    private var hasMockedConstructorParameters = false
    private var isParameterizedTest = false

    override val testClassBaseRunnerAnnotation: String = "@RunWith(MockitoJUnitRunner::class)"

    override val mockingRule =
        "${indent()}@get:Rule\n" +
                "${indent()}val rule: MethodRule = MockitoJUnit.rule()"

    override val knownImports = mapOf(
        "MockitoJUnitRunner" to "org.mockito.junit.MockitoJUnitRunner",
        "MockitoJUnit" to "org.mockito.junit.MockitoJUnit",
        "Mock" to "org.mockito.Mock",
        "mock" to "com.nhaarman.mockitokotlin2.mock",
        "Mockito" to "org.mockito.Mockito",
        "UseConstructor" to "com.nhaarman.mockitokotlin2.UseConstructor"
    ) + super.knownImports

    override val setUpStatements: String? = null

    override fun reset() {
        requiredImports.clear()
    }

    override fun getConstructorMock(parameterName: String, parameterType: DataType) =
        "${indent()}@Mock\n" +
                "${indent()}lateinit var $parameterName: ${parameterType.name}"

    override fun getMockedInstance(variableType: DataType) = "mock<${variableType.name}>()"

    override fun getAbstractClassUnderTest(classUnderTest: ClassMetadata) =
        "mock(defaultAnswer = Mockito.CALLS_REAL_METHODS${getConstructorArgumentsForAbstract(
            classUnderTest
        )})"

    override fun getRequiredImports() = requiredImports + super.getRequiredImports()

    override fun setIsParameterizedTest() {
        super.setIsParameterizedTest()

        isParameterizedTest = true
        addMockRuleIfNeeded()
    }

    override fun setHasMockedConstructorParameters(classUnderTest: ClassMetadata) {
        requiredImports.add("RunWith")
        requiredImports.add("MockitoJUnitRunner")
        requiredImports.add("Mock")
        if (classUnderTest.isAbstract && classUnderTest.constructorParameters.isNotEmpty()) {
            requiredImports.add("UseConstructor")
        }
        hasMockedConstructorParameters = true
        addMockRuleIfNeeded()
    }

    private fun addMockRuleIfNeeded() {
        if (isParameterizedTest && hasMockedConstructorParameters) {
            requiredImports.add("Rule")
            requiredImports.add("MethodRule")
            requiredImports.add("MockitoJUnit")
        }
    }

    override fun setHasMockedFunctionParameters() {
        setInstantiatesMocks()
    }

    override fun setHasMockedFunctionReturnValues() {
        setInstantiatesMocks()
    }

    override fun setIsAbstractClassUnderTest() {
        setInstantiatesMocks()
        requiredImports.add("Mockito")
    }

    private fun setInstantiatesMocks() {
        requiredImports.add("mock")
    }

    private fun getConstructorArgumentsForAbstract(classUnderTest: ClassMetadata) =
        if (classUnderTest.constructorParameters.isEmpty()) {
            ""
        } else {
            val arguments =
                classUnderTest.constructorParameters.joinToString(", ") { parameter -> parameter.name }
            ", useConstructor = UseConstructor.withArguments($arguments)"
        }
}