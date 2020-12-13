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
    private val isMockableTypedParameter: TypedParameter,
    private val isMockableExpected: Boolean,
) {
    companion object {
        @JvmStatic
        @Parameters(name = "Given {0}")
        fun data(): Collection<Array<*>> = listOf(
            arrayOf(TypedParameter("Name", DataType.Specific("Boolean", false)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Byte", true)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Class", false)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Double", true)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Float", false)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Int", true)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Integer", false)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Long", true)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Short", false)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("String", true)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Array", false)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("List", true)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("MutableList", false)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Map", true)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("MutableMap", false)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Set", true)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("MutableSet", false)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Unit", true)), false),
            arrayOf(TypedParameter("Name", DataType.Specific("Unknown", false)), true),
            arrayOf(TypedParameter("Name", DataType.Lambda("Lambda", true)), true),
            arrayOf(TypedParameter("Name", DataType.Generic("Generic", false)), true)
        )
    }

    private lateinit var cut: MockableTypeQualifier

    @Before
    fun setUp() {
        cut = MockableTypeQualifier()
    }

    @Test
    fun `When isMockable(TypedParameter) then return expected mockable state`() {
        // Given

        // When
        val actualValue = cut.isMockable(isMockableTypedParameter)

        // Then
        assertEquals(isMockableExpected, actualValue)
    }
}
