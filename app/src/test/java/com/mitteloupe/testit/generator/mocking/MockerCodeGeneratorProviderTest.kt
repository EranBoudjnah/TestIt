package com.mitteloupe.testit.generator.mocking

import com.mitteloupe.testit.config.model.Mocker
import com.mitteloupe.testit.generator.MockerCodeGeneratorProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class MockerCodeGeneratorProviderTest {
    private lateinit var classUnderTest: MockerCodeGeneratorProvider

    @Before
    fun setUp() {
        classUnderTest = MockerCodeGeneratorProvider(mock())
    }

    @Test
    fun `Given MOCKITO when getGenerator then returns mockito code generator`() {
        // Given
        val mocker = Mocker.MOCKITO
        val mockitoRuleVariableName = "mockitoRule"

        // When
        val actualValue = classUnderTest.getGenerator(mocker, mock(), mockitoRuleVariableName)

        // Then
        assertTrue(actualValue is MockitoCodeGenerator)
    }

    @Test
    fun `Given MOCKK when getGenerator then returns mockk code generator`() {
        // Given
        val mocker = Mocker.MOCKK
        val mockitoRuleVariableName = "canBeAnything"

        // When
        val actualValue = classUnderTest.getGenerator(mocker, mock(), mockitoRuleVariableName)

        // Then
        assertTrue(actualValue is MockKCodeGenerator)
    }
}
