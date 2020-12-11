package com.mitteloupe.testit.generator.formatting

import com.mitteloupe.testit.model.FunctionMetadata

fun FunctionMetadata.expectedReturnValueVariableName(suffix: String = "") = "${name}Expected$suffix"

val FunctionMetadata.nameInTestFunctionName
    get() = extensionReceiverType?.let { "${extensionReceiverType.name}#$name" } ?: name
