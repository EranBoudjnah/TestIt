package com.mitteloupe.testit.model

data class FunctionMetadata(
    val name: String,
    val isAbstract: Boolean,
    val parameters: List<TypedParameter>,
    val extensionReceiverType: DataType?,
    val returnType: DataType
)