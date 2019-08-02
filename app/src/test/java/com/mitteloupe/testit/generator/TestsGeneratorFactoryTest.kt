package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.Configuration
import com.mitteloupe.testit.model.Mocker
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

    @Before
    fun setUp() {
        cut = TestsGeneratorFactory(mockerCodeGeneratorProvider)
    }

    @Test
    fun `Given configuration when createTestsGenerator then queries correct dependencies`() {
        // Given
        val mocker = Mocker.MOCKITO
        val configuration = Configuration(
            mocker = mocker,
            classUnderTest = "classUnderTest",
            actualValue = "actual",
            defaultAssertion = "default"
        )
        given { mockerCodeGeneratorProvider.getGenerator(mocker) }.willReturn(mock())

        // When
        cut.createTestsGenerator(configuration)

        // Then
        verify(mockerCodeGeneratorProvider).getGenerator(mocker)
    }
}
