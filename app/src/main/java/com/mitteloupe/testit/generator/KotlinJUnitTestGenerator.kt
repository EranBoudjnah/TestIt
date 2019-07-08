package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.TypedParameter

class KotlinJUnitTestGenerator(
    private val stringBuilder: StringBuilder,
    private val mockerCodeGenerator: MockerCodeGenerator,
    private val classUnderTestVariableName: String,
    private val actualValueVariableName: String,
    private val defaultAssertionStatement: String
) : TestsGenerator {
    private val usedImports = mutableMapOf<String, String>()

    private val knownImports = mutableMapOf<String, String>()

    private val nonMockableTypes = listOf(
        ConcreteValue("Boolean") { "false" },
        ConcreteValue("Byte") { "0b0" },
        ConcreteValue("Class") { "Any::class.java" },
        ConcreteValue("Double") { "0.0" },
        ConcreteValue("Float") { "0f" },
        ConcreteValue("Int") { "0" },
        ConcreteValue("Long") { "0L" },
        ConcreteValue("Short") { "0.toShort()" },
        ConcreteValue("String") { parameterName -> "\"$parameterName\"" }
    )

    init {
        reset()
    }

    override fun ClassMetadata.addToTests() {
        setUpMockGenerator(this)
        stringBuilder.appendTestClass(this)
    }

    private fun setUpMockGenerator(classUnderTest: ClassMetadata) {
        if (hasMockableConstructorParameters(classUnderTest.constructorParameters)) {
            mockerCodeGenerator.setHasMockedConstructorParameters()
        }

        if (hasMockableFunctionParameters(classUnderTest.functions)) {
            mockerCodeGenerator.setHasMockedFunctionParameters()
        }
    }

    override fun reset() {
        stringBuilder.clear()
        with(usedImports) {
            clear()
            putAll(
                mutableMapOf(
                    "Before" to "org.junit.Before"
                )
            )
            putAll(mockerCodeGenerator.usedImports)
        }

        with(knownImports) {
            clear()
            putAll(
                mutableMapOf(
                    "Test" to "org.junit.Test"
                )
            )
            putAll(mockerCodeGenerator.knownImports)
        }
    }

    override fun generateTests() = stringBuilder.toString()

    private fun StringBuilder.appendTestClass(classUnderTest: ClassMetadata): StringBuilder {
        evaluateImports(classUnderTest)

        return appendPackageName(classUnderTest.packageName)
            .appendImports()
            .appendTestClassAnnotation(classUnderTest.constructorParameters)
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

    private fun StringBuilder.appendTestClassAnnotation(constructorParameters: List<TypedParameter>): StringBuilder {
        if (hasMockableConstructorParameters(constructorParameters)) {
            mockerCodeGenerator.testClassAnnotation?.let { annotation ->
                append("$annotation\n")
            }
        }
        return this
    }

    private fun StringBuilder.appendMocks(classParameters: List<TypedParameter>): StringBuilder {
        append(classParameters.joinToString("\n\n") { parameter ->
            val parameterName = parameter.name
            val parameterType = parameter.type
            nonMockableTypes.firstOrNull { type -> type.dataType == parameterType }?.let { type ->
                "private val $parameterName = ${type.defaultValue(parameterName)}"
            } ?: mockerCodeGenerator.getConstructorMock(parameterName, parameterType)
        })

        if (classParameters.isNotEmpty()) {
            appendBlankLine()
        }

        return this
    }

    private fun StringBuilder.appendSetUp(className: String, classParameters: List<TypedParameter>): StringBuilder {
        append("$INDENT@Before\n")
            .append("${INDENT}fun setUp() {\n")

        mockerCodeGenerator.setUpStatements?.let {
            append(it)
        }

        append("$INDENT_2$classUnderTestVariableName = $className(")
            .append(classParameters.joinToString(", ") { parameter -> parameter.name })
            .append(")\n")
            .append("$INDENT}")
            .appendBlankLine()

        return this
    }

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
            val value = getMockedValue(parameter.type, parameter.name)
            append("${INDENT_2}val ${parameter.name} = $value\n")
        }
        append("\n")

        return this
    }

    private fun getMockedValue(variableType: String, variableName: String) =
        nonMockableTypes.firstOrNull { type -> type.dataType == variableType }?.let { type ->
            type.defaultValue(variableName)
        } ?: mockerCodeGenerator.getMockedInstance(variableType)

    private fun StringBuilder.appendWhen(
        function: FunctionMetadata
    ): StringBuilder {
        val actualVariable = if (function.returnType != "Unit") {
            "val $actualValueVariableName = "
        } else {
            ""
        }
        return append("$INDENT_2// When\n")
            .append("$INDENT_2$actualVariable$classUnderTestVariableName.${function.name}(")
            .append(function.parameters.joinToString(", ") { it.name })
            .append(")")
            .appendBlankLine()
    }

    private fun StringBuilder.appendThen() = append("$INDENT_2// Then\n")
        .append("$INDENT_2$defaultAssertionStatement\n")

    private fun StringBuilder.appendBlankLine() = append("\n\n")

    private fun StringBuilder.appendClassVariable(
        className: String
    ) = append("${INDENT}private lateinit var $classUnderTestVariableName: $className\n\n")

    private fun evaluateImports(classUnderTest: ClassMetadata) {
        val imports = mockerCodeGenerator.getRequiredImports()
        imports.forEach(::addImportIfKnown)

        if (classUnderTest.functions.isNotEmpty()) {
            addImportIfKnown("Test")
        }

        classUnderTest.imports.forEach { (a, b) -> usedImports[a] = b }
    }

    private fun hasMockableConstructorParameters(constructorParameters: List<TypedParameter>) =
        constructorParameters.any { parameter -> parameter.isMockable() }

    private fun hasMockableFunctionParameters(functions: List<FunctionMetadata>) =
        functions.any { function ->
            function.parameters.any { parameter -> parameter.isMockable() }
        }

    private fun TypedParameter.isMockable(): Boolean {
        return nonMockableTypes.none { mockableType ->
            type == mockableType.dataType
        }
    }

    private fun addImportIfKnown(entityName: String) {
        knownImports[entityName]?.let { qualifiedName ->
            usedImports[entityName] = qualifiedName
            knownImports.remove(entityName)
        }
    }
}

private data class ConcreteValue(
    val dataType: String,
    val defaultValue: (String) -> String
)