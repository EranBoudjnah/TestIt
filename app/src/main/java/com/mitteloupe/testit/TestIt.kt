package com.mitteloupe.testit

import com.mitteloupe.testit.config.ConfigurationBuilder
import com.mitteloupe.testit.config.PropertiesReader
import com.mitteloupe.testit.file.FileInputStreamProvider
import com.mitteloupe.testit.file.FileProvider
import com.mitteloupe.testit.generator.TestFilePathFormatter
import com.mitteloupe.testit.generator.TestsGenerator
import com.mitteloupe.testit.generator.TestsGeneratorFactory
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.ClassTestCode
import com.mitteloupe.testit.parser.AntlrKotlinFileParser
import com.mitteloupe.testit.parser.KotlinFileParser

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

    fun saveTestsToFile(sourceFileName: String, classTestCode: ClassTestCode) {
        val sourceFile = fileProvider.getFile(sourceFileName)
        val outputPath = testFilePathFormatter.getTestFilePath(sourceFile.absolutePath)
        val outputFile = fileProvider.getFile("$outputPath${classTestCode.className}Test.kt")
        val isFileCreated = outputFile.createNewFile()
        if (isFileCreated) {
            outputFile.writeText(classTestCode.testSource)
            println("Wrote tests for ${classTestCode.className} to: ${outputFile.absolutePath}")
        } else {
            println("File already exists: ${outputFile.absolutePath}")
        }
    }

    fun showHelp() {
        println("File name of class to write tests for not specified.")
    }

    private fun getTestsForNodes(fileContents: String): List<ClassTestCode> {
        val fileMetadata = kotlinFileParser.parseFile(fileContents)
        val tests = mutableListOf<ClassTestCode>()

        fileMetadata.classes.forEach { classUnderTest ->
            testsGenerator.addToTests(classUnderTest)
            tests.add(
                ClassTestCode(
                    classUnderTest.packageName,
                    classUnderTest.className,
                    testsGenerator.generateTests()
                )
            )
            testsGenerator.reset()
        }

        return tests
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

    val testIt = TestIt(
        PropertiesReader(fileProvider, FileInputStreamProvider(), ConfigurationBuilder()),
        fileProvider,
        AntlrKotlinFileParser(),
        TestFilePathFormatter(),
        TestsGeneratorFactory()
    )

    if (args.isEmpty()) {
        testIt.showHelp()

    } else {
        val fileName = args[0]

        testIt.getTestsForFile(fileName).forEach { classTestCode ->
            testIt.saveTestsToFile(fileName, classTestCode)
        }
    }
}
