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
        fun data(): Collection<Array<*>> = listOf(
            arrayOf(arrayOf("a"), RunParameters("a", false)),
            arrayOf(arrayOf("b", "-p"), RunParameters("b", true))
        )
    }

    private lateinit var cut: ArgsToRunParameters

    @Before
    fun setUp() {
        cut = ArgsToRunParameters()
    }

    @Test
    fun `Given array of parameters when toParameters then returns expected RunParameters`() {
        // When
        val actualValue = cut.toParameters(toParametersArgs)

        // Then
        assertEquals(toParametersExpected, actualValue)
    }
}
