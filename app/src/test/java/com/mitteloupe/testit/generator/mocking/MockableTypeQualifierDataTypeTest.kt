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
            specificTestCase(name = "Boolean", defaultValue = "false"),
            specificTestCase(name = "Byte", defaultValue = "0b0", isNullable = true),
            specificTestCase(name = "Class", defaultValue = "Any::class.java"),
            specificTestCase(name = "Double", defaultValue = "0.0", isNullable = true),
            specificTestCase(name = "Float", defaultValue = "0f"),
            specificTestCase(name = "Int", defaultValue = "0", isNullable = true),
            specificTestCase(name = "Integer", defaultValue = "0 as Integer"),
            specificTestCase(name = "Long", defaultValue = "0L", isNullable = true),
            specificTestCase(name = "Short", defaultValue = "0.toShort()"),
            specificTestCase(name = "String", defaultValue = "\"Test\"", isNullable = true),
            specificTestCase(name = "Array", defaultValue = "arrayOf<Any>()"),
            specificTestCase(name = "List", defaultValue = "listOf<Any>()", isNullable = true),
            specificTestCase(
                name = "MutableList",
                defaultValue = "mutableListOf<Any>()",
                isNullable = false,
                isMockable = false
            ),
            specificTestCase(
                name = "Map",
                defaultValue = "mapOf<Any>()",
                isNullable = true
            ),
            specificTestCase(
                name = "MutableMap",
                defaultValue = "mutableMapOf<Any>()"
            ),
            specificTestCase(
                name = "Set",
                defaultValue = "setOf<Any>()",
                isNullable = true
            ),
            specificTestCase(
                name = "MutableSet",
                defaultValue = "mutableSetOf<Any>()"
            ),
            specificTestCase(
                name = "Unit",
                defaultValue = "Unit",
                isNullable = true
            ),
            specificTestCase(
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

        private fun specificTestCase(
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
