package com.mitteloupe.testit.generator

import com.mitteloupe.testit.generator.formatter.toKotlinString
import com.mitteloupe.testit.generator.mapper.DateTypeToParameterMapper
import com.mitteloupe.testit.generator.mocking.MockerCodeGenerator
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.FunctionsMetadataContainer
import com.mitteloupe.testit.model.StaticFunctionsMetadata
import com.mitteloupe.testit.model.TypedParameter
import com.mitteloupe.testit.model.concreteFunctions
import com.mitteloupe.testit.processing.hasReturnValue
import org.jetbrains.kotlin.backend.common.onlyIf

class TestStringBuilder(
    private val stringBuilder: StringBuilder,
    private val mockerCodeGenerator: MockerCodeGenerator,
    private val classUnderTestVariableName: String,
    private val actualValueVariableName: String,
    private val defaultAssertionStatement: String,
    private val dateTypeToParameterMapper: DateTypeToParameterMapper
) {
    fun appendTestClass(config: TestStringBuilderConfiguration): TestStringBuilder {
        val classUnderTest = config.classUnderTest
        val isParameterized = config.isParameterized
        return appendPackageName(classUnderTest.packageName)
            .appendImports(config.usedImports)
            .appendTestClassRunnerAnnotation(
                config.hasMockableConstructorParameters,
                isParameterized
            )
            .append("class ${classUnderTest.className}Test")
            .appendConstructorParameters(
                classUnderTest.constructorParameters,
                classUnderTest.functions,
                isParameterized
            )
            .append(" {\n")
            .onlyIf(
                { isParameterized },
                {
                    appendParameterizedCompanionObject(classUnderTest)
                        .appendBlankLine()
                }
            )
            .appendMockingRule(config.hasMockableConstructorParameters, isParameterized)
            .appendClassVariable(classUnderTest.className)
            .appendMocks(classUnderTest.constructorParameters)
            .appendSetUp(classUnderTest)
            .appendTests(false, classUnderTest, isParameterized)
            .append("}\n")
    }

    fun appendFunctionsTestClass(
        functionsUnderTest: StaticFunctionsMetadata,
        usedImports: Set<String>,
        outputClassName: String,
        isParameterized: Boolean
    ) = appendPackageName(functionsUnderTest.packageName)
        .appendImports(usedImports)
        .appendTestClassRunnerAnnotation(false, isParameterized)
        .append("class ${outputClassName}Test")
        .appendConstructorParameters(
            emptyList(),
            functionsUnderTest.functions,
            isParameterized
        )
        .append(" {\n")
        .appendTests(true, functionsUnderTest, isParameterized)
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

    private fun appendTestClassRunnerAnnotation(
        hasMockableConstructorParameters: Boolean,
        isParameterized: Boolean
    ): TestStringBuilder {
        when {
            isParameterized -> {
                append("${mockerCodeGenerator.testClassParameterizedRunnerAnnotation}\n")
            }
            hasMockableConstructorParameters -> {
                mockerCodeGenerator.testClassBaseRunnerAnnotation?.let { annotation ->
                    append("$annotation\n")
                }
            }
        }
        return this
    }

    private fun appendConstructorParameters(
        classUnderTestConstructorParameters: List<TypedParameter>,
        functions: List<FunctionMetadata>,
        isParameterized: Boolean
    ) = if (isParameterized) {
        val parameters =
            classUnderTestConstructorParameters +
                    functions.flatMap { function ->
                        function.parameters.map { parameter ->
                            val parameterName =
                                parameter.toKotlinString(function, true)
                            TypedParameter(parameterName, parameter.type)
                        } + TypedParameter(getExpectedVariableName(function), function.returnType)
                    }
        onlyIf(
            { parameters.isNotEmpty() },
            {
                append("(\n")
                    .append(parameters.joinToString(",\n") { parameter ->
                        "${INDENT}private val ${parameter.name}: ${parameter.type.toKotlinString()}"
                    })
                    .append("\n)")
            }
        )
    } else {
        this
    }

    private fun getExpectedVariableName(function: FunctionMetadata) =
        "${function.name}Expected"

    private fun appendMockingRule(
        hasMockableConstructorParameters: Boolean,
        isParameterized: Boolean
    ): TestStringBuilder {
        if (isParameterized && hasMockableConstructorParameters) {
            mockerCodeGenerator.mockingRule?.let {
                append(it)
                    .appendBlankLine()
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
        functionsMetadataContainer: FunctionsMetadataContainer,
        isParameterized: Boolean
    ): TestStringBuilder {
        val concreteFunctions = functionsMetadataContainer.concreteFunctions
        val lastIndex = concreteFunctions.size - 1
        concreteFunctions.forEachIndexed { index, function ->
            appendTest(isStatic, function, isParameterized)
            if (index != lastIndex) {
                append("\n")
            }
        }

        return this
    }

    private fun appendTest(
        isStatic: Boolean,
        function: FunctionMetadata,
        isParameterized: Boolean
    ) = append("$INDENT@Test\n")
        .append("${INDENT}fun `Given _ when ${function.nameForTestFunctionName} then _`() {\n")
        .appendTestBody(isStatic, function, isParameterized)
        .append("$INDENT}\n")

    private fun appendTestBody(
        isStatic: Boolean,
        function: FunctionMetadata,
        isParameterized: Boolean
    ) = appendGiven(function, isParameterized)
        .appendWhen(isStatic, function, isParameterized)
        .appendThen(function, isParameterized)

    private fun appendGiven(
        function: FunctionMetadata,
        isParameterized: Boolean
    ) = append("$INDENT_2// Given\n")
        .appendFunctionParameterMocks(function, isParameterized)

    private fun appendFunctionParameterMocks(
        function: FunctionMetadata,
        isParameterized: Boolean
    ) = onlyIf(
        { !isParameterized },
        {
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
        }
    )

    private fun appendExtensionReceiver(receiverType: DataType): TestStringBuilder {
        val receiverValue = mockerCodeGenerator.getMockedValue(receiverType.name, receiverType)
        return append("${INDENT_2}val receiver = $receiverValue\n")
    }

    private fun appendWhen(
        isStatic: Boolean,
        function: FunctionMetadata,
        isParameterized: Boolean
    ): TestStringBuilder {
        val actualVariable = if (function.hasReturnValue()) {
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
            .append(function.parameters.joinToString(", ") { parameter ->
                parameter.toKotlinString(function, isParameterized)
            })
            .append(")")

        if (!isStatic) {
            function.extensionReceiverType?.let {
                output.append("\n$INDENT_2}")
            }
        }

        return output.appendBlankLine()
    }

    private fun appendThen(
        function: FunctionMetadata,
        isParameterized: Boolean
    ) = append("$INDENT_2// Then\n")
        .appendReturnValueAssertion(function, isParameterized)
        .append("$INDENT_2$defaultAssertionStatement\n")

    private fun appendReturnValueAssertion(
        function: FunctionMetadata,
        isParameterized: Boolean
    ) = onlyIf(
        {
            function.hasReturnValue() && isParameterized
        },
        {
            append("${INDENT_2}assertEquals(${getExpectedVariableName(function)}, $actualValueVariableName)\n")
        }
    )

    private fun appendBlankLine() = append("\n\n")

    private fun appendClassVariable(
        className: String
    ) = append("${INDENT}private lateinit var $classUnderTestVariableName: $className")
        .appendBlankLine()

    private fun appendParameterizedCompanionObject(classUnderTest: ClassMetadata): TestStringBuilder {
        val returnTypes = classUnderTest.functions.map { it.returnType }
        return appendParameterizedCompanionObject(
            classUnderTest.constructorParameters +
                    classUnderTest.functions.flatMap { it.parameters }, returnTypes
        )
    }

    private fun appendParameterizedCompanionObject(
        parameters: List<TypedParameter>,
        expectedTypes: List<DataType>
    ) = append(
        "${INDENT}companion object {\n" +
                "${INDENT_2}@JvmStatic\n" +
                "${INDENT_2}@Parameters\n" +
                "${INDENT_2}fun data(): Collection<Array<*>> = listOf(\n" +
                "${INDENT_3}arrayOf("
    )
        .appendParameterValues(parameters + dateTypeToParameterMapper.toParameters(expectedTypes))
        .append(
            ")\n" +
                    "${INDENT_2})\n" +
                    "$INDENT}"
        )

    private fun appendParameterValues(parameters: List<TypedParameter>) =
        append(
            parameters.joinToString(", ") {
                mockerCodeGenerator.getMockedValue(it.name, it.type)
            }
        )

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