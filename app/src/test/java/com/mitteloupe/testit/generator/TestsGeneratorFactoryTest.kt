package com.mitteloupe.testit.generator

import com.mitteloupe.testit.config.model.Configuration
import com.mitteloupe.testit.config.model.ExceptionCaptureMethod
import com.mitteloupe.testit.config.model.Mocker
import com.mitteloupe.testit.generator.formatting.Formatting
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(MockitoJUnitRunner::class)
class TestsGeneratorFactoryTest {
    private lateinit var classUnderTest: TestsGeneratorFactory

    @Mock
    lateinit var mockerCodeGeneratorProvider: MockerCodeGeneratorProvider

    @Mock
    lateinit var formatting: Formatting

    @Before
    fun setUp() {
        classUnderTest = TestsGeneratorFactory(
            mockerCodeGeneratorProvider,
            formatting
        )
    }

    @Test
    fun `Given configuration when createTestsGenerator then queries correct dependencies`() {
        // Given
        val mocker = Mocker.MOCKITO
        val mockitoRule = "mockitoRule"
        val configuration = Configuration(
            mocker = mocker,
            mockitoRule = mockitoRule,
            classUnderTest = "classUnderTest",
            actualValue = "actual",
            defaultAssertion = "default",
            exceptionCaptureMethod = ExceptionCaptureMethod.NO_CAPTURE
        )
        given {
            mockerCodeGeneratorProvider.getGenerator(mocker, formatting, mockitoRule)
        }.willReturn(mock())

        // When
        classUnderTest.createTestsGenerator(configuration)

        // Then
        verify(mockerCodeGeneratorProvider).getGenerator(mocker, formatting, mockitoRule)
    }
}
