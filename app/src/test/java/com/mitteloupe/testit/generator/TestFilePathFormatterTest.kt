package com.mitteloupe.testit.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.MethodRule
import org.mockito.junit.MockitoJUnit

class TestFilePathFormatterTest {
    private lateinit var cut: TestFilePathFormatter

    @get:Rule
    val rule: MethodRule = MockitoJUnit.rule()

    @Before
    fun setUp() {
        cut = TestFilePathFormatter()
    }

    @Test
    fun `Given source file name when getTestFilePath then returns expected path for test`() {
        // Given
        val basePath = "/Users/me/Projects/TestIt/app/src"
        val sourceFileName = "$basePath/main/java/com/mitteloupe/testit/generator/TestFilePathFormatter.kt"
        val expected = "$basePath/test/java/com/mitteloupe/testit/generator/"

        // When
        val actual = cut.getTestFilePath(sourceFileName)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `Given unrecognized file name when getTestFilePath then returns null`() {
        // Given
        val sourceFileName = "/Users/me/Projects/TestIt/app/source/main/java/com/mitteloupe/testit/generator/TestFilePathFormatter.kt"

        // When
        val actual = cut.getTestFilePath(sourceFileName)

        // Then
        assertNull(actual)
    }
}
