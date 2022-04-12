package com.mitteloupe.testit.generator

import com.mitteloupe.testit.config.model.Configuration
import com.mitteloupe.testit.config.model.Mocker
import com.mitteloupe.testit.generator.formatting.Formatting
import com.mitteloupe.testit.generator.mocking.MockKCodeGenerator
import com.mitteloupe.testit.generator.mocking.MockableTypeQualifier
import com.mitteloupe.testit.generator.mocking.MockitoCodeGenerator

class TestsGeneratorFactory(
    private val mockerCodeGeneratorProvider: MockerCodeGeneratorProvider,
    private val formatting: Formatting
) {
    fun createTestsGenerator(configuration: Configuration): KotlinJUnitTestGenerator {
        val mockerCodeGenerator =
            mockerCodeGeneratorProvider.getGenerator(
                configuration.mocker,
                formatting,
                configuration.mockitoRule
            )
        return KotlinJUnitTestGenerator(
            TestStringBuilder(
                StringBuilder(),
                formatting,
                mockerCodeGenerator,
                configuration.classUnderTest,
                configuration.actualValue,
                configuration.defaultAssertion,
                configuration.exceptionCaptureMethod
            ),
            mockerCodeGenerator
        )
    }
}

class MockerCodeGeneratorProvider(private val mockableTypeQualifier: MockableTypeQualifier) {
    fun getGenerator(mocker: Mocker, formatting: Formatting, mockitoRuleVariableName: String) =
        when (mocker) {
            Mocker.MOCKITO -> {
                MockitoCodeGenerator(mockableTypeQualifier, formatting, mockitoRuleVariableName)
            }
            Mocker.MOCKK -> MockKCodeGenerator(mockableTypeQualifier, formatting)
        }
}
