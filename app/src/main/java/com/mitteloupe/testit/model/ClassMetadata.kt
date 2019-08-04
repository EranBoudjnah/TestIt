package com.mitteloupe.testit.model

data class ClassMetadata(
    val packageName: String,
    override val imports: Map<String, String>,
    val className: String,
    val isAbstract: Boolean,
    val constructorParameters: List<TypedParameter>,
    override val functions: List<FunctionMetadata>
) : ImportsContainer, FunctionsMetadataContainer
