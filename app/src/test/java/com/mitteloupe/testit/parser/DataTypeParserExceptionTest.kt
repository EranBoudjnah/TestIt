package com.mitteloupe.testit.parser

import org.junit.Before
import org.junit.Test

class DataTypeParserExceptionTest {
    private lateinit var cut: DataTypeParser

    @Before
    fun setUp() {
        cut = DataTypeParser()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Given invalid data type when parse then throws exception`() {
        // Given
        val dataType = "<A"

        // When
        cut.parse(dataType)

        // Then
        // Exception is thrown
    }
}
