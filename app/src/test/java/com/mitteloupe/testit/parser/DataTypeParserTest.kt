package com.mitteloupe.testit.parser

import com.mitteloupe.testit.model.DataType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class DataTypeParserTest(
    private val parseDataType: String,
    private val parseExpected: DataType
) {
    companion object {
        @JvmStatic
        @Parameters(name = "Parsing {0}")
        fun data(): Collection<Array<*>> = listOf(
            arrayOf("ABC", DataType.Specific("ABC", false)),
            arrayOf("ABC?", DataType.Specific("ABC", true)),
            arrayOf(
                "ABC<DEF>?",
                DataType.Generic(
                    "ABC",
                    true,
                    DataType.Specific("DEF", false)
                )
            ),
            arrayOf(
                "A<B?,C<D,E>?,F<G>>",
                DataType.Generic(
                    "A",
                    false,
                    DataType.Specific("B", true),
                    DataType.Generic(
                        "C",
                        true,
                        DataType.Specific("D", false),
                        DataType.Specific("E", false)
                    ),
                    DataType.Generic(
                        "F",
                        false,
                        DataType.Specific("G", false)
                    )
                )
            ),
            arrayOf(
                "(name:Value?)->Result",
                DataType.Lambda(
                    "Result",
                    false,
                    DataType.Specific("Value", true)
                )
            ),
            arrayOf(
                "(ABC<DEF>?)->G123",
                DataType.Lambda(
                    "G123",
                    false,
                    DataType.Generic(
                        "ABC",
                        true,
                        DataType.Specific("DEF", false)
                    )
                )
            )
        )
    }

    private lateinit var cut: DataTypeParser

    @Before
    fun setUp() {
        cut = DataTypeParser()
    }

    @Test
    fun `Given parse data and root null flag when parse then returns expected value`() {
        // When
        val actualValue = cut.parse(parseDataType)

        // Then
        assertEquals(parseExpected, actualValue)
    }
}
