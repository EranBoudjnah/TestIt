package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.StaticFunctionsMetadata

/**
 * Created by Eran Boudjnah on 2019-07-05.
 */
interface TestsGenerator {
    fun ClassMetadata.addToTests()

    fun StaticFunctionsMetadata.addToTests(outputClassName: String)

    fun reset()

    fun generateTests(): String
}