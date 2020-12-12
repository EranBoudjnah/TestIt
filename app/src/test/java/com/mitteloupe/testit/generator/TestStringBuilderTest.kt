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
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.After
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
private const val PARAMETERIZED_RUNNER_ANNOTATION = "@Parameterized"
private const val MOCKING_RULE = "__@get:Rule\n__val mockitoRule: MethodRule = MockitoJUnit.rule()"

private val unitDataType = DataType.Specific("Unit", false)

@RunWith(MockitoJUnitRunner::class)
class TestStringBuilderTest {
    private lateinit var cut: TestStringBuilder

    private lateinit var stringBuilder: StringBuilder

    @Mock
    private lateinit var formatting: Formatting

    @Mock
    private lateinit var mockerCodeGenerator: MockerCodeGenerator

    @Mock
    private lateinit var dateTypeToParameterMapper: DateTypeToParameterMapper

    private val outString = mutableListOf<String>()

    @Before
    fun setUp() {
        stringBuilder = mock {
            on { append(any<String>()) }.thenAnswer { invocation ->
                outString.add(invocation.getArgument(0))
                stringBuilder
            }
            on { toString() }.thenAnswer {
                outString.joinToString("")
            }
        }

        (1..3).forEach { indentation ->
            given { formatting.getIndentation(indentation) }
                .willReturn("__" * indentation)
        }

        cut = TestStringBuilder(
            this.stringBuilder,
            formatting,
            mockerCodeGenerator,
            CLASS_UNDER_TEST_VARIABLE_NAME,
            ACTUAL_VALUE_VARIABLE_NAME,
            DEFAULT_ASSERTION_STATEMENT,
            EXCEPTION_CAPTURE_METHOD,
            dateTypeToParameterMapper
        )
    }

    @After
    fun tearDown() {
        outString.clear()
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
    fun `Given parameterized test and minimal class data when appendTestClass then returns expected output`() {
        // Given
        val config = givenTestStringBuilderConfiguration(isParameterized = true)
        given { mockerCodeGenerator.testClassParameterizedRunnerAnnotation }
            .willReturn(PARAMETERIZED_RUNNER_ANNOTATION)

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                "\n" +
                "$PARAMETERIZED_RUNNER_ANNOTATION\n" +
                "class ${TEST_CLASS_NAME}Test {\n" +
                "__companion object {\n" +
                "____@JvmStatic\n" +
                "____@Parameters\n" +
                "____fun data(): Collection<Array<*>> = listOf(\n" +
                "______arrayOf()\n" +
                "____)\n" +
                "__}\n" +
                "\n" +
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
        val givenParameter1 = TypedParameter(givenParameterName1, unitDataType)
        val givenParameterName2 = "paramName2"
        val givenParameter2 = TypedParameter(givenParameterName2, unitDataType)
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
                "__private lateinit var $CLASS_UNDER_TEST_VARIABLE_NAME: $TEST_CLASS_NAME\n" +
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
    fun `Given parameterized test and class data with constructor parameters when appendTestClass then returns expected output`() {
        // Given
        val givenParameterName1 = "paramName1"
        val givenParameter1 =
            TypedParameter(givenParameterName1, DataType.Specific("String", false))
        val givenParameterName2 = "paramName2"
        val givenParameter2 = TypedParameter(
            givenParameterName2,
            DataType.Generic("Array", false, DataType.Specific("Int", false))
        )
        val givenParameterName3 = "paramName3"
        val givenParameter3 = TypedParameter(givenParameterName3, DataType.Lambda("Lambda", false))
        val config = givenTestStringBuilderConfiguration(
            constructorParameters = listOf(givenParameter1, givenParameter2, givenParameter3),
            isParameterized = true
        )
        val codeForParameter1 = "__private lateinit var mockParam1()"
        given { mockerCodeGenerator.getMockedVariableDefinition(givenParameter1) }
            .willReturn(codeForParameter1)

        val codeForParameter2 = "__private lateinit var mockParam2()"
        given { mockerCodeGenerator.getMockedVariableDefinition(givenParameter2) }
            .willReturn(codeForParameter2)

        val codeForParameter3 = "__private lateinit var mockParam3()"
        given { mockerCodeGenerator.getMockedVariableDefinition(givenParameter3) }
            .willReturn(codeForParameter3)

        given { mockerCodeGenerator.testClassParameterizedRunnerAnnotation }
            .willReturn(PARAMETERIZED_RUNNER_ANNOTATION)

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                "\n" +
                "$PARAMETERIZED_RUNNER_ANNOTATION\n" +
                "class ${TEST_CLASS_NAME}Test {\n" +
                "__companion object {\n" +
                "____@JvmStatic\n" +
                "____@Parameters\n" +
                "____fun data(): Collection<Array<*>> = listOf(\n" +
                "______arrayOf()\n" +
                "____)\n" +
                "__}\n" +
                "\n" +
                "__private lateinit var $CLASS_UNDER_TEST_VARIABLE_NAME: $TEST_CLASS_NAME\n" +
                "\n" +
                "$codeForParameter1\n" +
                "\n" +
                "$codeForParameter2\n" +
                "\n" +
                "$codeForParameter3\n" +
                "\n" +
                "__@Before\n" +
                "__fun setUp() {\n" +
                "____cut = $TEST_CLASS_NAME($givenParameterName1, $givenParameterName2, $givenParameterName3)\n" +
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
            FunctionMetadata(
                "function1",
                false,
                emptyList(),
                null,
                DataType.Specific("DataType1", false)
            )

        val functionMetadata2 =
            FunctionMetadata(
                "function2",
                true,
                emptyList(),
                null,
                DataType.Specific("DataType2", false)
            )

        val extensionReceiverType = DataType.Specific("ReceiverDataType", false)
        val functionMetadata3 = FunctionMetadata(
            "function3",
            false,
            emptyList(),
            extensionReceiverType,
            DataType.Specific("DataType3", false)
        )
        val mockedReceiverType = "mock<ReceiverDataType>()"
        givenMockedValue(extensionReceiverType, mockedReceiverType)

        val functionMetadata4 =
            FunctionMetadata("function4", false, emptyList(), null, unitDataType)

        val functionParameter1 = TypedParameter("functionParameterName1", unitDataType)
        val functionParameter2 = TypedParameter("functionParameterName2", unitDataType)
        val functionMetadata5 =
            FunctionMetadata(
                "function5",
                false,
                listOf(functionParameter1, functionParameter2),
                null,
                DataType.Specific("DataType5", false)
            )
        val mockedValue1 = "\"Some value 1\""
        givenMockedValue(functionParameter1, mockedValue1)
        val mockedValue2 = "\"Some value 2\""
        givenMockedValue(functionParameter2, mockedValue2)

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
                "____val receiver = $mockedReceiverType\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = with($CLASS_UNDER_TEST_VARIABLE_NAME) {\n" +
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
    fun `Given parameterized test and class data with functions when appendTestClass then returns expected output`() {
        // Given
        val functionName1 = "function1"
        val function1ReturnDataType = DataType.Specific("DataType1", false)
        val functionMetadata1 =
            FunctionMetadata(
                functionName1,
                false,
                emptyList(),
                null,
                function1ReturnDataType
            )

        val functionName2 = "function2"
        val function2ReturnDataType = DataType.Specific("DataType2", false)
        val functionMetadata2 =
            FunctionMetadata(
                functionName2,
                true,
                emptyList(),
                null,
                function2ReturnDataType
            )

        val extensionReceiverType = DataType.Specific("ReceiverDataType", false)
        val functionName3 = "function3"
        val function3ReturnDataType = DataType.Specific("DataType3", false)
        val functionMetadata3 = FunctionMetadata(
            functionName3,
            false,
            emptyList(),
            extensionReceiverType,
            function3ReturnDataType
        )
        val mockedReceiverType = "mock<ReceiverDataType>()"
        givenMockedValue(extensionReceiverType, mockedReceiverType)

        val functionName4 = "function4"
        val functionMetadata4 =
            FunctionMetadata(functionName4, false, emptyList(), null, unitDataType)

        val functionParameter1DataType = DataType.Specific("Boolean", true)
        val functionParameter1 =
            TypedParameter("functionParameterName1", functionParameter1DataType)
        val functionParameter2DataType =
            DataType.Generic("List", true, DataType.Specific("String", false))
        val functionParameter2 =
            TypedParameter("functionParameterName2", functionParameter2DataType)
        val functionParameter3DataType =
            DataType.Lambda("Unit", false, DataType.Specific("Double", false))
        val functionParameter3 =
            TypedParameter("functionParameterName3", functionParameter3DataType)
        val functionName5 = "function5"
        val function5ReturnDataType = DataType.Specific("DataType5", false)
        val functionMetadata5 =
            FunctionMetadata(
                functionName5,
                false,
                listOf(functionParameter1, functionParameter2, functionParameter3),
                null,
                function5ReturnDataType
            )
        val mockedValue1 = "\"Some value 1\""
        givenMockedValue(functionParameter1, mockedValue1)
        val mockedValue2 = "\"Some value 2\""
        givenMockedValue(functionParameter2, mockedValue2)
        val mockedValue3 = "\"Some value 3\""
        givenMockedValue(functionParameter3, mockedValue3)
        givenMockedValue(TypedParameter("function1Expected", function1ReturnDataType), mockedValue1)
        givenMockedValue(TypedParameter("function2Expected", function2ReturnDataType), mockedValue2)
        givenMockedValue(TypedParameter("function3Expected", function3ReturnDataType), mockedValue3)
        val expectedFunction5Parameter1 = "false"
        givenMockedValue(
            TypedParameter("function5FunctionParameterName1", functionParameter1DataType),
            expectedFunction5Parameter1
        )
        val expectedFunction5Parameter2 = "emptyList<String>()"
        givenMockedValue(
            TypedParameter("function5FunctionParameterName2", functionParameter2DataType),
            expectedFunction5Parameter2
        )
        val expectedFunction5Parameter3 = "{}"
        givenMockedValue(
            TypedParameter("function5FunctionParameterName3", functionParameter3DataType),
            expectedFunction5Parameter3
        )
        val mockedValue5 = "\"Some value 5\""
        givenMockedValue(TypedParameter("function5Expected", function5ReturnDataType), mockedValue5)

        given { mockerCodeGenerator.testClassParameterizedRunnerAnnotation }
            .willReturn(PARAMETERIZED_RUNNER_ANNOTATION)

        val config = givenTestStringBuilderConfiguration(
            functions = listOf(
                functionMetadata1,
                functionMetadata2,
                functionMetadata3,
                functionMetadata4,
                functionMetadata5
            ),
            isParameterized = true
        )

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        val capitalizedParameterName1 = "FunctionParameterName1"
        val capitalizedParameterName2 = "FunctionParameterName2"
        val capitalizedParameterName3 = "FunctionParameterName3"
        assertEquals(
            "package $PACKAGE_NAME\n" +
                "\n" +
                "$PARAMETERIZED_RUNNER_ANNOTATION\n" +
                "class ${TEST_CLASS_NAME}Test(\n" +
                "__private val ${functionName1}Expected: DataType1,\n" +
                "__private val ${functionName2}Expected: DataType2,\n" +
                "__private val ${functionName3}Expected: DataType3,\n" +
                "__private val ${functionName5}$capitalizedParameterName1: Boolean?,\n" +
                "__private val ${functionName5}$capitalizedParameterName2: List<String>?,\n" +
                "__private val ${functionName5}$capitalizedParameterName3: (Double) -> Unit,\n" +
                "__private val ${functionName5}Expected: DataType5\n" +
                ") {\n" +
                "__companion object {\n" +
                "____@JvmStatic\n" +
                "____@Parameters\n" +
                "____fun data(): Collection<Array<*>> = listOf(\n" +
                "______arrayOf($mockedValue1, $mockedValue2, $mockedValue3, $expectedFunction5Parameter1, $expectedFunction5Parameter2, $expectedFunction5Parameter3, $mockedValue5)\n" +
                "____)\n" +
                "__}\n" +
                "\n" +
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
                "____assertEquals(${functionName1}Expected, actualTest)\n" +
                "____$DEFAULT_ASSERTION_STATEMENT\n" +
                "__}\n" +
                "\n" +
                "__@Test\n" +
                "__fun `Given _ when ${extensionReceiverType.name}#${functionMetadata3.name} then _`() {\n" +
                "____// Given\n" +
                "____val receiver = $mockedReceiverType\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = with($CLASS_UNDER_TEST_VARIABLE_NAME) {\n" +
                "______receiver.${functionMetadata3.name}()\n" +
                "____}\n" +
                "\n" +
                "____// Then\n" +
                "____assertEquals(${functionName3}Expected, actualTest)\n" +
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
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = $CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata5.name}($functionName5$capitalizedParameterName1, $functionName5$capitalizedParameterName2, $functionName5$capitalizedParameterName3)\n" +
                "\n" +
                "____// Then\n" +
                "____assertEquals(${functionName5}Expected, actualTest)\n" +
                "____$DEFAULT_ASSERTION_STATEMENT\n" +
                "__}\n" +
                "}\n",
            outputString
        )
    }

    @Test
    fun `Given class data with overloaded functions when appendTestClass then returns expected output`() {
        // Given
        val overloadedFunctionName = "function1"
        val functionMetadata1 =
            FunctionMetadata(
                overloadedFunctionName,
                false,
                emptyList(),
                null,
                DataType.Specific("DataType1", false)
            )

        val parameterDataType2 = "Param2"
        val parameterName2 = "param2"
        val functionMetadata2 =
            FunctionMetadata(
                overloadedFunctionName,
                false,
                listOf(
                    TypedParameter(
                        parameterName2,
                        DataType.Specific(parameterDataType2, false)
                    )
                ),
                null,
                DataType.Specific("DataType2", false)
            )

        val commonReceiverName = "Receiver"
        val extensionReceiverType3 = DataType.Specific(commonReceiverName, false)
        val functionMetadata3 =
            FunctionMetadata(
                overloadedFunctionName,
                false,
                emptyList(),
                extensionReceiverType3,
                DataType.Specific("DataType1", false)
            )
        val mockedReceiverType = "mock<Receiver>()"
        givenMockedValue(extensionReceiverType3, mockedReceiverType)

        val parameterDataType4 = "Param4"
        val parameterName4 = "param4"
        val extensionReceiverType4 = DataType.Specific(commonReceiverName, false)
        val functionMetadata4 =
            FunctionMetadata(
                overloadedFunctionName,
                false,
                listOf(
                    TypedParameter(
                        parameterName4,
                        DataType.Specific(parameterDataType4, false)
                    )
                ),
                extensionReceiverType4,
                DataType.Specific("DataType2", false)
            )
        givenMockedValue(extensionReceiverType4, mockedReceiverType)

        val parameterName5 = "param5"
        val extensionReceiverType5 = DataType.Specific("Receiver2", false)
        val functionMetadata5 =
            FunctionMetadata(
                overloadedFunctionName,
                false,
                listOf(TypedParameter(parameterName5, DataType.Specific("Param", false))),
                extensionReceiverType5,
                DataType.Specific("DataType2", false)
            )
        givenMockedValue(extensionReceiverType5, mockedReceiverType)

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
                "__private lateinit var $CLASS_UNDER_TEST_VARIABLE_NAME: $TEST_CLASS_NAME\n" +
                "\n" +
                "__@Before\n" +
                "__fun setUp() {\n" +
                "____$CLASS_UNDER_TEST_VARIABLE_NAME = $TEST_CLASS_NAME()\n" +
                "__}\n" +
                "\n" +
                "__@Test\n" +
                "__fun `Given _ when $overloadedFunctionName() then _`() {\n" +
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
                "__fun `Given _ when $overloadedFunctionName($parameterDataType2) then _`() {\n" +
                "____// Given\n" +
                "____val $parameterName2 = null\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = $CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata1.name}($parameterName2)\n" +
                "\n" +
                "____// Then\n" +
                "____$DEFAULT_ASSERTION_STATEMENT\n" +
                "__}\n" +
                "\n" +
                "__@Test\n" +
                "__fun `Given _ when $commonReceiverName#$overloadedFunctionName() then _`() {\n" +
                "____// Given\n" +
                "____val receiver = $mockedReceiverType\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = with($CLASS_UNDER_TEST_VARIABLE_NAME) {\n" +
                "______receiver.${functionMetadata3.name}()\n" +
                "____}\n" +
                "\n" +
                "____// Then\n" +
                "____$DEFAULT_ASSERTION_STATEMENT\n" +
                "__}\n" +
                "\n" +
                "__@Test\n" +
                "__fun `Given _ when $commonReceiverName#$overloadedFunctionName($parameterDataType4) then _`() {\n" +
                "____// Given\n" +
                "____val $parameterName4 = null\n" +
                "\n" +
                "____val receiver = $mockedReceiverType\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = with($CLASS_UNDER_TEST_VARIABLE_NAME) {\n" +
                "______receiver.${functionMetadata4.name}($parameterName4)\n" +
                "____}\n" +
                "\n" +
                "____// Then\n" +
                "____$DEFAULT_ASSERTION_STATEMENT\n" +
                "__}\n" +
                "\n" +
                "__@Test\n" +
                "__fun `Given _ when ${extensionReceiverType5.name}#$overloadedFunctionName then _`() {\n" +
                "____// Given\n" +
                "____val $parameterName5 = null\n" +
                "\n" +
                "____val receiver = $mockedReceiverType\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = with($CLASS_UNDER_TEST_VARIABLE_NAME) {\n" +
                "______receiver.${functionMetadata5.name}($parameterName5)\n" +
                "____}\n" +
                "\n" +
                "____// Then\n" +
                "____$DEFAULT_ASSERTION_STATEMENT\n" +
                "__}\n" +
                "}\n",
            outputString
        )
    }

    @Test
    fun `Given parameterized test and class data with overloaded functions when appendTestClass then returns expected output`() {
        // Given
        val overloadedFunctionName = "function1"
        val dataType1 = "DataType1"
        val functionMetadata1 =
            FunctionMetadata(
                overloadedFunctionName,
                false,
                emptyList(),
                null,
                DataType.Specific(dataType1, false)
            )

        val parameterDataType2 = "Param2"
        val parameterName2 = "param2"
        val functionMetadata2 =
            FunctionMetadata(
                overloadedFunctionName,
                false,
                listOf(
                    TypedParameter(
                        parameterName2,
                        DataType.Specific(parameterDataType2, false)
                    )
                ),
                null,
                DataType.Specific("DataType2", false)
            )

        val commonReceiverName = "Receiver"
        val extensionReceiverType3 = DataType.Specific(commonReceiverName, false)
        val functionMetadata3 =
            FunctionMetadata(
                overloadedFunctionName,
                false,
                emptyList(),
                extensionReceiverType3,
                DataType.Specific(dataType1, false)
            )
        val mockedReceiverType = "mock<Receiver>()"
        givenMockedValue(extensionReceiverType3, mockedReceiverType)

        val parameterDataType4 = "Param4"
        val parameterName4 = "param4"
        val extensionReceiverType4 = DataType.Specific(commonReceiverName, false)
        val functionMetadata4 =
            FunctionMetadata(
                overloadedFunctionName,
                false,
                listOf(
                    TypedParameter(
                        parameterName4,
                        DataType.Specific(parameterDataType4, false)
                    )
                ),
                extensionReceiverType4,
                DataType.Specific("DataType2", false)
            )
        givenMockedValue(extensionReceiverType4, mockedReceiverType)

        val parameterName5 = "param5"
        val extensionReceiverType5 = DataType.Specific("Receiver2", false)
        val functionMetadata5 =
            FunctionMetadata(
                overloadedFunctionName,
                false,
                listOf(TypedParameter(parameterName5, DataType.Specific("Param5", false))),
                extensionReceiverType5,
                DataType.Specific("DataType2", false)
            )
        givenMockedValue(extensionReceiverType5, mockedReceiverType)

        val constructorExpected1 = "Expected1"
        val constructorParameterName2 = "Param2"
        val constructorExpected2 = "Expected2"
        val constructorExpected3 = "Expected3"
        val constructorParameterName4 = "Param4"
        val constructorExpected4 = "Expected4"
        val constructorParameterName5 = "Param5"
        val constructorExpected5 = "Expected5"

        given { mockerCodeGenerator.testClassParameterizedRunnerAnnotation }
            .willReturn(PARAMETERIZED_RUNNER_ANNOTATION)

        val config = givenTestStringBuilderConfiguration(
            functions = listOf(
                functionMetadata1,
                functionMetadata2,
                functionMetadata3,
                functionMetadata4,
                functionMetadata5
            ),
            isParameterized = true
        )

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                "\n" +
                "$PARAMETERIZED_RUNNER_ANNOTATION\n" +
                "class ${TEST_CLASS_NAME}Test(\n" +
                "__private val $overloadedFunctionName$constructorExpected1: $dataType1,\n" +
                "__private val $overloadedFunctionName$constructorParameterName2: $parameterDataType2,\n" +
                "__private val $overloadedFunctionName$constructorExpected2: DataType2,\n" +
                "__private val $overloadedFunctionName$constructorExpected3: $dataType1,\n" +
                "__private val $overloadedFunctionName$constructorParameterName4: Param4,\n" +
                "__private val $overloadedFunctionName$constructorExpected4: DataType2,\n" +
                "__private val $overloadedFunctionName$constructorParameterName5: Param5,\n" +
                "__private val $overloadedFunctionName$constructorExpected5: DataType2\n" +
                ") {\n" +
                "__companion object {\n" +
                "____@JvmStatic\n" +
                "____@Parameters\n" +
                "____fun data(): Collection<Array<*>> = listOf(\n" +
                "______arrayOf(null, null, null, null, null, null, null, null)\n" +
                "____)\n" +
                "__}\n" +
                "\n" +
                "__private lateinit var $CLASS_UNDER_TEST_VARIABLE_NAME: $TEST_CLASS_NAME\n" +
                "\n" +
                "__@Before\n" +
                "__fun setUp() {\n" +
                "____$CLASS_UNDER_TEST_VARIABLE_NAME = $TEST_CLASS_NAME()\n" +
                "__}\n" +
                "\n" +
                "__@Test\n" +
                "__fun `Given _ when $overloadedFunctionName() then _`() {\n" +
                "____// Given\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = $CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata1.name}()\n" +
                "\n" +
                "____// Then\n" +
                "____assertEquals($overloadedFunctionName$constructorExpected1, $ACTUAL_VALUE_VARIABLE_NAME)\n" +
                "____$DEFAULT_ASSERTION_STATEMENT\n" +
                "__}\n" +
                "\n" +
                "__@Test\n" +
                "__fun `Given _ when $overloadedFunctionName($parameterDataType2) then _`() {\n" +
                "____// Given\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = $CLASS_UNDER_TEST_VARIABLE_NAME.${functionMetadata1.name}($overloadedFunctionName$constructorParameterName2)\n" +
                "\n" +
                "____// Then\n" +
                "____assertEquals($overloadedFunctionName$constructorExpected2, $ACTUAL_VALUE_VARIABLE_NAME)\n" +
                "____$DEFAULT_ASSERTION_STATEMENT\n" +
                "__}\n" +
                "\n" +
                "__@Test\n" +
                "__fun `Given _ when $commonReceiverName#$overloadedFunctionName() then _`() {\n" +
                "____// Given\n" +
                "____val receiver = $mockedReceiverType\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = with($CLASS_UNDER_TEST_VARIABLE_NAME) {\n" +
                "______receiver.${functionMetadata3.name}()\n" +
                "____}\n" +
                "\n" +
                "____// Then\n" +
                "____assertEquals($overloadedFunctionName$constructorExpected3, $ACTUAL_VALUE_VARIABLE_NAME)\n" +
                "____$DEFAULT_ASSERTION_STATEMENT\n" +
                "__}\n" +
                "\n" +
                "__@Test\n" +
                "__fun `Given _ when $commonReceiverName#$overloadedFunctionName($parameterDataType4) then _`() {\n" +
                "____// Given\n" +
                "\n" +
                "____val receiver = $mockedReceiverType\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = with($CLASS_UNDER_TEST_VARIABLE_NAME) {\n" +
                "______receiver.${functionMetadata4.name}($overloadedFunctionName$constructorParameterName4)\n" +
                "____}\n" +
                "\n" +
                "____// Then\n" +
                "____assertEquals($overloadedFunctionName$constructorExpected4, $ACTUAL_VALUE_VARIABLE_NAME)\n" +
                "____$DEFAULT_ASSERTION_STATEMENT\n" +
                "__}\n" +
                "\n" +
                "__@Test\n" +
                "__fun `Given _ when ${extensionReceiverType5.name}#$overloadedFunctionName then _`() {\n" +
                "____// Given\n" +
                "\n" +
                "____val receiver = $mockedReceiverType\n" +
                "\n" +
                "____// When\n" +
                "____val $ACTUAL_VALUE_VARIABLE_NAME = with($CLASS_UNDER_TEST_VARIABLE_NAME) {\n" +
                "______receiver.${functionMetadata5.name}($overloadedFunctionName$constructorParameterName5)\n" +
                "____}\n" +
                "\n" +
                "____// Then\n" +
                "____assertEquals($overloadedFunctionName$constructorExpected5, $ACTUAL_VALUE_VARIABLE_NAME)\n" +
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
            FunctionMetadata(
                "function1",
                false,
                emptyList(),
                null,
                DataType.Specific("DataType1", false)
            )
        val config = givenTestStringBuilderConfiguration(
            functions = listOf(functionMetadata1)
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
    fun `Given parameterized test, class data with function and annotation exception when appendTestClass then returns expected output`() {
        // Given
        val functionName = "function1"
        val returnType = "DataType1"
        val functionMetadata1 =
            FunctionMetadata(
                functionName,
                false,
                emptyList(),
                null,
                DataType.Specific(returnType, false)
            )

        given { mockerCodeGenerator.testClassParameterizedRunnerAnnotation }
            .willReturn(PARAMETERIZED_RUNNER_ANNOTATION)

        val config = givenTestStringBuilderConfiguration(
            functions = listOf(functionMetadata1),
            isParameterized = true
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
                "$PARAMETERIZED_RUNNER_ANNOTATION\n" +
                "class ${TEST_CLASS_NAME}Test(\n" +
                "__private val ${functionName}Expected: $returnType\n" +
                ") {\n" +
                "__companion object {\n" +
                "____@JvmStatic\n" +
                "____@Parameters\n" +
                "____fun data(): Collection<Array<*>> = listOf(\n" +
                "______arrayOf(null)\n" +
                "____)\n" +
                "__}\n" +
                "\n" +
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
                "____assertEquals(${functionName}Expected, actualTest)\n" +
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
            FunctionMetadata(
                "function1",
                false,
                emptyList(),
                null,
                DataType.Specific("DataType1", false)
            )
        val config = givenTestStringBuilderConfiguration(
            functions = listOf(functionMetadata1)
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
    fun `Given parameterized test, class data with function and try-catch exception when appendTestClass then returns expected output`() {
        // Given
        val functionName = "function1"
        val returnType = "DataType1"
        val functionMetadata1 =
            FunctionMetadata(
                functionName,
                false,
                emptyList(),
                null,
                DataType.Specific(returnType, false)
            )

        given { mockerCodeGenerator.testClassParameterizedRunnerAnnotation }
            .willReturn(PARAMETERIZED_RUNNER_ANNOTATION)

        val config = givenTestStringBuilderConfiguration(
            functions = listOf(functionMetadata1),
            isParameterized = true
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
                "$PARAMETERIZED_RUNNER_ANNOTATION\n" +
                "class ${TEST_CLASS_NAME}Test(\n" +
                "__private val ${functionName}Expected: $returnType\n" +
                ") {\n" +
                "__companion object {\n" +
                "____@JvmStatic\n" +
                "____@Parameters\n" +
                "____fun data(): Collection<Array<*>> = listOf(\n" +
                "______arrayOf(null)\n" +
                "____)\n" +
                "__}\n" +
                "\n" +
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
                "____assertEquals(${functionName}Expected, actualTest)\n" +
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
    fun `Given parameterized tests, class data with mockable constructor parameters when appendTestClass then returns expected output`() {
        // Given
        val config = givenTestStringBuilderConfiguration(
            hasMockableConstructorParameters = true,
            isParameterized = true
        )
        given { mockerCodeGenerator.testClassParameterizedRunnerAnnotation }
            .willReturn(PARAMETERIZED_RUNNER_ANNOTATION)
        given { mockerCodeGenerator.mockingRule }
            .willReturn(MOCKING_RULE)

        // When
        val actualValue = cut.appendTestClass(config)

        // Then
        val outputString = actualValue.toString()
        assertEquals(
            "package $PACKAGE_NAME\n" +
                "\n" +
                "$PARAMETERIZED_RUNNER_ANNOTATION\n" +
                "class ${TEST_CLASS_NAME}Test {\n" +
                "__companion object {\n" +
                "____@JvmStatic\n" +
                "____@Parameters\n" +
                "____fun data(): Collection<Array<*>> = listOf(\n" +
                "______arrayOf()\n" +
                "____)\n" +
                "__}\n" +
                "\n" +
                "$MOCKING_RULE\n" +
                "\n" +
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
        val functionsUnderTest = StaticFunctionsMetadata(PACKAGE_NAME, emptyMap(), emptyList())
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
        val functionsUnderTest = StaticFunctionsMetadata(PACKAGE_NAME, emptyMap(), emptyList())
        val usedImports = setOf(givenImport1, givenImport2)
        val outputClassName = "outputClassName"
        val isParameterized = true
        given { mockerCodeGenerator.testClassParameterizedRunnerAnnotation }
            .willReturn("@RunWith(Parameterized::class)")

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
            FunctionMetadata(
                "function1",
                false,
                emptyList(),
                null,
                DataType.Specific("DataType1", false)
            )
        val functionMetadata2 =
            FunctionMetadata(
                "function2",
                true,
                emptyList(),
                null,
                DataType.Specific("DataType2", false)
            )
        val extensionReceiverType = DataType.Specific("ReceiverDataType", false)
        val functionMetadata3 =
            FunctionMetadata(
                "function3",
                false,
                emptyList(),
                extensionReceiverType,
                DataType.Specific("DataType3", false)
            )
        val mockedReceiverType = "mock<ReceiverDataType>()"
        givenMockedValue(extensionReceiverType, mockedReceiverType)
        val functionMetadata4 =
            FunctionMetadata("function4", false, emptyList(), null, unitDataType)
        val functionParameter1 = TypedParameter("functionParameterName1", unitDataType)
        val functionParameter2 = TypedParameter("functionParameterName2", unitDataType)
        val functionMetadata5 =
            FunctionMetadata(
                "function5",
                false,
                listOf(functionParameter1, functionParameter2),
                null,
                DataType.Specific("DataType5", false)
            )
        val mockedValue1 = "\"Some value 1\""
        givenMockedValue(functionParameter1, mockedValue1)
        val mockedValue2 = "\"Some value 2\""
        givenMockedValue(functionParameter2, mockedValue2)
        val functionsUnderTest = StaticFunctionsMetadata(
            PACKAGE_NAME,
            emptyMap(),
            listOf(
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
                "}\n",
            outputString
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

    private fun givenMockedValue(
        receiverType: DataType.Specific,
        mockedValue: String
    ) {
        given { mockerCodeGenerator.getMockedValue(receiverType.name, receiverType) }
            .willReturn(mockedValue)
    }

    private fun givenMockedValue(
        receiverType: TypedParameter,
        mockedValue: String
    ) {
        given { mockerCodeGenerator.getMockedValue(receiverType.name, receiverType.type) }
            .willReturn(mockedValue)
    }

    private fun givenTestStringBuilderConfiguration(
        isAbstract: Boolean = false,
        constructorParameters: List<TypedParameter> = emptyList(),
        functions: List<FunctionMetadata> = emptyList(),
        usedImports: Set<String> = emptySet(),
        hasMockableConstructorParameters: Boolean = false,
        isParameterized: Boolean = false
    ) = TestStringBuilderConfiguration(
        classUnderTest = ClassMetadata(
            PACKAGE_NAME,
            emptyMap(),
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

private operator fun String.times(times: Int) = Array(times) { this }.joinToString("")
