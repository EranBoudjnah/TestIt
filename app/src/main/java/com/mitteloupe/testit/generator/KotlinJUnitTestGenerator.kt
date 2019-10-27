package com.mitteloupe.testit.generator

import com.mitteloupe.testit.config.model.ExceptionCaptureMethod
import com.mitteloupe.testit.generator.mocking.MockerCodeGenerator
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.FunctionsMetadataContainer
import com.mitteloupe.testit.model.ImportsContainer
import com.mitteloupe.testit.model.StaticFunctionsMetadata
import com.mitteloupe.testit.model.TypedParameter
import com.mitteloupe.testit.model.concreteFunctions
import com.mitteloupe.testit.processing.hasReturnValue

class KotlinJUnitTestGenerator(
    private val stringBuilder: TestStringBuilder,
    private val mockerCodeGenerator: MockerCodeGenerator
) : TestsGenerator {
    private val usedImports = mutableMapOf<String, String>()

    private val knownImports = mutableMapOf<String, String>()

    init {
        reset()
    }

    override fun ClassMetadata.addToTests(
        isParameterized: Boolean,
        exceptionCaptureMethod: ExceptionCaptureMethod
    ) {
        setUpMockGenerator(isParameterized)
        evaluateImports(isParameterized, exceptionCaptureMethod)
        stringBuilder.appendTestClass(
            TestStringBuilderConfiguration(
                this,
                usedImports.values.toSet(),
                hasMockableConstructorParameters(constructorParameters),
                isParameterized
            )
        )
    }

    override fun StaticFunctionsMetadata.addToTests(
        outputClassName: String,
        isParameterized: Boolean,
        exceptionCaptureMethod: ExceptionCaptureMethod
    ) {
        setUpMockGenerator()
        evaluateImports(isParameterized, exceptionCaptureMethod)
        stringBuilder.appendFunctionsTestClass(
            this,
            usedImports.values.toSet(),
            outputClassName,
            isParameterized
        )
    }

    override fun reset() {
        stringBuilder.clear()
        usedImports.clear()

        with(knownImports) {
            clear()
            putAll(
                mutableMapOf(
                    "Before" to "org.junit.Before",
                    "Test" to "org.junit.Test",
                    "RunWith" to "org.junit.runner.RunWith",
                    "Parameterized" to "org.junit.runners.Parameterized",
                    "Parameters" to "org.junit.runners.Parameterized.Parameters",
                    "assertEquals" to "org.junit.Assert.assertEquals"
                )
            )
            putAll(mockerCodeGenerator.knownImports)
        }
    }

    override fun generateTests() = stringBuilder.toString()

    private fun ClassMetadata.setUpMockGenerator(isParameterized: Boolean) {
        if (isParameterized) {
            mockerCodeGenerator.setIsParameterizedTest()
            if (hasMockableFunctionReturnValues(concreteFunctions)) {
                mockerCodeGenerator.setHasMockedFunctionReturnValues()
            }
        }

        if (hasMockableConstructorParameters(constructorParameters)) {
            mockerCodeGenerator.setHasMockedConstructorParameters(this)
        }

        val concreteFunctions = concreteFunctions
        if (hasMockableFunctionParameters(concreteFunctions) ||
            hasMockableReceiver(concreteFunctions)
        ) {
            mockerCodeGenerator.setHasMockedFunctionParameters()
        }

        if (isAbstract) {
            mockerCodeGenerator.setIsAbstractClassUnderTest()
        }
    }

    private fun StaticFunctionsMetadata.setUpMockGenerator() {
        if (hasMockableFunctionParameters(functions) ||
            hasMockableReceiver(functions)
        ) {
            mockerCodeGenerator.setHasMockedFunctionParameters()
        }
    }

    private fun ClassMetadata.evaluateImports(
        isParameterized: Boolean,
        exceptionCaptureMethod: ExceptionCaptureMethod
    ) {
        evaluateMockCodeGeneratorImports()

        addImportIfKnown("Before")

        evaluateExceptionImports(
            exceptionCaptureMethod,
            functions
        )

        if (isParameterized) {
            evaluateParameterizedImports(functions)
        }

        evaluateFunctionImports()

        evaluateImportsContainerImports()
    }

    private fun StaticFunctionsMetadata.evaluateImports(
        isParameterized: Boolean,
        exceptionCaptureMethod: ExceptionCaptureMethod
    ) {
        evaluateMockCodeGeneratorImports()

        evaluateExceptionImports(
            exceptionCaptureMethod,
            functions
        )

        if (isParameterized) {
            evaluateParameterizedImports(functions)
        }

        evaluateFunctionImports()

        evaluateImportsContainerImports()
    }

    private fun evaluateMockCodeGeneratorImports() {
        val imports = mockerCodeGenerator.getRequiredImports()
        imports.forEach(::addImportIfKnown)
    }

    private fun evaluateParameterizedImports(functions: List<FunctionMetadata>) {
        addImportIfKnown("RunWith")
        addImportIfKnown("Parameterized")
        addImportIfKnown("Parameters")
        if (functions.any { function -> function.hasReturnValue() }) {
            addImportIfKnown("assertEquals")
        }
    }

    private fun evaluateExceptionImports(
        exceptionCaptureMethod: ExceptionCaptureMethod,
        functions: List<FunctionMetadata>
    ) {
        if (functions.isNotEmpty()) {
            addImportIfKnown("assertEquals")
        }
    }

    private fun FunctionsMetadataContainer.evaluateFunctionImports() {
        if (concreteFunctions.isNotEmpty()) {
            addImportIfKnown("Test")
        }
    }

    private fun ImportsContainer.evaluateImportsContainerImports() {
        imports.forEach { (a, b) -> usedImports[a] = b }
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

    private fun hasMockableFunctionReturnValues(functions: List<FunctionMetadata>) =
        functions.any { function ->
            mockerCodeGenerator.isMockable(function.returnType)
        }

    private fun MockerCodeGenerator.isMockable(parameter: TypedParameter) = parameter.isMockable()

    private fun MockerCodeGenerator.isMockable(dataType: DataType) = dataType.isMockable()
}
