package com.mitteloupe.testit

import com.mitteloupe.testit.config.ConfigurationBuilder
import com.mitteloupe.testit.config.PropertiesReader
import com.mitteloupe.testit.file.FileInputStreamProvider
import com.mitteloupe.testit.file.FileProvider
import com.mitteloupe.testit.generator.MockerCodeGeneratorProvider
import com.mitteloupe.testit.generator.TestFilePathFormatter
import com.mitteloupe.testit.generator.TestsGenerator
import com.mitteloupe.testit.generator.TestsGeneratorFactory
import com.mitteloupe.testit.generator.formatting.Formatting
import com.mitteloupe.testit.generator.mapper.DateTypeToParameterMapper
import com.mitteloupe.testit.generator.mocking.MockableTypeQualifier
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.ClassTestCode
import com.mitteloupe.testit.model.FileMetadata
import com.mitteloupe.testit.model.StaticFunctionsMetadata
import com.mitteloupe.testit.parser.AntlrKotlinFileParser
import com.mitteloupe.testit.parser.KotlinFileParser
import com.mitteloupe.testit.terminal.mapper.ArgsToRunParameters
import java.io.File

/**
 * Created by Eran Boudjnah on 2019-07-04.
 */
class TestIt(
    private val propertiesReader: PropertiesReader,
    private val fileProvider: FileProvider,
    private val kotlinFileParser: KotlinFileParser,
    private val testFilePathFormatter: TestFilePathFormatter,
    private val testsGeneratorFactory: TestsGeneratorFactory
) {
    private lateinit var testsGenerator: TestsGenerator

    init {
        loadConfiguration()
    }

    fun getTestsForFile(filePath: String, parameterized: Boolean) =
        getFileContents(filePath)?.let { contents ->
            getTestsForNodes(
                filePath,
                contents,
                parameterized
            )
        } ?: listOf()

    fun saveTestsToFile(sourceFileName: String, classTestCode: ClassTestCode): String {
        val outputFile = getTestOutputFile(sourceFileName, classTestCode.className)
        outputFile.parentFile.mkdirs()
        val isFileCreated = outputFile.createNewFile()
        return if (isFileCreated) {
            outputFile.writeText(classTestCode.testSource)
            "Wrote tests for ${classTestCode.className} to: ${outputFile.absolutePath}"
        } else {
            "File already exists, skipped: ${outputFile.absolutePath}"
        }
    }

    fun showHelp() {
        print("File name of class to write tests for not specified.")
    }

    private fun isFileExisting(sourceFileName: String, className: String) =
        getTestOutputFile(sourceFileName, className).exists()

    private fun getTestOutputFile(
        sourceFileName: String,
        className: String
    ): File {
        val outputPath = getTestOutputFilePath(sourceFileName)
        return fileProvider.getFile("$outputPath${className}Test.kt")
    }

    private fun getTestOutputFilePath(sourceFileName: String): String? {
        val sourceFile = fileProvider.getFile(sourceFileName)
        return testFilePathFormatter.getTestFilePath(sourceFile.absolutePath)
    }

    private fun getTestsForNodes(
        filePath: String,
        fileContents: String,
        parameterized: Boolean
    ) = kotlinFileParser
        .parseFile(fileContents).let { fileMetaData ->
            fileMetaData.getTestsForClasses(filePath, parameterized)
                .also { tests ->
                    if (fileMetaData.staticFunctions.functions.isNotEmpty()) {
                        tests.plus(
                            fileMetaData.getTestsForStaticFunctions(
                                filePath,
                                fileMetaData.staticFunctions.packageName,
                                parameterized
                            )
                        )
                    }
                }
        }

    private fun FileMetadata.getTestsForClasses(
        sourceFilePath: String,
        isParameterized: Boolean
    ) = classes
        .mapNotNull { classUnderTest ->
            if (isFileExisting(sourceFilePath, classUnderTest.className)) {
                println(
                    "File already exists, skipped: ${getTestOutputFile(
                        sourceFilePath,
                        classUnderTest.className
                    )}"
                )
                null
            } else {
                val outputTestCode = generateTestsForClassUnderTest(classUnderTest, isParameterized)
                if (outputTestCode.isBlank()) {
                    null
                } else {
                    ClassTestCode(
                        classUnderTest.packageName,
                        classUnderTest.className,
                        outputTestCode
                    )
                }
            }
        }

    private fun FileMetadata.getTestsForStaticFunctions(
        filePath: String,
        packageName: String,
        isParameterized: Boolean
    ): ClassTestCode {
        val fileName = filePath.getFileName()
        val outputFileName = getStaticFunctionsTestFileName(fileName)
        val testCode = if (isFileExisting(filePath, outputFileName)) {
            ""
        } else {
            generateTestsForStaticFunctions(staticFunctions, outputFileName, isParameterized)
        }

        return ClassTestCode(
            packageName,
            outputFileName,
            testCode
        )
    }

    private fun getStaticFunctionsTestFileName(sourceFileName: String): String {
        val fileNameWithoutExtension = sourceFileName.substringBeforeLast(".")
        return "${fileNameWithoutExtension}Statics"
    }

    private fun generateTestsForClassUnderTest(
        classUnderTest: ClassMetadata,
        isParameterized: Boolean
    ): String {
        testsGenerator.addToTests(classUnderTest, isParameterized)
        val result = testsGenerator.generateTests()
        testsGenerator.reset()
        return result
    }

    private fun generateTestsForStaticFunctions(
        functionsUnderTest: StaticFunctionsMetadata,
        outputClassName: String,
        isParameterized: Boolean
    ): String {
        testsGenerator.addToTests(functionsUnderTest, outputClassName, isParameterized)
        val result = testsGenerator.generateTests()
        testsGenerator.reset()
        return result
    }

    private fun loadConfiguration() {
        val appPath = getApplicationRootPath()
        val configuration = propertiesReader.readFromFile("$appPath/settings.properties")

        testsGenerator = testsGeneratorFactory.createTestsGenerator(configuration)
    }

    private fun getApplicationRootPath() =
        System.getProperty("user.dir")
            .substringBeforeLast("/")

    private fun KotlinFileParser.parseFile(source: String) = source.parse()

    private fun TestsGenerator.addToTests(
        classUnderTest: ClassMetadata,
        isParameterized: Boolean
    ) = classUnderTest.addToTests(isParameterized)

    private fun TestsGenerator.addToTests(
        functionsUnderTest: StaticFunctionsMetadata,
        outputClassName: String,
        isParameterized: Boolean
    ) = functionsUnderTest.addToTests(outputClassName, isParameterized)

    private fun getFileContents(fileName: String): String? {
        val file = fileProvider.getFile(fileName)
        if (!file.exists()) {
            println("File not found: $fileName")
            return null
        }
        return file.readLines().joinToString("\n")
    }

    private fun String.getFileName() = substringAfterLast("/")
}

fun main(args: Array<String>) {
    val fileProvider = FileProvider()

    val propertiesReader =
        PropertiesReader(fileProvider, FileInputStreamProvider(), ConfigurationBuilder())
    val mockerCodeGeneratorProvider = MockerCodeGeneratorProvider(MockableTypeQualifier())
    val formatting = Formatting()
    val dateTypeToParameterMapper = DateTypeToParameterMapper()
    val testsGeneratorFactory = TestsGeneratorFactory(
        mockerCodeGeneratorProvider,
        formatting,
        dateTypeToParameterMapper
    )
    val testIt = TestIt(
        propertiesReader,
        fileProvider,
        AntlrKotlinFileParser(),
        TestFilePathFormatter(),
        testsGeneratorFactory
    )

    val argsToRunParameters = ArgsToRunParameters()
    val runParameters = argsToRunParameters.toParameters(args)

    if (runParameters.filePath.isNullOrBlank()) {
        testIt.showHelp()

    } else {
        val filePath = runParameters.filePath

        testIt.getTestsForFile(filePath, runParameters.parameterized).forEach { classTestCode ->
            println(testIt.saveTestsToFile(filePath, classTestCode))
        }
    }
}
