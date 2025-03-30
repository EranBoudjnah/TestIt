package com.mitteloupe.testit.generator

import com.mitteloupe.testit.config.model.ExceptionCaptureMethod
import com.mitteloupe.testit.generator.formatting.Formatting
import com.mitteloupe.testit.generator.formatting.expectedReturnValueVariableName
import com.mitteloupe.testit.generator.formatting.nameInTestFunctionName
import com.mitteloupe.testit.generator.formatting.toKotlinString
import com.mitteloupe.testit.generator.mocking.MockerCodeGenerator
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.FunctionsMetadataContainer
import com.mitteloupe.testit.model.StaticFunctionsMetadata
import com.mitteloupe.testit.model.TypedParameter
import com.mitteloupe.testit.model.concreteFunctions
import com.mitteloupe.testit.processing.hasReturnValue

class TestStringBuilder(
    private val stringBuilder: StringBuilder,
    private val formatting: Formatting,
    private val mockerCodeGenerator: MockerCodeGenerator,
    private val classUnderTestVariableName: String,
    private val actualValueVariableName: String,
    private val defaultAssertionStatement: String,
    private val exceptionCaptureMethod: ExceptionCaptureMethod
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
                classUnderTest.functions,
                isParameterized
            )
            .append(" {\n")
            .onlyIf(isParameterized) {
                appendParameterizedCompanionObject(classUnderTest)
                    .appendBlankLine()
            }
            .appendMockingRule(config.hasMockableConstructorParameters, isParameterized)
            .appendClassVariable(classUnderTest.className)
            .appendMocks(classUnderTest.constructorParameters)
            .appendSetUp(classUnderTest)
            .appendTests(
                false,
                classUnderTest,
                isParameterized
            )
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
            functionsUnderTest.functions,
            isParameterized
        )
        .append(" {\n")
        .appendTests(
            true,
            functionsUnderTest,
            isParameterized
        )
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

        return onlyIf(usedImports.isNotEmpty()) { append("\n") }
    }

    private fun appendTestClassRunnerAnnotation(
        hasMockableConstructorParameters: Boolean,
        isParameterized: Boolean
    ) = when {
        isParameterized -> {
            append("${mockerCodeGenerator.testClassParameterizedRunnerAnnotation}\n")
        }

        hasMockableConstructorParameters -> {
            mockerCodeGenerator.testClassBaseRunnerAnnotation?.let { annotation ->
                append("$annotation\n")
            }
            this
        }

        else -> this
    }

    private fun appendConstructorParameters(
        functions: List<FunctionMetadata>,
        isParameterized: Boolean
    ) = onlyIf(isParameterized) {
        val parameters = getFunctionParametersAsConstructorParameters(functions)
        onlyIf(parameters.isNotEmpty()) {
            append("(\n")
                .append(
                    parameters.joinToString(",\n") { parameter ->
                        "${indent()}private val ${parameter.name}: " +
                            parameter.type.toKotlinString()
                    }
                )
                .append("\n)")
        }
    }

    private fun getFunctionParametersAsConstructorParameters(
        functions: List<FunctionMetadata>
    ): List<TypedParameter> {
        val functionNameSuffixProvider = FunctionNameSuffixProvider(functions)
        return functions.flatMap { function ->
            function.parameters.filter { parameter -> parameter.type.isNotUnit }
                .map { parameter ->
                    val parameterName = parameter.toKotlinString(function, true)
                    TypedParameter(parameterName, parameter.type)
                } +
                emptyList<TypedParameter>().onlyIf(function.returnType.isNotUnit) {
                    val suffix = functionNameSuffixProvider.suffix(function)
                    listOf(
                        TypedParameter(
                            function.expectedReturnValueVariableName(suffix),
                            function.returnType
                        )
                    )
                }
        }
    }

    private fun appendMockingRule(
        hasMockableConstructorParameters: Boolean,
        isParameterized: Boolean
    ) = onlyIf(isParameterized && hasMockableConstructorParameters) {
        mockerCodeGenerator.mockingRule?.let {
            append(it).appendBlankLine()
        }
    }

    private fun appendMocks(classParameters: List<TypedParameter>) =
        append(
            classParameters.joinToString("\n\n") { parameter ->
                mockerCodeGenerator.getMockedVariableDefinition(parameter)
            }
        ).onlyIf(classParameters.isNotEmpty()) { appendBlankLine() }

    private fun appendSetUp(
        classUnderTest: ClassMetadata
    ): TestStringBuilder {
        append("${indent()}@Before\n")
            .append("${indent()}fun setUp() {\n")

        mockerCodeGenerator.setUpStatements?.let(::append)

        return append("${indent(2)}$classUnderTestVariableName = ")
            .appendIf(
                { classUnderTest.isAbstract },
                {
                    val abstractClassUnderTest =
                        mockerCodeGenerator.getAbstractClassUnderTest(classUnderTest)
                    "$abstractClassUnderTest\n"
                },
                {
                    "${classUnderTest.className}(" +
                        classUnderTest.constructorParameters
                            .joinToString(", ") { parameter -> parameter.name } +
                        ")\n"
                }
            ).append("${indent()}}")
            .appendBlankLine()
    }

    private fun appendTests(
        isStatic: Boolean,
        functionsMetadataContainer: FunctionsMetadataContainer,
        isParameterized: Boolean
    ): TestStringBuilder {
        val concreteFunctions = functionsMetadataContainer.concreteFunctions
        val lastIndex = concreteFunctions.size - 1
        val functionNameSuffixProvider = FunctionNameSuffixProvider(concreteFunctions)
        concreteFunctions.forEachIndexed { index, function ->
            val isOverloaded = !concreteFunctions.isSingle { functionMetadata ->
                functionMetadata.name == function.name &&
                    functionMetadata.extensionReceiverType ==
                    function.extensionReceiverType
            }
            val expectedSuffix = functionNameSuffixProvider.suffix(function)
            appendTest(isStatic, function, isOverloaded, isParameterized, expectedSuffix)
                .onlyIf(exceptionCaptureMethod != ExceptionCaptureMethod.NO_CAPTURE) {
                    append("\n")
                        .appendExceptionTest(
                            isStatic,
                            function,
                            isOverloaded,
                            isParameterized,
                            exceptionCaptureMethod,
                            expectedSuffix
                        )
                }
                .onlyIf(index != lastIndex) { append("\n") }
        }

        return this
    }

    private fun appendTest(
        isStatic: Boolean,
        function: FunctionMetadata,
        isOverloaded: Boolean,
        isParameterized: Boolean,
        expectedSuffix: String
    ) = append("${indent()}@Test\n")
        .append("${indent()}fun `Given _ when ${function.nameInTestFunctionName}")
        .onlyIf(isOverloaded) { appendFunctionParameterTypes(function.parameters) }
        .append(" then _`() {\n")
        .appendTestBody(
            isStatic,
            function,
            isParameterized,
            ExceptionCaptureMethod.NO_CAPTURE,
            expectedSuffix
        )
        .append("${indent()}}\n")

    private fun appendExceptionTest(
        isStatic: Boolean,
        function: FunctionMetadata,
        isOverloaded: Boolean,
        isParameterized: Boolean,
        exceptionCaptureMethod: ExceptionCaptureMethod,
        expectedSuffix: String
    ) = append("${indent()}@Test")
        .onlyIf(exceptionCaptureMethod == ExceptionCaptureMethod.ANNOTATION_EXPECTS) {
            append("(expected = Exception::class)")
        }
        .append("\n")
        .append("${indent()}fun `Given _ when ${function.nameInTestFunctionName}")
        .onlyIf(isOverloaded) { appendFunctionParameterTypes(function.parameters) }
        .append(" then throws exception`() {\n")
        .appendTestBody(isStatic, function, isParameterized, exceptionCaptureMethod, expectedSuffix)
        .append("${indent()}}\n")

    private fun appendFunctionParameterTypes(parameters: List<TypedParameter>) =
        append(
            "(" +
                parameters.joinToString(", ") { parameter ->
                    parameter.type.toKotlinString()
                } +
                ")"
        )

    private fun appendTestBody(
        isStatic: Boolean,
        function: FunctionMetadata,
        isParameterized: Boolean,
        exceptionCaptureMethod: ExceptionCaptureMethod,
        expectedSuffix: String
    ) = appendGiven(function, isParameterized, exceptionCaptureMethod)
        .appendWhen(isStatic, function, isParameterized, exceptionCaptureMethod)
        .appendThen(function, isParameterized, exceptionCaptureMethod, expectedSuffix)

    private fun appendGiven(
        function: FunctionMetadata,
        isParameterized: Boolean,
        exceptionCaptureMethod: ExceptionCaptureMethod
    ) = append("${indent(2)}// Given\n")
        .appendFunctionParameterMocks(function, isParameterized)
        .onlyIf(exceptionCaptureMethod == ExceptionCaptureMethod.TRY_CATCH) {
            append("${indent(2)}val expectedException = Exception()\n")
                .append("${indent(2)}lateinit var actualException: Exception\n")
        }
        .append("\n")

    private fun appendFunctionParameterMocks(
        function: FunctionMetadata,
        isParameterized: Boolean
    ) = onlyIf(!isParameterized) {
        function.parameters.filter { it.type.isNotUnit }
            .forEach { parameter ->
                val value = mockerCodeGenerator.getMockedValue(parameter.name, parameter.type)
                append("${indent(2)}val ${parameter.name} = $value\n")
            }
        this
    }.also {
        function.extensionReceiverType?.let { receiverType ->
            if (function.parameters.isNotEmpty()) {
                append("\n")
            }
            appendExtensionReceiver(receiverType)
        }
    }

    private fun appendExtensionReceiver(receiverType: DataType): TestStringBuilder {
        val receiverValue = mockerCodeGenerator.getMockedValue(receiverType.name, receiverType)
        return append("${indent(2)}val receiver = $receiverValue\n")
    }

    private fun appendWhen(
        isStatic: Boolean,
        function: FunctionMetadata,
        isParameterized: Boolean,
        expectsException: ExceptionCaptureMethod
    ) = append("${indent(2)}// When\n")
        .onlyIf(expectsException == ExceptionCaptureMethod.TRY_CATCH) {
            append(indent(2))
            append("try {\n")
            append(indent())
        }
        .append(indent(2))
        .onlyIf(expectsException == ExceptionCaptureMethod.NO_CAPTURE) {
            appendActualVariable(function)
        }
        .appendReceiverOpen(function, isStatic)
        .append("${function.name}(")
        .append(
            function.parameters.joinToString(", ") { parameter ->
                parameter.toKotlinString(function, isParameterized)
            }
        )
        .append(")")
        .appendReceiverClose(function, isStatic)
        .onlyIf(expectsException == ExceptionCaptureMethod.TRY_CATCH) {
            append("\n")
                .append("${indent(2)}} catch (exception: Exception) {\n")
                .append("${indent(3)}actualException = exception\n")
                .append("${indent(2)}}")
        }
        .appendBlankLine()

    private fun appendActualVariable(
        function: FunctionMetadata
    ) = onlyIf(function.hasReturnValue()) { append("val $actualValueVariableName = ") }

    private fun appendReceiverOpen(
        function: FunctionMetadata,
        isStatic: Boolean
    ) = append(
        function.extensionReceiverType?.let {
            val classUnderTestWrapperOpen =
                if (isStatic) "" else "with($classUnderTestVariableName) {\n${indent(3)}"
            "${classUnderTestWrapperOpen}receiver."
        } ?: if (isStatic) "" else "$classUnderTestVariableName."
    )

    private fun appendReceiverClose(
        function: FunctionMetadata,
        isStatic: Boolean
    ) = onlyIf(!isStatic) {
        function.extensionReceiverType?.let {
            append("\n${indent(2)}}")
        }
    }

    private fun appendThen(
        function: FunctionMetadata,
        isParameterized: Boolean,
        exceptionCaptureMethod: ExceptionCaptureMethod,
        expectedSuffix: String
    ) = append("${indent(2)}// Then\n")
        .onlyIf(exceptionCaptureMethod == ExceptionCaptureMethod.NO_CAPTURE) {
            appendReturnValueAssertion(function, isParameterized, expectedSuffix)
        }
        .append(indent(2))
        .append {
            when (exceptionCaptureMethod) {
                ExceptionCaptureMethod.TRY_CATCH -> {
                    "assertEquals(expectedException, actualException)\n"
                }

                ExceptionCaptureMethod.ANNOTATION_EXPECTS -> "// Exception is thrown\n"
                else -> "$defaultAssertionStatement\n"
            }
        }

    private fun appendReturnValueAssertion(
        function: FunctionMetadata,
        isParameterized: Boolean,
        expectedSuffix: String
    ) = onlyIf(function.hasReturnValue() && isParameterized) {
        append(
            indent(2) + "assertEquals(" +
                function.expectedReturnValueVariableName(expectedSuffix) + ", " +
                actualValueVariableName + ")\n"
        )
    }

    private fun appendBlankLine() = append("\n\n")

    private fun appendClassVariable(
        className: String
    ) = append("${indent()}private lateinit var $classUnderTestVariableName: $className")
        .appendBlankLine()

    private fun appendParameterizedCompanionObject(
        classUnderTest: ClassMetadata
    ): TestStringBuilder {
        val parameters = getFunctionParametersAsConstructorParameters(classUnderTest.functions)
        return appendParameterizedCompanionObject(parameters)
    }

    private fun appendParameterizedCompanionObject(parameters: List<TypedParameter>) = append(
        "${indent()}companion object {\n" +
            "${indent(2)}@JvmStatic\n" +
            "${indent(2)}@Parameters\n" +
            "${indent(2)}fun data(): Iterable<Array<*>> = setOf(\n" +
            "${indent(3)}arrayOf("
    )
        .append(
            parameters.joinToString(", ") {
                mockerCodeGenerator.getMockedValue(it.name, it.type)
            }
        )
        .append(
            ")\n" +
                "${indent(2)})\n" +
                "${indent()}}"
        )

    private fun append(string: String): TestStringBuilder {
        stringBuilder.append(string)
        return this
    }

    private fun append(block: () -> String) = append(block())

    private fun appendIf(
        condition: () -> Boolean,
        stringIfTrue: () -> String,
        stringElse: () -> String
    ) = append(
        if (condition()) {
            stringIfTrue()
        } else {
            stringElse()
        }
    )

    private fun indent(indentation: Int = 1) = formatting.getIndentation(indentation)
}

class FunctionNameSuffixProvider(private val functions: Iterable<FunctionMetadata>) {
    private val functionNameCounter = mutableMapOf<String, Int>()

    fun suffix(function: FunctionMetadata) = if (functionNameCounter.containsKey(function.name)) {
        val occurrences = (functionNameCounter[function.name] ?: 0) + 1
        functionNameCounter[function.name] = occurrences
        "$occurrences"
    } else {
        functionNameCounter[function.name] = 1
        val hasMultipleUses = functions.count { it.name == function.name } > 1
        if (hasMultipleUses) {
            "1"
        } else {
            ""
        }
    }
}

private fun List<FunctionMetadata>.isSingle(predicate: (function: FunctionMetadata) -> Boolean) =
    singleOrNull { function -> predicate(function) } != null

private fun <T : Any> T.onlyIf(isTrue: Boolean, action: T.() -> T?) =
    if (isTrue) {
        action() ?: this
    } else {
        this
    }
