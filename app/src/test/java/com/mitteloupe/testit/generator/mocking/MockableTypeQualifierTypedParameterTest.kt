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
        fun data(): Iterable<Array<*>> = setOf(
            testCase(parameterName = "Name", dataTypeName = "Boolean"),
            testCase(
                parameterName = "Name",
                dataTypeName = "Byte",
                isNullable = true
            ),
            testCase(parameterName = "Name", dataTypeName = "Class"),
            testCase(
                parameterName = "Name",
                dataTypeName = "Double",
                isNullable = true
            ),
            testCase(parameterName = "Name", dataTypeName = "Float"),
            testCase(
                parameterName = "Name",
                dataTypeName = "Int",
                isNullable = true
            ),
            testCase(parameterName = "Name", dataTypeName = "Integer"),
            testCase(
                parameterName = "Name",
                dataTypeName = "Long",
                isNullable = true
            ),
            testCase(parameterName = "Name", dataTypeName = "Short"),
            testCase(
                parameterName = "Name",
                dataTypeName = "String",
                isNullable = true
            ),
            testCase(parameterName = "Name", dataTypeName = "Array"),
            testCase(
                parameterName = "Name",
                dataTypeName = "List",
                isNullable = true
            ),
            testCase(parameterName = "Name", dataTypeName = "MutableList"),
            testCase(
                parameterName = "Name",
                dataTypeName = "Map",
                isNullable = true
            ),
            testCase(parameterName = "Name", dataTypeName = "MutableMap"),
            testCase(
                parameterName = "Name",
                dataTypeName = "Set",
                isNullable = true
            ),
            testCase(parameterName = "Name", dataTypeName = "MutableSet"),
            testCase(
                parameterName = "Name",
                dataTypeName = "Unit",
                isNullable = true
            ),
            testCase(
                parameterName = "Name",
                dataTypeName = "Unknown",
                expected = true
            ),
            arrayOf(TypedParameter("Name", DataType.Lambda("Lambda", true)), true),
            arrayOf(TypedParameter("Name", DataType.Generic("Generic", false)), true)
        )

        private fun testCase(
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
