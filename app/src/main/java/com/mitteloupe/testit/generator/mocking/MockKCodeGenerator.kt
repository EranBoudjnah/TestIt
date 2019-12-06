package com.mitteloupe.testit.generator.mocking

import com.mitteloupe.testit.generator.formatting.Formatting
import com.mitteloupe.testit.generator.formatting.toKotlinString
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.concreteFunctions

class MockKCodeGenerator(
    mockableTypeQualifier: MockableTypeQualifier,
    formatting: Formatting
) : MockerCodeGenerator(mockableTypeQualifier, formatting) {
    private var _hasMockedConstructorParameters = false

    private val stringBuilder by lazy { StringBuilder() }

    override val testClassBaseRunnerAnnotation: String? = null
    override val mockingRule: String? = null

    override val knownImports = mapOf(
        "MockKAnnotations" to "io.mockk.MockKAnnotations",
        "MockK" to "io.mockk.impl.annotations.MockK",
        "mockk" to "io.mockk.mockk"
    ) + super.knownImports

    override val setUpStatements: String?
        get() = if (_hasMockedConstructorParameters) {
            "${indent(2)}MockKAnnotations.init(this, relaxUnitFun = true)\n"
        } else {
            null
        }

    override fun reset() {
        _requiredImports.clear()
    }

    override fun getConstructorMock(parameterName: String, parameterType: DataType) =
        "${indent()}@MockK\n" +
                "${indent()}lateinit var $parameterName: ${parameterType.toKotlinString()}"

    override fun getAbstractClassUnderTest(classUnderTest: ClassMetadata): String {
        val arguments =
            classUnderTest.constructorParameters.joinToString(", ") { parameter -> parameter.name }
        stringBuilder.clear()
            .append("object : ${classUnderTest.className}($arguments) {\n")

        classUnderTest.concreteFunctions
            .forEach { function ->
                val mockedValue = getMockedValue(function.name, function.returnType)
                stringBuilder.append("${indent(3)}override fun test2() = $mockedValue\n")
            }

        stringBuilder.append("${indent(2)}}")

        return stringBuilder.toString()
    }

    override fun getMockedInstance(variableType: DataType) = "mockk<${variableType.toKotlinString()}>()"

    override fun setHasMockedConstructorParameters(classUnderTest: ClassMetadata) {
        _hasMockedConstructorParameters = true
        _requiredImports.add("MockKAnnotations")
        _requiredImports.add("MockK")
    }

    override fun setHasMockedFunctionParameters() {
        setInstantiatesMocks()
    }

    override fun setHasMockedFunctionReturnValues() {
        setInstantiatesMocks()
    }

    private fun setInstantiatesMocks() {
        _requiredImports.add("mockk")
    }

    override fun setIsAbstractClassUnderTest() {
    }
}