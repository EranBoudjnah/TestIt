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
        fun data(): Iterable<Array<*>> = setOf(
            testCase("ABC", DataType.Specific("ABC", false)),
            testCase("ABC?", DataType.Specific("ABC", true)),
            testCase(
                "ABC<DEF>?",
                DataType.Generic(
                    "ABC",
                    true,
                    DataType.Specific("DEF", false)
                )
            ),
            testCase(
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
            testCase(
                "(name:Value?)->Result",
                DataType.Lambda(
                    "Result",
                    false,
                    DataType.Specific("Value", true)
                )
            ),
            testCase(
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

        private fun testCase(
            dataType: String,
            concreteDataType: DataType
        ) = arrayOf(dataType, concreteDataType)
    }

    private lateinit var classUnderTest: DataTypeParser

    @Before
    fun setUp() {
        classUnderTest = DataTypeParser()
    }

    @Test
    fun `Given parse data and root null flag when parse then returns expected value`() {
        // When
        val actualValue = classUnderTest.parse(parseDataType)

        // Then
        assertEquals(parseExpected, actualValue)
    }
}
