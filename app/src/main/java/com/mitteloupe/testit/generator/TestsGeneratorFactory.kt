package com.mitteloupe.testit.generator

import com.mitteloupe.testit.generator.mocking.MockKCodeGenerator
import com.mitteloupe.testit.generator.mocking.MockableTypeQualifier
import com.mitteloupe.testit.generator.mocking.MockitoCodeGenerator
import com.mitteloupe.testit.model.Configuration
import com.mitteloupe.testit.model.Mocker

class TestsGeneratorFactory(
    private val mockerCodeGeneratorProvider: MockerCodeGeneratorProvider
) {
    fun createTestsGenerator(configuration: Configuration) = KotlinJUnitTestGenerator(
        StringBuilder(),
        mockerCodeGeneratorProvider.getGenerator(configuration.mocker),
        configuration.classUnderTest,
        configuration.actualValue,
        configuration.defaultAssertion
    )
}

class MockerCodeGeneratorProvider(private val mockableTypeQualifier: MockableTypeQualifier) {
    fun getGenerator(mocker: Mocker) = when (mocker) {
        Mocker.MOCKITO -> MockitoCodeGenerator(mockableTypeQualifier)
        Mocker.MOCKK -> MockKCodeGenerator(mockableTypeQualifier)
    }
}