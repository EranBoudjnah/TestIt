package com.mitteloupe.testit.generator.mocking

import com.mitteloupe.testit.model.DataType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class MockableTypeQualifierDataTypeTest(
    private val givenDataType: DataType,
    private val expectedType: String?,
    private val expectedDefaultValue: String?,
    private val isMockableExpected2: Boolean
) {
    companion object {
        @JvmStatic
        @Parameters(name = "Given {0}")
        fun data(): Iterable<Array<*>> = setOf(
            testCase(name = "Boolean", defaultValue = "false"),
            testCase(name = "Byte", defaultValue = "0b0", isNullable = true),
            testCase(name = "Class", defaultValue = "Any::class.java"),
            testCase(name = "Double", defaultValue = "0.0", isNullable = true),
            testCase(name = "Float", defaultValue = "0f"),
            testCase(name = "Int", defaultValue = "0", isNullable = true),
            testCase(name = "Integer", defaultValue = "0 as Integer"),
            testCase(name = "Long", defaultValue = "0L", isNullable = true),
            testCase(name = "Short", defaultValue = "0.toShort()"),
            testCase(name = "String", defaultValue = "\"Test\"", isNullable = true),
            testCase(name = "Array", defaultValue = "arrayOf<Any>()"),
            testCase(name = "List", defaultValue = "listOf<Any>()", isNullable = true),
            testCase(
                name = "MutableList",
                defaultValue = "mutableListOf<Any>()",
                isNullable = false,
                isMockable = false
            ),
            testCase(
                name = "Map",
                defaultValue = "mapOf<Any>()",
                isNullable = true
            ),
            testCase(
                name = "MutableMap",
                defaultValue = "mutableMapOf<Any>()"
            ),
            testCase(
                name = "Set",
                defaultValue = "setOf<Any>()",
                isNullable = true
            ),
            testCase(
                name = "MutableSet",
                defaultValue = "mutableSetOf<Any>()"
            ),
            testCase(
                name = "Unit",
                defaultValue = "Unit",
                isNullable = true
            ),
            testCase(
                name = "CustomDataType",
                defaultValue = null,
                isNullable = false,
                isMockable = true,
                expectedType = null
            ),
            arrayOf(
                DataType.Lambda("Lambda", true, dataType("a"), dataType("b")),
                "(?)->?",
                "{ a, b -> }",
                true
            ),
            arrayOf(DataType.Generic("Manager", false, dataType("Employee")), null, null, true)
        )

        private fun testCase(
            name: String,
            defaultValue: String?,
            isNullable: Boolean = false,
            isMockable: Boolean = false,
            expectedType: String? = name
        ) = arrayOf(DataType.Specific(name, isNullable), expectedType, defaultValue, isMockable)

        private fun dataType(name: String) = DataType.Specific(name, false)
    }

    private lateinit var classUnderTest: MockableTypeQualifier

    @Before
    fun setUp() {
        classUnderTest = MockableTypeQualifier()
    }

    @Test
    fun `When getNonMockableType then returns expected type, default value`() {
        // Given

        // When
        val actualValue = classUnderTest.getNonMockableType(givenDataType)
        val actualDataType = actualValue?.dataType
        val actualDefaultValue = actualValue?.defaultValue?.invoke("Test", givenDataType)

        // Then
        assertEquals(expectedType, actualDataType)
        assertEquals(expectedDefaultValue, actualDefaultValue)
    }

    @Test
    fun `When isMockable(DataType) then returns expected mockable state`() {
        // Given

        // When
        val actualValue = classUnderTest.isMockable(givenDataType)

        // Then
        assertEquals(isMockableExpected2, actualValue)
    }
}
