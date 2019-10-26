package com.mitteloupe.testit.generator.formatter

import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.TypedParameter

fun TypedParameter.toKotlinString(
    function: FunctionMetadata,
    isParameterized: Boolean
) = if (isParameterized) {
    "${function.name}${name.capitalize()}"
} else {
    name
}
