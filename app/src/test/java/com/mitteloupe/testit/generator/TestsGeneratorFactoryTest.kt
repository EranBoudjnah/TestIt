package com.mitteloupe.testit.generator

import com.mitteloupe.testit.config.model.Configuration
import com.mitteloupe.testit.config.model.ExceptionCaptureMethod
import com.mitteloupe.testit.config.model.Mocker
import com.mitteloupe.testit.generator.formatting.Formatting
import com.mitteloupe.testit.generator.mapper.DateTypeToParameterMapper
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TestsGeneratorFactoryTest {
    private lateinit var cut: TestsGeneratorFactory

    @Mock
    lateinit var mockerCodeGeneratorProvider: MockerCodeGeneratorProvider

    @Mock
    lateinit var formatting: Formatting

    @Mock
    lateinit var dateTypeToParameterMapper: DateTypeToParameterMapper

    @Before
    fun setUp() {
        cut = TestsGeneratorFactory(
            mockerCodeGeneratorProvider,
            formatting,
            dateTypeToParameterMapper
        )
    }

    @Test
    fun `Given configuration when createTestsGenerator then queries correct dependencies`() {
        // Given
        val mocker = Mocker.MOCKITO
        val configuration = Configuration(
            mocker = mocker,
            classUnderTest = "classUnderTest",
            actualValue = "actual",
            defaultAssertion = "default",
            exceptionCaptureMethod = ExceptionCaptureMethod.NO_CAPTURE
        )
        given { mockerCodeGeneratorProvider.getGenerator(mocker, formatting) }.willReturn(mock())

        // When
        cut.createTestsGenerator(configuration)

        // Then
        verify(mockerCodeGeneratorProvider).getGenerator(mocker, formatting)
    }
}
