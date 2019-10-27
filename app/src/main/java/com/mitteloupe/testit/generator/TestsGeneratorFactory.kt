package com.mitteloupe.testit.generator

import com.mitteloupe.testit.generator.formatting.Formatting
import com.mitteloupe.testit.generator.mapper.DateTypeToParameterMapper
import com.mitteloupe.testit.generator.mocking.MockKCodeGenerator
import com.mitteloupe.testit.generator.mocking.MockableTypeQualifier
import com.mitteloupe.testit.generator.mocking.MockitoCodeGenerator
import com.mitteloupe.testit.config.model.Configuration
import com.mitteloupe.testit.config.model.Mocker

class TestsGeneratorFactory(
    private val mockerCodeGeneratorProvider: MockerCodeGeneratorProvider,
    private val formatting: Formatting,
    private val dataTypeToParameterMapper: DateTypeToParameterMapper
) {
    fun createTestsGenerator(configuration: Configuration): KotlinJUnitTestGenerator {
        val mockerCodeGenerator =
            mockerCodeGeneratorProvider.getGenerator(configuration.mocker, formatting)
        return KotlinJUnitTestGenerator(
            TestStringBuilder(
                StringBuilder(),
                formatting,
                mockerCodeGenerator,
                configuration.classUnderTest,
                configuration.actualValue,
                configuration.defaultAssertion,
                configuration.exceptionCaptureMethod,
                dataTypeToParameterMapper
            ),
            mockerCodeGenerator
        )
    }
}

class MockerCodeGeneratorProvider(private val mockableTypeQualifier: MockableTypeQualifier) {
    fun getGenerator(mocker: Mocker, formatting: Formatting) = when (mocker) {
        Mocker.MOCKITO -> MockitoCodeGenerator(mockableTypeQualifier, formatting)
        Mocker.MOCKK -> MockKCodeGenerator(mockableTypeQualifier, formatting)
    }
}