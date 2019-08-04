package com.mitteloupe.testit.model

interface FunctionsMetadataContainer {
    val functions: List<FunctionMetadata>
}

val FunctionsMetadataContainer.concreteFunctions
    get() = functions.filter { !it.isAbstract }
