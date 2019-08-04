package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.FunctionsMetadataContainer
import com.mitteloupe.testit.model.ImportsContainer
import com.mitteloupe.testit.model.StaticFunctionsMetadata
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

    init {
        reset()
    }

    override fun ClassMetadata.addToTests() {
        setUpMockGenerator(this)
        stringBuilder.appendTestClass(this)
    }

    override fun StaticFunctionsMetadata.addToTests(outputClassName: String) {
        setUpMockGenerator(this)
        stringBuilder.appendFunctionsTestClass(this, outputClassName)
    }

    override fun reset() {
        stringBuilder.clear()
        mockerCodeGenerator.reset()
        with(usedImports) {
            clear()
        }

        with(knownImports) {
            clear()
            putAll(
                mutableMapOf(
                    "Before" to "org.junit.Before",
                    "Test" to "org.junit.Test"
                )
            )
            putAll(mockerCodeGenerator.knownImports)
        }
    }

    override fun generateTests() = stringBuilder.toString()

    private fun setUpMockGenerator(classUnderTest: ClassMetadata) {
        if (hasMockableConstructorParameters(classUnderTest.constructorParameters)) {
            mockerCodeGenerator.setHasMockedConstructorParameters(classUnderTest)
        }

        val concreteFunctions = classUnderTest.concreteFunctions
        if (hasMockableFunctionParameters(concreteFunctions) ||
            hasMockableReceiver(concreteFunctions)
        ) {
            mockerCodeGenerator.setHasMockedFunctionParameters()
        }

        if (classUnderTest.isAbstract) {
            mockerCodeGenerator.setIsAbstractClassUnderTest()
        }
    }

    private fun setUpMockGenerator(functionsUnderTest: StaticFunctionsMetadata) {
        if (hasMockableFunctionParameters(functionsUnderTest.functions) ||
            hasMockableReceiver(functionsUnderTest.functions)
        ) {
            mockerCodeGenerator.setHasMockedFunctionParameters()
        }
    }

    private fun StringBuilder.appendTestClass(classUnderTest: ClassMetadata): StringBuilder {
        evaluateImports(classUnderTest)

        return appendPackageName(classUnderTest.packageName)
            .appendImports()
            .appendTestClassAnnotation(classUnderTest.constructorParameters)
            .append("class ${classUnderTest.className}Test {\n")
            .appendClassVariable(classUnderTest.className)
            .appendMocks(classUnderTest.constructorParameters)
            .appendSetUp(classUnderTest)
            .appendTests(false, classUnderTest)
            .append("}\n")
    }

    private fun StringBuilder.appendFunctionsTestClass(
        functionsUnderTest: StaticFunctionsMetadata,
        outputClassName: String
    ): StringBuilder {
        evaluateImports(functionsUnderTest)

        return appendPackageName(functionsUnderTest.packageName)
            .appendImports()
            .append("class ${outputClassName}Test {\n")
            .appendTests(true, functionsUnderTest)
            .append("}\n")
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
            mockerCodeGenerator.getMockedVariableDefinition(parameter)
        })

        if (classParameters.isNotEmpty()) {
            appendBlankLine()
        }

        return this
    }

    private fun StringBuilder.appendSetUp(
        classUnderTest: ClassMetadata
    ): StringBuilder {
        append("$INDENT@Before\n")
            .append("${INDENT}fun setUp() {\n")

        mockerCodeGenerator.setUpStatements?.let {
            append(it)
        }

        append("$INDENT_2$classUnderTestVariableName = ")
        if (classUnderTest.isAbstract) {
            val abstractClassUnderTest = mockerCodeGenerator.getAbstractClassUnderTest(classUnderTest)
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

    private fun StringBuilder.appendTests(
        isStatic: Boolean,
        functionsMetadataContainer: FunctionsMetadataContainer
    ): StringBuilder {
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

    private fun StringBuilder.appendTest(
        isStatic: Boolean,
        function: FunctionMetadata
    ) = append("$INDENT@Test\n")
        .append("${INDENT}fun `Given _ when ${function.nameForTestFunctionName} then _`() {\n")
        .appendTestBody(isStatic, function)
        .append("$INDENT}\n")

    private fun StringBuilder.appendTestBody(
        isStatic: Boolean,
        function: FunctionMetadata
    ) = appendGiven(function)
        .appendWhen(isStatic, function)
        .appendThen()

    private fun StringBuilder.appendGiven(
        function: FunctionMetadata
    ) = append("$INDENT_2// Given\n")
        .appendFunctionParameterMocks(function)

    private fun StringBuilder.appendFunctionParameterMocks(function: FunctionMetadata): StringBuilder {
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

    private fun StringBuilder.appendExtensionReceiver(receiverType: DataType): java.lang.StringBuilder? {
        val receiverValue = mockerCodeGenerator.getMockedValue(receiverType.name, receiverType)
        return append("${INDENT_2}val receiver = $receiverValue\n")
    }

    private fun StringBuilder.appendWhen(
        isStatic: Boolean,
        function: FunctionMetadata
    ): StringBuilder {
        val actualVariable = if (function.returnType.name != "Unit") {
            "val $actualValueVariableName = "
        } else {
            ""
        }
        val receiver = function.extensionReceiverType?.let {
            val classUnderTestWrapperOpen = if (isStatic) "" else "with($classUnderTestVariableName) {\n$INDENT_3"
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

    private fun StringBuilder.appendThen() = append("$INDENT_2// Then\n")
        .append("$INDENT_2$defaultAssertionStatement\n")

    private fun StringBuilder.appendBlankLine() = append("\n\n")

    private fun StringBuilder.appendClassVariable(
        className: String
    ) = append("${INDENT}private lateinit var $classUnderTestVariableName: $className\n\n")

    private fun evaluateImports(classUnderTest: ClassMetadata) {
        evaluateMockCodeGeneratorImports()

        addImportIfKnown("Before")

        evaluateFunctionImports(classUnderTest)

        evaluateImportsContainerImports(classUnderTest)
    }

    private fun evaluateImports(functionsUnderTest: StaticFunctionsMetadata) {
        evaluateMockCodeGeneratorImports()

        evaluateFunctionImports(functionsUnderTest)

        evaluateImportsContainerImports(functionsUnderTest)
    }

    private fun evaluateMockCodeGeneratorImports() {
        val imports = mockerCodeGenerator.getRequiredImports()
        imports.forEach(::addImportIfKnown)
    }

    private fun evaluateFunctionImports(functionsMetadataContainer: FunctionsMetadataContainer) {
        if (functionsMetadataContainer.concreteFunctions.isNotEmpty()) {
            addImportIfKnown("Test")
        }
    }

    private fun evaluateImportsContainerImports(importsContainer: ImportsContainer) {
        importsContainer.imports.forEach { (a, b) -> usedImports[a] = b }
    }

    private fun addImportIfKnown(entityName: String) {
        knownImports[entityName]?.let { qualifiedName ->
            usedImports[entityName] = qualifiedName
            knownImports.remove(entityName)
        }
    }

    private fun hasMockableConstructorParameters(constructorParameters: List<TypedParameter>) =
        constructorParameters.any { parameter -> mockerCodeGenerator.isMockable(parameter) }

    private fun hasMockableFunctionParameters(functions: List<FunctionMetadata>) =
        functions.any { function ->
            function.parameters.any { parameter -> mockerCodeGenerator.isMockable(parameter) }
        }

    private fun hasMockableReceiver(functions: List<FunctionMetadata>) =
        functions.any { function ->
            function.extensionReceiverType?.let { dataType ->
                mockerCodeGenerator.isMockable(dataType)
            } ?: false
        }

    private val FunctionsMetadataContainer.concreteFunctions
        get() = functions.filter { !it.isAbstract }

    private val FunctionMetadata.nameForTestFunctionName
        get() = extensionReceiverType?.let { "${extensionReceiverType.name}#$name" } ?: name

    private fun MockerCodeGenerator.isMockable(parameter: TypedParameter) = parameter.isMockable()

    private fun MockerCodeGenerator.isMockable(dataType: DataType) = dataType.isMockable()
}
