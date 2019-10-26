package com.mitteloupe.testit.generator

import com.mitteloupe.testit.model.ClassMetadata

data class TestStringBuilderConfiguration(
    val classUnderTest: ClassMetadata,
    val usedImports: Set<String>,
    val hasMockableConstructorParameters: Boolean,
    val isParameterized: Boolean
)