package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.Configuration
import com.mitteloupe.testit.model.Mocker

class TestsGeneratorFactory {
    fun createTestsGenerator(configuration: Configuration): TestsGenerator {
        val mockerCodeGenerator = when (configuration.mocker) {
            Mocker.MOCKITO -> MockitoCodeGenerator()
            Mocker.MOCKK -> MockKCodeGenerator()
        }
        return KotlinJUnitTestGenerator(
            StringBuilder(),
            mockerCodeGenerator,
            configuration.classUnderTest,
            configuration.actualValue,
            configuration.defaultAssertion
        )
    }
}