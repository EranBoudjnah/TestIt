package com.mitteloupe.testit.generator

import com.mitteloupe.testit.config.model.ExceptionCaptureMethod
import com.mitteloupe.testit.generator.formatting.Formatting
import com.mitteloupe.testit.generator.mapper.DateTypeToParameterMapper
import com.mitteloupe.testit.generator.mocking.MockerCodeGenerator
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.StaticFunctionsMetadata
import com.mitteloupe.testit.model.TypedParameter
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

private const val PACKAGE_NAME = "com.test.it"
private const val TEST_CLASS_NAME = "TestClass"
private const val CLASS_UNDER_TEST_VARIABLE_NAME = "cut"
private const val ACTUAL_VALUE_VARIABLE_NAME = "actualTest"
private const val DEFAULT_ASSERTION_STATEMENT = "defaultAssertion"
private val EXCEPTION_CAPTURE_METHOD = ExceptionCaptureMethod.NO_CAPTURE

@RunWith(MockitoJUnitRunner::class)
class TestStringBuilderTest {
    private lateinit var cut: TestStringBuilder

    private lateinit var stringBuilder: StringBuilder

    @Mock
    lateinit var formatting: Formatting

    @Mock
    lateinit var mockerCodeGenerator: MockerCodeGenerator

    @Mock
    lateinit var dateTypeToParameterMapper: DateTypeToParameterMapper

    @Before
    fun setUp() {
        stringBuilder = spy(StringBuilder())

        given { formatting.getIndentation(1) }
            .willReturn("__")
        given { formatting.getIndentation(2) }
            .willReturn("____")
        given { formatting.getIndentation(3) }
            .willReturn("______")

        cut = TestStringBuilder(
            stringBuilder,
            formatting,
            mockerCodeGenerator,
            CLASS_UNDER_TEST_VARIABLE_NAME,
            ACTUAL_VALUE_VARIABLE_NAME,
            DEFAULT_ASSERTION_STATEMENT,
            EXCEPTION_CAPTURE_METHOD,
            dateTypeToParameterMapper
        )
    }

    @Test
    fun `Given minimal class data when appendTestClass then returns expected output`() {
        // Given
        val config = givenTestStringBuilderConfiguration()

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "class ${TEST_CLASS_NAME}Test {\n" +
                    "__private lateinit var $CLASS_UNDER_TEST_VARIABLE_NAME: $TEST_CLASS_NAME\n" +
                    "\n" +
                    "__@Before\n" +
                    "__fun setUp() {\n" +
                    "____$CLASS_UNDER_TEST_VARIABLE_NAME = $TEST_CLASS_NAME()\n" +
                    "__}\n" +
                    "\n" +
                    "}\n",
            outputString
        )
    }

    @Test
    fun `Given abstract class data when appendTestClass then returns expected output`() {
        // Given
        val config = givenTestStringBuilderConfiguration(
            isAbstract = true
        )
        val givenAbstractCode = "AbstractCode()"
        given { mockerCodeGenerator.getAbstractClassUnderTest(config.classUnderTest) }
            .willReturn(givenAbstractCode)

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "class ${TEST_CLASS_NAME}Test {\n" +
                    "__private lateinit var $CLASS_UNDER_TEST_VARIABLE_NAME: $TEST_CLASS_NAME\n" +
                    "\n" +
                    "__@Before\n" +
                    "__fun setUp() {\n" +
                    "____$CLASS_UNDER_TEST_VARIABLE_NAME = $givenAbstractCode\n" +
                    "__}\n" +
                    "\n" +
                    "}\n",
            outputString
        )
    }

    @Test
    fun `Given class data with imports when appendTestClass then returns expected output`() {
        // Given
        val givenImport1 = "com.path.to.Class1"
        val givenImport2 = "com.path.to.Class2"
        val config = givenTestStringBuilderConfiguration(
            usedImports = setOf(givenImport1, givenImport2)
        )

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "import $givenImport1\n" +
                    "import $givenImport2\n" +
                    "\n" +
                    "class ${TEST_CLASS_NAME}Test {\n" +
                    "__private lateinit var cut: $TEST_CLASS_NAME\n" +
                    "\n" +
                    "__@Before\n" +
                    "__fun setUp() {\n" +
                    "____cut = $TEST_CLASS_NAME()\n" +
                    "__}\n" +
                    "\n" +
                    "}\n",
            outputString
        )
    }

    @Test
    fun `Given class data with constructor parameters when appendTestClass then returns expected output`() {
        // Given
        val givenParameterName1 = "paramName1"
        val givenParameter1 = TypedParameter(givenParameterName1, mock())
        val givenParameterName2 = "paramName2"
        val givenParameter2 = TypedParameter(givenParameterName2, mock())
        val config = givenTestStringBuilderConfiguration(
            constructorParameters = listOf(givenParameter1, givenParameter2)
        )
        val codeForParameter1 = "__private lateinit var mockParam1()"
        given { mockerCodeGenerator.getMockedVariableDefinition(givenParameter1) }
            .willReturn(codeForParameter1)

        val codeForParameter2 = "__private lateinit var mockParam2()"
        given { mockerCodeGenerator.getMockedVariableDefinition(givenParameter2) }
            .willReturn(codeForParameter2)

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "class ${TEST_CLASS_NAME}Test {\n" +
                    "__private lateinit var cut: $TEST_CLASS_NAME\n" +
                    "\n" +
                    "$codeForParameter1\n" +
                    "\n" +
                    "$codeForParameter2\n" +
                    "\n" +
                    "__@Before\n" +
                    "__fun setUp() {\n" +
                    "____cut = $TEST_CLASS_NAME($givenParameterName1, $givenParameterName2)\n" +
                    "__}\n" +
                    "\n" +
                    "}\n",
            outputString
        )
    }

    @Test
    fun `Given class data with functions when appendTestClass then returns expected output`() {
        // Given
        val functionMetadata1 =
            FunctionMetadata("function1", false, listOf(), null, DataType.Specific("DataType1"))

        val functionMetadata2 =
            FunctionMetadata("function2", true, listOf(), null, DataType.Specific("DataType2"))

        val extensionReceiverType = DataType.Specific("ReceiverDataType")
        val functionMetadata3 = FunctionMetadata(
            "function3",
            false,
            listOf(),
            extensionReceiverType,
            DataType.Specific("DataType3")
        )
        val mockedReceiverType = "mock<ReceiverDataType>()"
        given {
            mockerCodeGenerator.getMockedValue(
                extensionReceiverType.name,
                extensionReceiverType
            )
        }.willReturn(mockedReceiverType)

        val functionMetadata4 =
            FunctionMetadata("function4", false, listOf(), null, DataType.Specific("Unit"))

        val functionParameter1 = TypedParameter("functionParameterName1", mock())
        val functionParameter2 = TypedParameter("functionParameterName2", mock())
        val functionMetadata5 =
            FunctionMetadata(
                "function5",
                false,
                listOf(functionParameter1, functionParameter2),
                null,
                DataType.Specific("DataType5")
            )
        val mockedValue1 = "\"Some value 1\""
        given {
            mockerCodeGenerator.getMockedValue(
                functionParameter1.name,
                functionParameter1.type
            )
        }.willReturn(mockedValue1)
        val mockedValue2 = "\"Some value 2\""
        given {
            mockerCodeGenerator.getMockedValue(
                functionParameter2.name,
                functionParameter2.type
            )
        }.willReturn(mockedValue2)

        val config = givenTestStringBuilderConfiguration(
            functions = listOf(
                functionMetadata1,
                functionMetadata2,
                functionMetadata3,
                functionMetadata4,
                functionMetadata5
            )
        )

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "class ${TEST_CLASS_NAME}Test {\n" +
                    "__private lateinit var cut: $TEST_CLASS_NAME\n" +
                    "\n" +
                    "__@Before\n" +
                    "__fun setUp() {\n" +
                    "____cut = $TEST_CLASS_NAME()\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${functionMetadata1.name} then _`() {\n" +
                    "____// Given\n" +
                    "\n" +
                    "____// When\n" +
                    "____val $ACTUAL_VALUE_VARIABLE_NAME = $CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata1.name}()\n" +
                    "\n" +
                    "____// Then\n" +
                    "____$DEFAULT_ASSERTION_STATEMENT\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${extensionReceiverType.name}#${functionMetadata3.name} then _`() {\n" +
                    "____// Given\n" +
                    "____val receiver = mock<${extensionReceiverType.name}>()\n" +
                    "\n" +
                    "____// When\n" +
                    "____val $ACTUAL_VALUE_VARIABLE_NAME = with(cut) {\n" +
                    "______receiver.${functionMetadata3.name}()\n" +
                    "____}\n" +
                    "\n" +
                    "____// Then\n" +
                    "____$DEFAULT_ASSERTION_STATEMENT\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${functionMetadata4.name} then _`() {\n" +
                    "____// Given\n" +
                    "\n" +
                    "____// When\n" +
                    "____$CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata4.name}()\n" +
                    "\n" +
                    "____// Then\n" +
                    "____$DEFAULT_ASSERTION_STATEMENT\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${functionMetadata5.name} then _`() {\n" +
                    "____// Given\n" +
                    "____val ${functionParameter1.name} = $mockedValue1\n" +
                    "____val ${functionParameter2.name} = $mockedValue2\n" +
                    "\n" +
                    "____// When\n" +
                    "____val $ACTUAL_VALUE_VARIABLE_NAME = $CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata5.name}(${functionParameter1.name}, ${functionParameter2.name})\n" +
                    "\n" +
                    "____// Then\n" +
                    "____$DEFAULT_ASSERTION_STATEMENT\n" +
                    "__}\n" +
                    "}\n",
            outputString
        )
    }

    @Test
    fun `Given class data with function and annotation exception when appendTestClass then returns expected output`() {
        // Given
        val functionMetadata1 =
            FunctionMetadata("function1", false, listOf(), null, DataType.Specific("DataType1"))
        val config = givenTestStringBuilderConfiguration(
            functions = listOf(
                functionMetadata1
            )
        )

        val cut = TestStringBuilder(
            stringBuilder,
            formatting,
            mockerCodeGenerator,
            CLASS_UNDER_TEST_VARIABLE_NAME,
            ACTUAL_VALUE_VARIABLE_NAME,
            DEFAULT_ASSERTION_STATEMENT,
            ExceptionCaptureMethod.ANNOTATION_EXPECTS,
            dateTypeToParameterMapper
        )

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "class ${TEST_CLASS_NAME}Test {\n" +
                    "__private lateinit var cut: $TEST_CLASS_NAME\n" +
                    "\n" +
                    "__@Before\n" +
                    "__fun setUp() {\n" +
                    "____cut = $TEST_CLASS_NAME()\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${functionMetadata1.name} then _`() {\n" +
                    "____// Given\n" +
                    "\n" +
                    "____// When\n" +
                    "____val $ACTUAL_VALUE_VARIABLE_NAME = $CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata1.name}()\n" +
                    "\n" +
                    "____// Then\n" +
                    "____$DEFAULT_ASSERTION_STATEMENT\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test(expected = Exception::class)\n" +
                    "__fun `Given _ when ${functionMetadata1.name} then throws exception`() {\n" +
                    "____// Given\n" +
                    "\n" +
                    "____// When\n" +
                    "____$CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata1.name}()\n" +
                    "\n" +
                    "____// Then\n" +
                    "____// Exception is thrown\n" +
                    "__}\n" +
                    "}\n",
            outputString
        )
    }

    @Test
    fun `Given class data with function and try-catch exception when appendTestClass then returns expected output`() {
        // Given
        val functionMetadata1 =
            FunctionMetadata("function1", false, listOf(), null, DataType.Specific("DataType1"))
        val config = givenTestStringBuilderConfiguration(
            functions = listOf(
                functionMetadata1
            )
        )

        val cut = TestStringBuilder(
            stringBuilder,
            formatting,
            mockerCodeGenerator,
            CLASS_UNDER_TEST_VARIABLE_NAME,
            ACTUAL_VALUE_VARIABLE_NAME,
            DEFAULT_ASSERTION_STATEMENT,
            ExceptionCaptureMethod.TRY_CATCH,
            dateTypeToParameterMapper
        )

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "class ${TEST_CLASS_NAME}Test {\n" +
                    "__private lateinit var cut: $TEST_CLASS_NAME\n" +
                    "\n" +
                    "__@Before\n" +
                    "__fun setUp() {\n" +
                    "____cut = $TEST_CLASS_NAME()\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${functionMetadata1.name} then _`() {\n" +
                    "____// Given\n" +
                    "\n" +
                    "____// When\n" +
                    "____val $ACTUAL_VALUE_VARIABLE_NAME = $CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata1.name}()\n" +
                    "\n" +
                    "____// Then\n" +
                    "____$DEFAULT_ASSERTION_STATEMENT\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${functionMetadata1.name} then throws exception`() {\n" +
                    "____// Given\n" +
                    "____val expectedException = Exception()\n" +
                    "____lateinit var actualException: Exception\n" +
                    "\n" +
                    "____// When\n" +
                    "____try {\n" +
                    "______$CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata1.name}()\n" +
                    "____} catch (exception: Exception) {\n" +
                    "______actualException = exception\n" +
                    "____}\n" +
                    "\n" +
                    "____// Then\n" +
                    "____assertEquals(expectedException, actualException)\n" +
                    "__}\n" +
                    "}\n",
            outputString
        )
    }

    @Test
    fun `Given class data with mockable constructor parameters when appendTestClass then returns expected output`() {
        // Given
        val config = givenTestStringBuilderConfiguration(hasMockableConstructorParameters = true)
        val givenAnnotation = "@Annotation"
        given { mockerCodeGenerator.testClassBaseRunnerAnnotation }.willReturn(givenAnnotation)

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "$givenAnnotation\n" +
                    "class ${TEST_CLASS_NAME}Test {\n" +
                    "__private lateinit var cut: $TEST_CLASS_NAME\n" +
                    "\n" +
                    "__@Before\n" +
                    "__fun setUp() {\n" +
                    "____$CLASS_UNDER_TEST_VARIABLE_NAME = $TEST_CLASS_NAME()\n" +
                    "__}\n" +
                    "\n" +
                    "}\n",
            outputString
        )
    }

    @Test
    fun `Given static functions metadata with imports when appendFunctionsTestClass then returns expected output`() {
        // Given
        val givenImport1 = "com.path.to.Class1"
        val givenImport2 = "com.path.to.Class2"
        val functionsUnderTest = StaticFunctionsMetadata(
            PACKAGE_NAME, mapOf(), listOf()
        )
        val usedImports = setOf(givenImport1, givenImport2)
        val outputClassName = "outputClassName"
        val isParameterized = false

        // When
        val actualValue = cut.appendFunctionsTestClass(
            functionsUnderTest,
            usedImports,
            outputClassName,
            isParameterized
        )

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "import $givenImport1\n" +
                    "import $givenImport2\n" +
                    "\n" +
                    "class ${outputClassName}Test {\n" +
                    "}\n",
            outputString
        )
    }

    @Test
    fun `Given parameterized test with static functions metadata with imports when appendFunctionsTestClass then returns expected output`() {
        // Given
        val givenImport1 = "com.path.to.Class1"
        val givenImport2 = "com.path.to.Class2"
        val functionsUnderTest = StaticFunctionsMetadata(
            PACKAGE_NAME, mapOf(), listOf()
        )
        val usedImports = setOf(givenImport1, givenImport2)
        val outputClassName = "outputClassName"
        val isParameterized = true
        given { mockerCodeGenerator.testClassParameterizedRunnerAnnotation }
            .willReturn("@RunWith(Parameterized::class)")

        // When
        val actualValue = cut.appendFunctionsTestClass(
            functionsUnderTest, usedImports, outputClassName,
            isParameterized
        )

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "import $givenImport1\n" +
                    "import $givenImport2\n" +
                    "\n" +
                    "@RunWith(Parameterized::class)\n" +
                    "class ${outputClassName}Test {\n" +
                    "}\n",
            outputString
        )
    }

    @Test
    fun `Given static functions metadata when appendFunctionsTestClass then returns expected output`() {
        // Given
        val functionMetadata1 =
            FunctionMetadata("function1", false, listOf(), null, DataType.Specific("DataType1"))
        val functionMetadata2 =
            FunctionMetadata("function2", true, listOf(), null, DataType.Specific("DataType2"))
        val extensionReceiverType = DataType.Specific("ReceiverDataType")
        val functionMetadata3 =
            FunctionMetadata(
                "function3",
                false,
                listOf(),
                extensionReceiverType,
                DataType.Specific("DataType3")
            )
        val mockedReceiverType = "mock<ReceiverDataType>()"
        given {
            mockerCodeGenerator.getMockedValue(
                extensionReceiverType.name,
                extensionReceiverType
            )
        }
            .willReturn(mockedReceiverType)
        val functionMetadata4 =
            FunctionMetadata("function4", false, listOf(), null, DataType.Specific("Unit"))
        val functionParameter1 = TypedParameter("functionParameterName1", mock())
        val functionParameter2 = TypedParameter("functionParameterName2", mock())
        val functionMetadata5 =
            FunctionMetadata(
                "function5",
                false,
                listOf(functionParameter1, functionParameter2),
                null,
                DataType.Specific("DataType5")
            )
        val mockedValue1 = "\"Some value 1\""
        given {
            mockerCodeGenerator.getMockedValue(
                functionParameter1.name,
                functionParameter1.type
            )
        }
            .willReturn(mockedValue1)
        val mockedValue2 = "\"Some value 2\""
        given {
            mockerCodeGenerator.getMockedValue(
                functionParameter2.name,
                functionParameter2.type
            )
        }
            .willReturn(mockedValue2)
        val functionsUnderTest = StaticFunctionsMetadata(
            PACKAGE_NAME, mapOf(), listOf(
                functionMetadata1,
                functionMetadata2,
                functionMetadata3,
                functionMetadata4,
                functionMetadata5
            )
        )
        val usedImports = setOf<String>()
        val outputClassName = "outputClassName"

        // When
        val actualValue =
            cut.appendFunctionsTestClass(functionsUnderTest, usedImports, outputClassName, false)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                    "\n" +
                    "class ${outputClassName}Test {\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${functionMetadata1.name} then _`() {\n" +
                    "____// Given\n" +
                    "\n" +
                    "____// When\n" +
                    "____val $ACTUAL_VALUE_VARIABLE_NAME = ${functionMetadata1.name}()\n" +
                    "\n" +
                    "____// Then\n" +
                    "____$DEFAULT_ASSERTION_STATEMENT\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${extensionReceiverType.name}#${functionMetadata3.name} then _`() {\n" +
                    "____// Given\n" +
                    "____val receiver = mock<${extensionReceiverType.name}>()\n" +
                    "\n" +
                    "____// When\n" +
                    "____val $ACTUAL_VALUE_VARIABLE_NAME = receiver.${functionMetadata3.name}()\n" +
                    "\n" +
                    "____// Then\n" +
                    "____$DEFAULT_ASSERTION_STATEMENT\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${functionMetadata4.name} then _`() {\n" +
                    "____// Given\n" +
                    "\n" +
                    "____// When\n" +
                    "____${functionMetadata4.name}()\n" +
                    "\n" +
                    "____// Then\n" +
                    "____$DEFAULT_ASSERTION_STATEMENT\n" +
                    "__}\n" +
                    "\n" +
                    "__@Test\n" +
                    "__fun `Given _ when ${functionMetadata5.name} then _`() {\n" +
                    "____// Given\n" +
                    "____val ${functionParameter1.name} = $mockedValue1\n" +
                    "____val ${functionParameter2.name} = $mockedValue2\n" +
                    "\n" +
                    "____// When\n" +
                    "____val $ACTUAL_VALUE_VARIABLE_NAME = ${functionMetadata5.name}(${functionParameter1.name}, ${functionParameter2.name})\n" +
                    "\n" +
                    "____// Then\n" +
                    "____$DEFAULT_ASSERTION_STATEMENT\n" +
                    "__}\n" +
                    "}\n", outputString
        )
    }

    @Test
    fun `When clear then resets stringBuilder and mockerCodeGenerator`() {
        // When
        cut.clear()

        // Then
        verify(stringBuilder).clear()
        verifyNoMoreInteractions(stringBuilder)
        verify(mockerCodeGenerator).reset()
        verifyNoMoreInteractions(mockerCodeGenerator)
    }

    private fun givenTestStringBuilderConfiguration(
        isAbstract: Boolean = false,
        constructorParameters: List<TypedParameter> = listOf(),
        functions: List<FunctionMetadata> = listOf(),
        usedImports: Set<String> = setOf(),
        hasMockableConstructorParameters: Boolean = false,
        isParameterized: Boolean = false
    ) = TestStringBuilderConfiguration(
        classUnderTest = ClassMetadata(
            PACKAGE_NAME,
            mapOf(),
            TEST_CLASS_NAME,
            isAbstract,
            constructorParameters,
            functions
        ),
        usedImports = usedImports,
        hasMockableConstructorParameters = hasMockableConstructorParameters,
        isParameterized = isParameterized
    )
}
