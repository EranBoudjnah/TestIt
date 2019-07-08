# TestIt

While [TDD](https://en.wikipedia.org/wiki/Test-driven_development) is a better approach for development, many projects end up with low test coverage.
This project is here to help you improve your test coverage by reducing the effort spent on writing boilerplate code, allowing you to focus on writing the actual tests.

Generate unit testing boilerplate from kotlin files.

## Getting Started

While you can run `./gradlew run --args "filepath"`, it might be more convenient to set up a shortcut to the provided helper script:

### Install

1. Get [HomeBrew](https://brew.sh/)
2. Run `brew install coreutils`
3. Create a symbolic link: `sudo ln -s /path/to/testit /usr/local/bin`

Note: your project would need to include [mockito 2](https://site.mockito.org/) and [mockito-kotlin](https://github.com/nhaarman/mockito-kotlin) or [MockK](https://mockk.io/).

## Output

TestIt generates a test file for you in the default expected path.

For example, when run against itself (`testit app/src/main/java/com/mitteloupe/testit/TestIt.kt`) -
see [source file](https://github.com/EranBoudjnah/TestIt/blob/master/app/src/main/java/com/mitteloupe/testit/TestIt.kt) -
it generates the below file at `app/src/test/java/com/mitteloupe/testit/TestItTest.kt`:

```kotlin
package com.mitteloupe.testit

import com.mitteloupe.testit.config.PropertiesReader
import com.mitteloupe.testit.file.FileProvider
import com.mitteloupe.testit.generator.TestFilePathFormatter
import com.mitteloupe.testit.generator.TestsGeneratorFactory
import com.mitteloupe.testit.model.ClassTestCode
import com.mitteloupe.testit.parser.KotlinFileParser
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TestItTest {
    private lateinit var cut: TestIt

    @Mock
    lateinit var propertiesReader: PropertiesReader

    @Mock
    lateinit var fileProvider: FileProvider

    @Mock
    lateinit var kotlinFileParser: KotlinFileParser

    @Mock
    lateinit var testFilePathFormatter: TestFilePathFormatter

    @Mock
    lateinit var testsGeneratorFactory: TestsGeneratorFactory

    @Before
    fun setUp() {
        cut = TestIt(propertiesReader, fileProvider, kotlinFileParser, testFilePathFormatter, testsGeneratorFactory)
    }

    @Test
    fun `Given _ when getTestsForFile then _`() {
        // Given
        val fileName = "fileName"

        // When
        val actualValue = cut.getTestsForFile(fileName)

        // Then
        TODO("Define assertions")
    }

    @Test
    fun `Given _ when saveTestsToFile then _`() {
        // Given
        val sourceFileName = "sourceFileName"
        val classTestCode = mock<ClassTestCode>()

        // When
        cut.saveTestsToFile(sourceFileName, classTestCode)

        // Then
        TODO("Define assertions")
    }

    @Test
    fun `Given _ when showHelp then _`() {
        // Given

        // When
        cut.showHelp()

        // Then
        TODO("Define assertions")
    }
}
```

## Features

* Automatically compiles a list of required imports
* Supports multiple classes in one Kotlin file
* Supports both [mockito 2](https://site.mockito.org/) and [MockK](https://mockk.io/)

## Acknowledgments

This code uses a JAR from [kotlin-grammar-tools](https://github.com/Kotlin/grammar-tools) to parse Kotlin code.

## Created by
[Eran Boudjnah](https://www.linkedin.com/in/eranboudjnah)

## License
MIT © [Eran Boudjnah](https://www.linkedin.com/in/eranboudjnah)