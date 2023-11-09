package com.mitteloupe.testit.generator.mocking

import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.TypedParameter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class MockableTypeQualifierTypedParameterTest(
    private val givenTypedParameter: TypedParameter,
    private val expected: Boolean
) {
    companion object {
        @JvmStatic
        @Parameters(name = "Given {0}")
        fun data(): Collection<Array<*>> = listOf(
            expectedSpecificTestCase(parameterName = "Name", dataTypeName = "Boolean"),
            expectedSpecificTestCase(
                parameterName = "Name",
                dataTypeName = "Byte",
                isNullable = true
            ),
            expectedSpecificTestCase(parameterName = "Name", dataTypeName = "Class"),
            expectedSpecificTestCase(
                parameterName = "Name",
                dataTypeName = "Double",
                isNullable = true
            ),
            expectedSpecificTestCase(parameterName = "Name", dataTypeName = "Float"),
            expectedSpecificTestCase(
                parameterName = "Name",
                dataTypeName = "Int",
                isNullable = true
            ),
            expectedSpecificTestCase(parameterName = "Name", dataTypeName = "Integer"),
            expectedSpecificTestCase(
                parameterName = "Name",
                dataTypeName = "Long",
                isNullable = true
            ),
            expectedSpecificTestCase(parameterName = "Name", dataTypeName = "Short"),
            expectedSpecificTestCase(
                parameterName = "Name",
                dataTypeName = "String",
                isNullable = true
            ),
            expectedSpecificTestCase(parameterName = "Name", dataTypeName = "Array"),
            expectedSpecificTestCase(
                parameterName = "Name",
                dataTypeName = "List",
                isNullable = true
            ),
            expectedSpecificTestCase(parameterName = "Name", dataTypeName = "MutableList"),
            expectedSpecificTestCase(
                parameterName = "Name",
                dataTypeName = "Map",
                isNullable = true
            ),
            expectedSpecificTestCase(parameterName = "Name", dataTypeName = "MutableMap"),
            expectedSpecificTestCase(
                parameterName = "Name",
                dataTypeName = "Set",
                isNullable = true
            ),
            expectedSpecificTestCase(parameterName = "Name", dataTypeName = "MutableSet"),
            expectedSpecificTestCase(
                parameterName = "Name",
                dataTypeName = "Unit",
                isNullable = true
            ),
            expectedSpecificTestCase(
                parameterName = "Name",
                dataTypeName = "Unknown",
                expected = true
            ),
            arrayOf(TypedParameter("Name", DataType.Lambda("Lambda", true)), true),
            arrayOf(TypedParameter("Name", DataType.Generic("Generic", false)), true)
        )

        private fun expectedSpecificTestCase(
            parameterName: String,
            dataTypeName: String,
            isNullable: Boolean = false,
            expected: Boolean = false
        ) = arrayOf(
            TypedParameter(
                name = parameterName,
                type = DataType.Specific(dataTypeName, isNullable)
            ),
            expected
        )
    }

    private lateinit var classUnderTest: MockableTypeQualifier

    @Before
    fun setUp() {
        classUnderTest = MockableTypeQualifier()
    }

    @Test
    fun `When isMockable(TypedParameter) then return expected mockable state`() {
        // Given

        // When
        val actualValue = classUnderTest.isMockable(givenTypedParameter)

        // Then
        assertEquals(expected, actualValue)
    }
}
