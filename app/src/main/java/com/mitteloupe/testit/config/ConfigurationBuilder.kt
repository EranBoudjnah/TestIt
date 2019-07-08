package com.mitteloupe.testit.config

import com.mitteloupe.testit.model.Configuration
import com.mitteloupe.testit.model.Mocker

class ConfigurationBuilder {
    private var mocker: Mocker? = null
    private var classUnderTest: String? = null
    private var actualValue: String? = null
    private var defaultAssertion: String? = null

    fun addProperty(key: Any, value: Any): ConfigurationBuilder {
        when (key) {
            !is String -> throw IllegalArgumentException("Ambiguous property: $key")
            "dependency.mocker" -> {
                mocker = value.toMocker()
            }
            "vocabulary.classundertest" -> {
                classUnderTest = value.toString()
            }
            "vocabulary.actualvalue" -> {
                actualValue = value.toString()
            }
            "test.defaultassertion" -> {
                defaultAssertion = value.toString()
            }
            else -> throw IllegalArgumentException("Unknown property: $key")
        }

        return this
    }

    fun build(): Configuration {
        val outMocker = mocker ?: throw IllegalStateException("Mocker type not defined")
        val outClassUnderTest = classUnderTest ?: throw IllegalStateException("Mocker type not defined")
        val outActualValue = actualValue ?: throw IllegalStateException("Mocker type not defined")
        val outDefaultAssertion = defaultAssertion ?: throw IllegalStateException("Mocker type not defined")

        return Configuration(
            outMocker,
            outClassUnderTest,
            outActualValue,
            outDefaultAssertion
        )
    }
}

private fun Any.toMocker() = when (this) {
    is String -> when (this) {
        "mockito" -> Mocker.MOCKITO
        "mockk" -> Mocker.MOCKK
        else -> throw IllegalArgumentException("Unknown mocker type: $this")
    }
    else -> throw IllegalArgumentException("Unknown mocker type: $this")
}