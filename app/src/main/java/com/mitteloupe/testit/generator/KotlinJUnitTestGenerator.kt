package com.mitteloupe.testit.generator

import com.mitteloupe.testit.generator.mocking.MockerCodeGenerator
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.FunctionsMetadataContainer
import com.mitteloupe.testit.model.ImportsContainer
import com.mitteloupe.testit.model.StaticFunctionsMetadata
import com.mitteloupe.testit.model.TypedParameter
import com.mitteloupe.testit.model.concreteFunctions

class KotlinJUnitTestGenerator(
    private val stringBuilder: TestStringBuilder,
    private val mockerCodeGenerator: MockerCodeGenerator
) : TestsGenerator {
    private val usedImports = mutableMapOf<String, String>()

    private val knownImports = mutableMapOf<String, String>()

    init {
        reset()
    }

    override fun ClassMetadata.addToTests(isParameterized: Boolean) {
        setUpMockGenerator(isParameterized)
        evaluateImports()
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
        isParameterized: Boolean
    ) {
        setUpMockGenerator()
        evaluateImports()
        stringBuilder.appendFunctionsTestClass(this, usedImports.values.toSet(), outputClassName, isParameterized)
    }

    override fun reset() {
        stringBuilder.clear()
        usedImports.clear()

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

    private fun ClassMetadata.setUpMockGenerator(isParameterized: Boolean) {
        if (isParameterized) {
            mockerCodeGenerator.setIsParameterizedTest()
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


    private fun ClassMetadata.evaluateImports() {
        evaluateMockCodeGeneratorImports()

        addImportIfKnown("Before")

        evaluateFunctionImports()

        evaluateImportsContainerImports()
    }

    private fun StaticFunctionsMetadata.evaluateImports() {
        evaluateMockCodeGeneratorImports()

        evaluateFunctionImports()

        evaluateImportsContainerImports()
    }

    private fun evaluateMockCodeGeneratorImports() {
        val imports = mockerCodeGenerator.getRequiredImports()
        imports.forEach(::addImportIfKnown)
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

    private fun MockerCodeGenerator.isMockable(parameter: TypedParameter) = parameter.isMockable()

    private fun MockerCodeGenerator.isMockable(dataType: DataType) = dataType.isMockable()
}
