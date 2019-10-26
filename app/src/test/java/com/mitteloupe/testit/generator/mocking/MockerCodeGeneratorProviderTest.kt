package com.mitteloupe.testit.generator.mocking

import com.mitteloupe.testit.generator.MockerCodeGeneratorProvider
import com.mitteloupe.testit.model.Mocker
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockerCodeGeneratorProviderTest {
    private lateinit var cut: MockerCodeGeneratorProvider

    @Before
    fun setUp() {
        cut = MockerCodeGeneratorProvider(mock())
    }

    @Test
    fun `Given MOCKITO when getGenerator then returns mockito code generator`() {
        // Given
        val mocker = Mocker.MOCKITO

        // When
        val actualValue = cut.getGenerator(mocker, mock())

        // Then
        assertTrue(actualValue is MockitoCodeGenerator)
    }

    @Test
    fun `Given MOCKK when getGenerator then returns mockk code generator`() {
        // Given
        val mocker = Mocker.MOCKK

        // When
        val actualValue = cut.getGenerator(mocker, mock())

        // Then
        assertTrue(actualValue is MockKCodeGenerator)
    }
}
