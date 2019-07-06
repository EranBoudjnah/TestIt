# TestIt

Generate unit testing boilerplate from kotlin files.

## Getting Started

While you can run `./gradlew run --args "filepath"`, it might be more convenient to set up a shortcut to the provided helper script:

### Prerequisites

1. Make the helper script executable: `chmod +x /path/to/testit`
2. Get [HomeBrew](https://brew.sh/)
3. Run `brew install coreutils`
4. Create a symbolic link: `sudo ln -s /path/to/testit /usr/local/bin`

## Output

TestIt generates a test file for you in the default expected path.

For example, when run against itself (`testit app/src/main/java/com/mitteloupe/testit/TestIt.kt`) -
see [source file](https://github.com/EranBoudjnah/TestIt/blob/master/app/src/main/java/com/mitteloupe/testit/TestIt.kt) -
it generates the below file at `app/src/test/java/com/mitteloupe/testit/TestItTest.kt`:

```
package com.mitteloupe.testit

import com.mitteloupe.testit.generator.TestsGenerator
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
    lateinit var kotlinFileParser: KotlinFileParser

    @Mock
    lateinit var testsGenerator: TestsGenerator

    @Before
    fun setUp() {
        cut = TestIt(kotlinFileParser, testsGenerator)
    }

    @Test
    fun `Given _ when getTestsForFile then _`() {
        // Given
        val fileName = mock<String>()

        // When
        val actual = cut.getTestsForFile(fileName)

        // Then
    }

    @Test
    fun `Given _ when saveTestsToFile then _`() {
        // Given
        val sourceFileName = mock<String>()
        val classTestCode = mock<ClassTestCode>()

        // When
        cut.saveTestsToFile(sourceFileName, classTestCode)

        // Then
    }

    @Test
    fun `Given _ when showHelp then _`() {
        // Given

        // When
        cut.showHelp()

        // Then
    }
}
```

## Acknowledgments

This code uses a JAR from [kotlin-grammar-tools](https://github.com/Kotlin/grammar-tools) to parse Kotlin code.

## Created by
[Eran Boudjnah](https://www.linkedin.com/in/eranboudjnah)

## License
MIT © [Eran Boudjnah](https://www.linkedin.com/in/eranboudjnah)