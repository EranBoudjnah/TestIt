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
        fun data(): Collection<Array<*>> = listOf(
            arrayOf(DataType.Specific("Boolean", false), "Boolean", "false", false),
            arrayOf(DataType.Specific("Byte", true), "Byte", "0b0", false),
            arrayOf(DataType.Specific("Class", false), "Class", "Any::class.java", false),
            arrayOf(DataType.Specific("Double", true), "Double", "0.0", false),
            arrayOf(DataType.Specific("Float", false), "Float", "0f", false),
            arrayOf(DataType.Specific("Int", true), "Int", "0", false),
            arrayOf(DataType.Specific("Integer", false), "Integer", "0 as Integer", false),
            arrayOf(DataType.Specific("Long", true), "Long", "0L", false),
            arrayOf(DataType.Specific("Short", false), "Short", "0.toShort()", false),
            arrayOf(DataType.Specific("String", true), "String", "\"Test\"", false),
            arrayOf(DataType.Specific("Array", false), "Array", "arrayOf<Any>()", false),
            arrayOf(DataType.Specific("List", true), "List", "listOf<Any>()", false),
            arrayOf(DataType.Specific("MutableList", false), "MutableList", "mutableListOf<Any>()", false),
            arrayOf(DataType.Specific("Map", true), "Map", "mapOf<Any>()", false),
            arrayOf(DataType.Specific("MutableMap", false), "MutableMap", "mutableMapOf<Any>()", false),
            arrayOf(DataType.Specific("Set", true), "Set", "setOf<Any>()", false),
            arrayOf(DataType.Specific("MutableSet", false), "MutableSet", "mutableSetOf<Any>()", false),
            arrayOf(DataType.Specific("Unit", true), "Unit", "Unit", false),
            arrayOf(DataType.Specific("CustomDataType", false), "CustomDataType", "CustomDataType()", true),
            arrayOf(DataType.Lambda("Lambda", true, dataType("a"), dataType("b")), "(?)->?", "{ a, b -> }", true),
            arrayOf(DataType.Generic("Manager", false, dataType("Employee")), "Manager", "Manager<Employee>()", true)
        )

        private fun dataType(name: String) = DataType.Specific(name, false)
    }

    private lateinit var cut: MockableTypeQualifier

    @Before
    fun setUp() {
        cut = MockableTypeQualifier()
    }

    @Test
    fun `When getNonMockableType then returns expected type, default value`() {
        // Given

        // When
        val actualValue = cut.getNonMockableType(givenDataType)
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
        val actualValue = cut.isMockable(givenDataType)

        // Then
        assertEquals(isMockableExpected2, actualValue)
    }
}
