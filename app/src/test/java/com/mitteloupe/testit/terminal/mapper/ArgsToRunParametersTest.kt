package com.mitteloupe.testit.terminal.mapper

import com.mitteloupe.testit.terminal.model.RunParameters
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ArgsToRunParametersTest(
    private val toParametersArgs: Array<String>,
    private val toParametersExpected: RunParameters
) {
    companion object {
        @JvmStatic
        @Parameters
        fun data(): Iterable<Array<*>> = setOf(
            testCase(arrayOf("a"), RunParameters("a", false)),
            testCase(arrayOf("b", "-p"), RunParameters("b", true))
        )

        private fun testCase(arguments: Array<String>, parameters: RunParameters) =
            arrayOf(arguments, parameters)
    }

    private lateinit var classUnderTest: ArgsToRunParameters

    @Before
    fun setUp() {
        classUnderTest = ArgsToRunParameters()
    }

    @Test
    fun `Given array of parameters when toParameters then returns expected RunParameters`() {
        // When
        val actualValue = classUnderTest.toParameters(toParametersArgs)

        // Then
        assertEquals(toParametersExpected, actualValue)
    }
}
