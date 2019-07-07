package com.mitteloupe.testit.generator

class MockkCodeGenerator : MockerCodeGenerator {
    private val requiredImports = mutableSetOf<String>()

    private var _hasMockedConstructorParameters = false

    override val testClassAnnotation: String? = null

    override val usedImports = emptyMap<String, String>()

    override val knownImports = mapOf(
        "MockKAnnotations" to "io.mockk.MockKAnnotations",
        "MockK" to "io.mockk.impl.annotations.MockK",
        "mockk" to "io.mockk.mockk"
    )

    override val setUpStatements: String?
        get() = if (_hasMockedConstructorParameters) {
            "${INDENT_2}MockKAnnotations.init(this, relaxUnitFun = true)\n"
        } else {
            null
        }

    override fun getConstructorMock(parameterName: String, parameterType: String) =
        "$INDENT@MockK\n" +
                "${INDENT}lateinit var $parameterName: $parameterType"

    override fun getMockedInstance(variableType: String) = "mockk<$variableType>()"

    override fun setHasMockedConstructorParameters() {
        _hasMockedConstructorParameters = true
        requiredImports.add("MockKAnnotations")
        requiredImports.add("MockK")
    }

    override fun setHasMockedFunctionParameters() {
        requiredImports.add("mockk")
    }

    override fun getRequiredImports() = requiredImports
}