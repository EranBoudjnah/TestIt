package com.mitteloupe.testit.processing

import com.mitteloupe.testit.model.FunctionMetadata

fun FunctionMetadata.hasReturnValue() = returnType.name != "Unit"
