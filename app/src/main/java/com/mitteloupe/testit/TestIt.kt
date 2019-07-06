package com.mitteloupe.testit

import com.mitteloupe.testit.generator.KotlinJUnitTestGenerator
import com.mitteloupe.testit.generator.TestsGenerator
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.ClassTestCode
import com.mitteloupe.testit.parser.AntlrKotlinFileParser
import com.mitteloupe.testit.parser.KotlinFileParser
import java.io.File

/**
 * Created by Eran Boudjnah on 2019-07-04.
 */
class TestIt(
    private val kotlinFileParser: KotlinFileParser,
    private val testsGenerator: TestsGenerator
) {
    fun getTestsForFile(fileName: String) = getFileContents(fileName)?.let { getTestsForNodes(it) } ?: listOf()

    fun saveTestsToFile(sourceFileName: String, classTestCode: ClassTestCode) {
        val sourceFile = File(sourceFileName)
        val outputPath = getTestPathForClass(sourceFile.absolutePath)
        val outputFile = File("$outputPath${classTestCode.className}Test.kt")
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

    private fun KotlinFileParser.parseFile(source: String) = source.parse()

    private fun getTestPathForClass(fileName: String): String? {
        val pathRegex = Regex("/src/[a-z]+/(java|kotlin)/")

        return if (pathRegex.containsMatchIn(fileName)) {
            val sourcePath = fileName.substringBeforeLast("/") + "/"
            pathRegex.replace(sourcePath) { matchResult ->
                "/src/test/${matchResult.groupValues[1]}/"
            }
        } else {
            null
        }
    }

    private fun TestsGenerator.addToTests(classUnderTest: ClassMetadata) = classUnderTest.addToTests()

    private fun getFileContents(fileName: String): String? {
        val file = File(fileName)
        if (!file.exists()) {
            println("File not found: $fileName")
            return null
        }
        return file.readLines().joinToString("\n")
    }
}

fun main(args: Array<String>) {
    val testIt = TestIt(AntlrKotlinFileParser(), KotlinJUnitTestGenerator(StringBuilder()))

    if (args.isEmpty()) {
        testIt.showHelp()

    } else {
        val fileName = args[0]

        testIt.getTestsForFile(fileName).forEach { classTestCode ->
            testIt.saveTestsToFile(fileName, classTestCode)
        }
    }
}
