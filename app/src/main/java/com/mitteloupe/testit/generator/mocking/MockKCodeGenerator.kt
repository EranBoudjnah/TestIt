package com.mitteloupe.testit.generator.mocking

import com.mitteloupe.testit.generator.INDENT
import com.mitteloupe.testit.generator.INDENT_2
import com.mitteloupe.testit.generator.INDENT_3
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.concreteFunctions

class MockKCodeGenerator(mockableTypeQualifier: MockableTypeQualifier) :
    MockerCodeGenerator(mockableTypeQualifier) {
    private val requiredImports = mutableSetOf<String>()

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
            "${INDENT_2}MockKAnnotations.init(this, relaxUnitFun = true)\n"
        } else {
            null
        }

    override fun reset() {
        requiredImports.clear()
    }

    override fun getConstructorMock(parameterName: String, parameterType: DataType) =
        "$INDENT@MockK\n" +
                "${INDENT}lateinit var $parameterName: ${parameterType.name}"

    override fun getAbstractClassUnderTest(classUnderTest: ClassMetadata): String {
        val arguments =
            classUnderTest.constructorParameters.joinToString(", ") { parameter -> parameter.name }
        stringBuilder.clear()
            .append("object : ${classUnderTest.className}($arguments) {\n")

        classUnderTest.concreteFunctions
            .forEach { function ->
                val mockedValue = getMockedValue(function.name, function.returnType)
                stringBuilder.append("${INDENT_3}override fun test2() = $mockedValue\n")
            }

        stringBuilder.append("$INDENT_2}")

        return stringBuilder.toString()
    }

    override fun getMockedInstance(variableType: DataType) = "mockk<${variableType.name}>()"

    override fun getRequiredImports() = requiredImports + super.getRequiredImports()

    override fun setHasMockedConstructorParameters(classUnderTest: ClassMetadata) {
        _hasMockedConstructorParameters = true
        requiredImports.add("MockKAnnotations")
        requiredImports.add("MockK")
    }

    override fun setHasMockedFunctionParameters() {
        setInstantiatesMocks()
    }

    override fun setHasMockedFunctionReturnValues() {
        setInstantiatesMocks()
    }

    private fun setInstantiatesMocks() {
        requiredImports.add("mockk")
    }

    override fun setIsAbstractClassUnderTest() {
    }
}