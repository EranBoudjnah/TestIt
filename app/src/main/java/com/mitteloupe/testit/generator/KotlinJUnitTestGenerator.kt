package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.TypedParameter

private const val INDENT = "    "
private const val INDENT_2 = "$INDENT$INDENT"
private const val CLASS_UNDER_TEST = "cut"

class KotlinJUnitTestGenerator(
    private val stringBuilder: StringBuilder
) : TestsGenerator {
    private val usedImports = mutableMapOf<String, String>()

    private val knownImports = mutableMapOf<String, String>()

    init {
        reset()
    }

    override fun ClassMetadata.addToTests() {
        stringBuilder.appendTestClass(this)
    }

    override fun reset() {
        stringBuilder.clear()
        usedImports.clear()
        usedImports.putAll(
            mutableMapOf(
                "RunWith" to "org.junit.runner.RunWith",
                "MockitoJUnitRunner" to "org.mockito.junit.MockitoJUnitRunner",
                "Before" to "org.junit.Before"
            )
        )
        knownImports.clear()
        knownImports.putAll(
            mutableMapOf(
                "Mock" to "org.mockito.Mock",
                "mock" to "com.nhaarman.mockitokotlin2.mock",
                "Test" to "org.junit.Test"
            )
        )
    }

    override fun generateTests() = stringBuilder.toString()

    private fun StringBuilder.appendTestClass(classUnderTest: ClassMetadata): StringBuilder {
        evaluateImports(classUnderTest)

        return appendPackageName(classUnderTest.packageName)
            .appendImports()
            .appendRunWithAnnotation()
            .append("class ${classUnderTest.className}Test {\n")
            .appendClassVariable(classUnderTest.className)
            .appendMocks(classUnderTest.constructorParameters)
            .appendSetUp(classUnderTest.className, classUnderTest.constructorParameters)
            .appendTests(classUnderTest)
            .append("}")
            .appendBlankLine()
    }

    private fun StringBuilder.appendPackageName(
        packageName: String
    ) = append("package $packageName")
        .appendBlankLine()

    private fun StringBuilder.appendImports(): StringBuilder {
        usedImports.values.sorted().forEach { qualifiedName ->
            append("import $qualifiedName\n")
        }

        append("\n")

        return this
    }

    private fun StringBuilder.appendRunWithAnnotation() = append("@RunWith(MockitoJUnitRunner::class)\n")

    private fun StringBuilder.appendClassVariable(
        className: String
    ) = append("${INDENT}private lateinit var $CLASS_UNDER_TEST: $className\n\n")

    private fun StringBuilder.appendMocks(classParameters: List<TypedParameter>): StringBuilder {
        append(classParameters.joinToString("\n\n") { parameter ->
            when (parameter.type) {
                "String" -> "private val ${parameter.name} = \"${parameter.name}\""
                else -> "$INDENT@Mock\n" +
                        "${INDENT}lateinit var ${parameter.name}: ${parameter.type}"
            }
        })

        if (classParameters.isNotEmpty()) {
            appendBlankLine()
        }

        return this
    }

    private fun StringBuilder.appendSetUp(className: String, classParameters: List<TypedParameter>) =
        append("$INDENT@Before\n")
            .append("${INDENT}fun setUp() {\n")
            .append("$INDENT_2$CLASS_UNDER_TEST = $className(")
            .append(classParameters.joinToString(", ") { parameter -> parameter.name })
            .append(")\n")
            .append("$INDENT}")
            .appendBlankLine()

    private fun StringBuilder.appendTests(classUnderTest: ClassMetadata): StringBuilder {
        val lastIndex = classUnderTest.functions.size - 1
        classUnderTest.functions.forEachIndexed { index, function ->
            appendTest(function)
            if (index != lastIndex) {
                append("\n")
            }
        }

        return this
    }

    private fun StringBuilder.appendTest(
        function: FunctionMetadata
    ) = append("$INDENT@Test\n")
        .append("${INDENT}fun `Given _ when ${function.name} then _`() {\n")
        .appendTestBody(function)
        .append("$INDENT}\n")

    private fun StringBuilder.appendTestBody(
        function: FunctionMetadata
    ) = appendGiven(function)
        .appendWhen(function)
        .appendThen()

    private fun StringBuilder.appendGiven(
        function: FunctionMetadata
    ) = append("$INDENT_2// Given\n")
        .appendFunctionParameterMocks(function)

    private fun StringBuilder.appendFunctionParameterMocks(function: FunctionMetadata): StringBuilder {
        function.parameters.forEach { parameter ->
            val value = getMockedValue(parameter.type)
            append("${INDENT_2}val ${parameter.name} = $value\n")
        }
        append("\n")

        return this
    }

    private fun getMockedValue(variableType: String) = when (variableType) {
        "String" -> "\"variableType\""
        else -> "mock<$variableType>()"
    }

    private fun StringBuilder.appendWhen(
        function: FunctionMetadata
    ): StringBuilder {
        val actualVariable = if (function.returnType != "Unit") {
            "val actual = "
        } else {
            ""
        }
        return append("$INDENT_2// When\n")
            .append("$INDENT_2${actualVariable}cut.${function.name}(")
            .append(function.parameters.joinToString(", ") { it.name })
            .append(")")
            .appendBlankLine()
    }

    private fun StringBuilder.appendThen() = append("$INDENT_2// Then\n")

    private fun StringBuilder.appendBlankLine() = append("\n\n")

    private fun evaluateImports(classUnderTest: ClassMetadata) {
        if (classUnderTest.constructorParameters.isNotEmpty()) {
            addImportIfKnown("Mock")
        }
        if (classUnderTest.functions.isNotEmpty()) {
            addImportIfKnown("Test")
        }
        if (classUnderTest.functions.any { it.parameters.isNotEmpty() }) {
            addImportIfKnown("mock")
        }

        classUnderTest.imports.forEach { (a, b) -> usedImports[a] = b }
    }

    private fun addImportIfKnown(entityName: String) {
        knownImports[entityName]?.let { qualifiedName ->
            usedImports[entityName] = qualifiedName
            knownImports.remove(entityName)
        }
    }
}