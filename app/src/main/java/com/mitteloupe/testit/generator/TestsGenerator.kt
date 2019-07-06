package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.ClassMetadata

/**
 * Created by Eran Boudjnah on 2019-07-05.
 */
interface TestsGenerator {
    fun ClassMetadata.addToTests()

    fun reset()

    fun generateTests(): String
}