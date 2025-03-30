package com.mitteloupe.testit.generator.mocking

import com.mitteloupe.testit.generator.formatting.Formatting
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.TypedParameter
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.given
import org.mockito.kotlin.mock

private const val MOCKITO_RULE_VARIABLE_NAME = "testMockitoRule"

@RunWith(MockitoJUnitRunner::class)
class MockitoCodeGeneratorTest {
    private lateinit var classUnderTest: MockitoCodeGenerator

    @Mock
    lateinit var mockableTypeQualifier: MockableTypeQualifier

    @Mock
    lateinit var formatting: Formatting

    @Before
    fun setUp() {
        given { formatting.getIndentation(1) }.willReturn("_")

        classUnderTest =
            MockitoCodeGenerator(mockableTypeQualifier, formatting, MOCKITO_RULE_VARIABLE_NAME)
    }

    @Test
    fun `Given imports were required when reset then imports are empty`() {
        // Given
        classUnderTest.setHasMockedFunctionParameters()

        // When
        classUnderTest.reset()

        // Then
        assertTrue(classUnderTest.requiredImports.isEmpty())
    }

    @Test
    fun `Given parameter when getConstructorMock then returns expected mocking code`() {
        // Given
        val parameterName = "parameterName"
        val parameterType = DataType.Specific("testing", false)
        val expected = "_@Mock\n" +
            "_private lateinit var parameterName: testing"

        // When
        val actualValue = classUnderTest.getConstructorMock(parameterName, parameterType)

        // Then
        assertEquals(expected, actualValue)
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `Given class without constructor parameters when getAbstractClassUnderTest then returns expected code`() {
        // Given
        val classUnderTest = mockClassMetadata()
        val expected = "mock(defaultAnswer = Mockito.CALLS_REAL_METHODS)"

        // When
        val actualValue = this.classUnderTest.getAbstractClassUnderTest(classUnderTest)

        // Then
        assertEquals(expected, actualValue)
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `Given class with constructor parameters when getAbstractClassUnderTest then returns expected code`() {
        // Given
        val classUnderTest = mockClassMetadata(
            constructorParameters = listOf(
                TypedParameter("param1", DataType.Specific("type1", false)),
                TypedParameter("param2", DataType.Generic("type2", false))
            )
        )
        val expected = "mock(" +
            "defaultAnswer = Mockito.CALLS_REAL_METHODS, " +
            "useConstructor = UseConstructor.withArguments(param1, param2)" +
            ")"

        // When
        val actualValue = this.classUnderTest.getAbstractClassUnderTest(classUnderTest)

        // Then
        assertEquals(expected, actualValue)
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `Given no mocked constructor parameters when setIsParameterizedTest then imports not added`() {
        // Given
        val expected1 = "Rule"
        val expected2 = "MethodRule"
        val expected3 = "MockitoJUnit"

        // When
        classUnderTest.setIsParameterizedTest()
        val actualValue = classUnderTest.requiredImports

        // Then
        assertThat(actualValue, not(hasItems(expected1, expected2, expected3)))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `Given mocked constructor parameters when setIsParameterizedTest then adds expected imports`() {
        // Given
        val expected1 = "Rule"
        val expected2 = "MethodRule"
        val expected3 = "MockitoJUnit"
        classUnderTest.setHasMockedConstructorParameters(mock())

        // When
        classUnderTest.setIsParameterizedTest()
        val actualValue = classUnderTest.requiredImports

        // Then
        assertThat(actualValue, hasItems(expected1, expected2, expected3))
    }

    @Test
    fun `Given concrete class when setHasMockedConstructorParameters then adds expected imports`() {
        // Given
        val expected1 = "RunWith"
        val expected2 = "MockitoJUnitRunner"
        val expected3 = "Mock"
        val classUnderTest = mockClassMetadata()

        // When
        this.classUnderTest.setHasMockedConstructorParameters(classUnderTest)
        val actualValue = this.classUnderTest.requiredImports

        // Then
        assertEquals(actualValue, setOf(expected1, expected2, expected3))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `Given abstract class without constructor parameters when setHasMockedConstructorParameters then adds expected imports`() {
        // Given
        val expected1 = "RunWith"
        val expected2 = "MockitoJUnitRunner"
        val expected3 = "Mock"
        val classUnderTest = mockClassMetadata(
            isAbstract = true
        )

        // When
        this.classUnderTest.setHasMockedConstructorParameters(classUnderTest)
        val actualValue = this.classUnderTest.requiredImports

        // Then
        assertEquals(actualValue, setOf(expected1, expected2, expected3))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `Given abstract class with constructor parameters when setHasMockedConstructorParameters then adds expected imports`() {
        // Given
        val expected1 = "RunWith"
        val expected2 = "MockitoJUnitRunner"
        val expected3 = "Mock"
        val expected4 = "UseConstructor"
        val classUnderTest = mockClassMetadata(
            isAbstract = true,
            constructorParameters = listOf(mock())
        )

        // When
        this.classUnderTest.setHasMockedConstructorParameters(classUnderTest)
        val actualValue = this.classUnderTest.requiredImports

        // Then
        assertEquals(actualValue, setOf(expected1, expected2, expected3, expected4))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `Given parameterized test when setHasMockedConstructorParameters then adds expected imports`() {
        // Given
        val expected1 = "RunWith"
        val expected2 = "MockitoJUnitRunner"
        val expected3 = "Mock"
        val expected4 = "Rule"
        val expected5 = "MethodRule"
        val expected6 = "MockitoJUnit"
        val classUnderTest = mockClassMetadata()
        this.classUnderTest.setIsParameterizedTest()

        // When
        this.classUnderTest.setHasMockedConstructorParameters(classUnderTest)
        val actualValue = this.classUnderTest.requiredImports

        // Then
        assertEquals(
            actualValue,
            setOf(
                expected1,
                expected2,
                expected3,
                expected4,
                expected5,
                expected6
            )
        )
    }

    @Test
    fun `When setHasMockedFunctionParameters then added expected import`() {
        // Given
        val expected = "mock"

        // When
        classUnderTest.setHasMockedFunctionParameters()
        val actualValue = classUnderTest.requiredImports

        // Then
        assertEquals(actualValue, setOf(expected))
    }

    @Test
    fun `When setHasMockedFunctionReturnValues then _`() {
        // Given
        val expected = "mock"

        // When
        classUnderTest.setHasMockedFunctionReturnValues()
        val actualValue = classUnderTest.requiredImports

        // Then
        assertEquals(actualValue, setOf(expected))
    }

    @Test
    fun `When setIsAbstractClassUnderTest then adds expected imports`() {
        // Given
        val expected1 = "mock"
        val expected2 = "Mockito"

        // When
        classUnderTest.setIsAbstractClassUnderTest()
        val actualValue = classUnderTest.requiredImports

        // Then
        assertEquals(actualValue, setOf(expected1, expected2))
    }

    @Test
    fun `Given mockable data type when getMockedValue then returns expected value`() {
        // Given
        val variableName = "variableName"
        val variableType = DataType.Specific("mockable data type", false)
        val expected = "mock<mockable data type>()"
        given { mockableTypeQualifier.getNonMockableType(variableType) }
            .willReturn(null)

        // When
        val actualValue = classUnderTest.getMockedValue(variableName, variableType)

        // Then
        assertEquals(expected, actualValue)
    }

    @Test
    fun `Given mockable generic data type when getMockedValue then returns expected value`() {
        // Given
        val variableName = "variableName"
        val variableType = DataType.Generic(
            "data type",
            false,
            DataType.Generic(
                "nested type",
                true,
                DataType.Specific("deeply nested", false)
            ),
            DataType.Specific("another type", false)
        )
        val expected = "mock<data type<nested type<deeply nested>?, another type>>()"
        given { mockableTypeQualifier.getNonMockableType(variableType) }
            .willReturn(null)

        // When
        val actualValue = classUnderTest.getMockedValue(variableName, variableType)

        // Then
        assertEquals(expected, actualValue)
    }

    @Test
    fun `When getting setUpStatements then returns null`() {
        // When
        val actualValue = classUnderTest.setUpStatements

        // Then
        assertNull(actualValue)
    }

    @Test
    fun `When getting mockingRule then returns expected code`() {
        // Given
        val expectedValue = "_@get:Rule\n" +
            "_val $MOCKITO_RULE_VARIABLE_NAME: MethodRule = MockitoJUnit.rule()"

        // When
        val actualValue = classUnderTest.mockingRule

        // Then
        assertEquals(expectedValue, actualValue)
    }
}

private fun mockClassMetadata(
    packageName: String = "packageName",
    imports: Map<String, String> = emptyMap(),
    className: String = "className",
    isAbstract: Boolean = false,
    constructorParameters: List<TypedParameter> = emptyList(),
    functions: List<FunctionMetadata> = emptyList()
) = ClassMetadata(packageName, imports, className, isAbstract, constructorParameters, functions)
