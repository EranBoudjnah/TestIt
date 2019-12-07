package com.mitteloupe.testit.model

data class FileMetadata(
    val classes: List<ClassMetadata>,
    val staticFunctions: StaticFunctionsMetadata
)
