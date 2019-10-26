package com.mitteloupe.testit.generator.formatting

import com.mitteloupe.testit.model.FunctionMetadata

val FunctionMetadata.expectedReturnValueVariableName
    get() = "${name}Expected"

val FunctionMetadata.nameInTestFunctionName
    get() = extensionReceiverType?.let { "${extensionReceiverType.name}#$name" } ?: name
