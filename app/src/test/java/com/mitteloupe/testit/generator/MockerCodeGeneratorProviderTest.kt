package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.Mocker
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockerCodeGeneratorProviderTest {
    private lateinit var cut: MockerCodeGeneratorProvider

    @Before
    fun setUp() {
        cut = MockerCodeGeneratorProvider()
    }

    @Test
    fun `Given MOCKITO when getGenerator then returns mockito code generator`() {
        // Given
        val mocker = Mocker.MOCKITO

        // When
        val actualValue = cut.getGenerator(mocker)

        // Then
        assertTrue(actualValue is MockitoCodeGenerator)
    }

    @Test
    fun `Given MOCKK when getGenerator then returns mockk code generator`() {
        // Given
        val mocker = Mocker.MOCKK

        // When
        val actualValue = cut.getGenerator(mocker)

        // Then
        assertTrue(actualValue is MockKCodeGenerator)
    }
}
