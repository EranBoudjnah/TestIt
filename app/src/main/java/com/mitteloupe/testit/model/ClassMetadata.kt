package com.mitteloupe.testit.model

data class ClassMetadata(
    val packageName: String,
    val imports: Map<String, String>,
    val className: String,
    val constructorParameters: List<TypedParameter>,
    val functions: List<FunctionMetadata>
)
