package com.mitteloupe.testit.model

data class FunctionMetadata(
    val name: String,
    val parameters: List<TypedParameter>,
    val returnType: String
)