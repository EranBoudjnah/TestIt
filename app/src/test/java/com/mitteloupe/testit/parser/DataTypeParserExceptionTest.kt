package com.mitteloupe.testit.parser

import org.junit.Before
import org.junit.Test

class DataTypeParserExceptionTest {
    private lateinit var classUnderTest: DataTypeParser

    @Before
    fun setUp() {
        classUnderTest = DataTypeParser()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Given invalid data type when parse then throws exception`() {
        // Given
        val dataType = "<A"

        // When
        classUnderTest.parse(dataType)

        // Then
        // Exception is thrown
    }
}
