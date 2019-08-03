package com.mitteloupe.testit

import com.mitteloupe.testit.config.ConfigurationBuilder
import com.mitteloupe.testit.config.PropertiesReader
import com.mitteloupe.testit.file.FileInputStreamProvider
import com.mitteloupe.testit.file.FileProvider
import com.mitteloupe.testit.generator.MockableTypeQualifier
import com.mitteloupe.testit.generator.MockerCodeGeneratorProvider
import com.mitteloupe.testit.generator.TestFilePathFormatter
import com.mitteloupe.testit.generator.TestsGenerator
import com.mitteloupe.testit.generator.TestsGeneratorFactory
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.ClassTestCode
import com.mitteloupe.testit.parser.AntlrKotlinFileParser
import com.mitteloupe.testit.parser.KotlinFileParser
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

    fun getTestsForFile(fileName: String) = getFileContents(fileName)?.let { getTestsForNodes(it) } ?: listOf()

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

    private fun isFileExisting(sourceFileName: String, className: String): Boolean {
        val outputFile = getTestOutputFile(sourceFileName, className)
        return outputFile.exists()
    }

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

    private fun getTestsForNodes(fileContents: String) = kotlinFileParser
        .parseFile(fileContents)
        .classes
        .map { classUnderTest ->
            val outputTestCode = if (isFileExisting("", classUnderTest.className)) {
                ""
            } else {
                generateTestsForClassUnderTest(classUnderTest)
            }

            ClassTestCode(
                classUnderTest.packageName,
                classUnderTest.className,
                outputTestCode
            )
        }

    private fun generateTestsForClassUnderTest(classUnderTest: ClassMetadata): String {
        testsGenerator.addToTests(classUnderTest)
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

    private fun TestsGenerator.addToTests(classUnderTest: ClassMetadata) = classUnderTest.addToTests()

    private fun getFileContents(fileName: String): String? {
        val file = fileProvider.getFile(fileName)
        if (!file.exists()) {
            println("File not found: $fileName")
            return null
        }
        return file.readLines().joinToString("\n")
    }
}

fun main(args: Array<String>) {
    val fileProvider = FileProvider()

    val propertiesReader = PropertiesReader(fileProvider, FileInputStreamProvider(), ConfigurationBuilder())
    val mockerCodeGeneratorProvider = MockerCodeGeneratorProvider(MockableTypeQualifier())
    val testsGeneratorFactory = TestsGeneratorFactory(mockerCodeGeneratorProvider)
    val testIt = TestIt(
        propertiesReader,
        fileProvider,
        AntlrKotlinFileParser(),
        TestFilePathFormatter(),
        testsGeneratorFactory
    )

    if (args.isEmpty()) {
        testIt.showHelp()

    } else {
        val fileName = args[0]

        testIt.getTestsForFile(fileName).forEach { classTestCode ->
            println(testIt.saveTestsToFile(fileName, classTestCode))
        }
    }
}
