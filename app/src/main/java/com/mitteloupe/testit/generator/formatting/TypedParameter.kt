package com.mitteloupe.testit.generator.formatting

import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.TypedParameter

fun TypedParameter.toKotlinString(
    function: FunctionMetadata,
    isParameterized: Boolean
) = if (type.isUnit) {
    "Unit"
}else {
    if (isParameterized) {
        "${function.name}${name.capitalize()}"
    } else {
        name
    }
}
