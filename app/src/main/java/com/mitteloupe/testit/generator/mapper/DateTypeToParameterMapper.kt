package com.mitteloupe.testit.generator.mapper

import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.TypedParameter

class DateTypeToParameterMapper {
    fun toParameters(dataTypes: List<DataType>) =
        dataTypes.map { dataType -> toParameter(dataType) }

    private fun toParameter(dataType: DataType) =
        TypedParameter(dataType.name, dataType)
}