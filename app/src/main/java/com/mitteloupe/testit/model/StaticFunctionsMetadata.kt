package com.mitteloupe.testit.model

data class StaticFunctionsMetadata(
    val packageName: String,
    override val imports: Map<String, String>,
    override val functions: List<FunctionMetadata>
) : ImportsContainer, FunctionsMetadataContainer
