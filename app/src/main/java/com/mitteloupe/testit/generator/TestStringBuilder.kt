package com.mitteloupe.testit.generator

import com.mitteloupe.testit.generator.mocking.MockerCodeGenerator
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.FunctionsMetadataContainer
import com.mitteloupe.testit.model.StaticFunctionsMetadata
import com.mitteloupe.testit.model.TypedParameter
import com.mitteloupe.testit.model.concreteFunctions

class TestStringBuilder(
    private val stringBuilder: StringBuilder,
    private val mockerCodeGenerator: MockerCodeGenerator,
    private val classUnderTestVariableName: String,
    private val actualValueVariableName: String,
    private val defaultAssertionStatement: String
) {
    fun appendTestClass(config: TestStringBuilderConfiguration): TestStringBuilder {
        val classUnderTest = config.classUnderTest
        return appendPackageName(classUnderTest.packageName)
            .appendImports(config.usedImports)
            .appendTestClassAnnotation(
                config.hasMockableConstructorParameters,
                config.isParameterized
            )
            .append("class ${classUnderTest.className}Test {\n")
            .appendClassVariable(classUnderTest.className)
            .appendMocks(classUnderTest.constructorParameters)
            .appendSetUp(classUnderTest)
            .appendTests(false, classUnderTest)
            .append("}\n")
    }

    fun appendFunctionsTestClass(
        functionsUnderTest: StaticFunctionsMetadata,
        usedImports: Set<String>,
        outputClassName: String,
        isParameterized: Boolean
    ) = appendPackageName(functionsUnderTest.packageName)
        .appendImports(usedImports)
        .appendTestClassAnnotation(false, isParameterized)
        .append("class ${outputClassName}Test {\n")
        .appendTests(true, functionsUnderTest)
        .append("}\n")

    fun clear() {
        stringBuilder.clear()
        mockerCodeGenerator.reset()
    }

    override fun toString() = stringBuilder.toString()

    private fun appendPackageName(
        packageName: String
    ) = append("package $packageName")
        .appendBlankLine()

    private fun appendImports(usedImports: Set<String>): TestStringBuilder {
        usedImports.sorted().forEach { qualifiedName ->
            append("import $qualifiedName\n")
        }

        if (usedImports.isNotEmpty()) {
            append("\n")
        }

        return this
    }

    private fun appendTestClassAnnotation(
        hasMockableConstructorParameters: Boolean,
        isParameterized: Boolean
    ): TestStringBuilder {
        when {
            hasMockableConstructorParameters -> {
                mockerCodeGenerator.testClassBaseRunnerAnnotation?.let { annotation ->
                    append("$annotation\n")
                }
            }
            isParameterized -> {
                append("${mockerCodeGenerator.testClassParameterizedRunnerAnnotation}\n")
            }
        }
        return this
    }

    private fun appendMocks(classParameters: List<TypedParameter>): TestStringBuilder {
        append(classParameters.joinToString("\n\n") { parameter ->
            mockerCodeGenerator.getMockedVariableDefinition(parameter)
        })

        if (classParameters.isNotEmpty()) {
            appendBlankLine()
        }

        return this
    }

    private fun appendSetUp(
        classUnderTest: ClassMetadata
    ): TestStringBuilder {
        append("$INDENT@Before\n")
            .append("${INDENT}fun setUp() {\n")

        mockerCodeGenerator.setUpStatements?.let {
            append(it)
        }

        append("$INDENT_2$classUnderTestVariableName = ")
        if (classUnderTest.isAbstract) {
            val abstractClassUnderTest =
                mockerCodeGenerator.getAbstractClassUnderTest(classUnderTest)
            append("$abstractClassUnderTest\n")

        } else {
            append("${classUnderTest.className}(")
                .append(classUnderTest.constructorParameters.joinToString(", ") { parameter -> parameter.name })
                .append(")\n")
        }

        append("$INDENT}")
            .appendBlankLine()

        return this
    }

    private fun appendTests(
        isStatic: Boolean,
        functionsMetadataContainer: FunctionsMetadataContainer
    ): TestStringBuilder {
        val concreteFunctions = functionsMetadataContainer.concreteFunctions
        val lastIndex = concreteFunctions.size - 1
        concreteFunctions.forEachIndexed { index, function ->
            appendTest(isStatic, function)
            if (index != lastIndex) {
                append("\n")
            }
        }

        return this
    }

    private fun appendTest(
        isStatic: Boolean,
        function: FunctionMetadata
    ) = append("$INDENT@Test\n")
        .append("${INDENT}fun `Given _ when ${function.nameForTestFunctionName} then _`() {\n")
        .appendTestBody(isStatic, function)
        .append("$INDENT}\n")

    private fun appendTestBody(
        isStatic: Boolean,
        function: FunctionMetadata
    ) = appendGiven(function)
        .appendWhen(isStatic, function)
        .appendThen()

    private fun appendGiven(
        function: FunctionMetadata
    ) = append("$INDENT_2// Given\n")
        .appendFunctionParameterMocks(function)

    private fun appendFunctionParameterMocks(function: FunctionMetadata): TestStringBuilder {
        function.parameters.forEach { parameter ->
            val value = mockerCodeGenerator.getMockedValue(parameter.name, parameter.type)
            append("${INDENT_2}val ${parameter.name} = $value\n")
        }
        function.extensionReceiverType?.let { receiverType ->
            if (function.parameters.isNotEmpty()) {
                append("\n")
            }
            appendExtensionReceiver(receiverType)
        }
        append("\n")

        return this
    }

    private fun appendExtensionReceiver(receiverType: DataType): TestStringBuilder {
        val receiverValue = mockerCodeGenerator.getMockedValue(receiverType.name, receiverType)
        return append("${INDENT_2}val receiver = $receiverValue\n")
    }

    private fun appendWhen(
        isStatic: Boolean,
        function: FunctionMetadata
    ): TestStringBuilder {
        val actualVariable = if (function.returnType.name != "Unit") {
            "val $actualValueVariableName = "
        } else {
            ""
        }
        val receiver = function.extensionReceiverType?.let {
            val classUnderTestWrapperOpen =
                if (isStatic) "" else "with($classUnderTestVariableName) {\n$INDENT_3"
            "${classUnderTestWrapperOpen}receiver."
        } ?: if (isStatic) "" else "$classUnderTestVariableName."
        val output = append("$INDENT_2// When\n")
            .append("$INDENT_2$actualVariable$receiver${function.name}(")
            .append(function.parameters.joinToString(", ") { it.name })
            .append(")")

        if (!isStatic) {
            function.extensionReceiverType?.let {
                output.append("\n$INDENT_2}")
            }
        }

        return output.appendBlankLine()
    }

    private fun appendThen() = append("$INDENT_2// Then\n")
        .append("$INDENT_2$defaultAssertionStatement\n")

    private fun appendBlankLine() = append("\n\n")

    private fun appendClassVariable(
        className: String
    ) = append("${INDENT}private lateinit var $classUnderTestVariableName: $className\n\n")

    private fun append(string: String): TestStringBuilder {
        stringBuilder.append(string)
        return this
    }

    private val FunctionMetadata.nameForTestFunctionName
        get() = extensionReceiverType?.let { "${extensionReceiverType.name}#$name" } ?: name
}

data class TestStringBuilderConfiguration(
    val classUnderTest: ClassMetadata,
    val usedImports: Set<String>,
    val hasMockableConstructorParameters: Boolean,
    val isParameterized: Boolean
)