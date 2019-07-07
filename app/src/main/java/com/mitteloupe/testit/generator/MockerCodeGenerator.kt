package com.mitteloupe.testit.generator

/**
 * Created by Eran Boudjnah on 2019-07-07.
 */
interface MockerCodeGenerator {
    val testClassAnnotation: String?

    val usedImports: Map<String, String>

    val knownImports: Map<String, String>

    val setUpStatements: String?

    fun getConstructorMock(parameterName: String, parameterType: String): String

    fun getMockedInstance(variableType: String): String

    fun getRequiredImports(): Set<String>

    fun setHasMockedConstructorParameters()

    fun setHasMockedFunctionParameters()
}