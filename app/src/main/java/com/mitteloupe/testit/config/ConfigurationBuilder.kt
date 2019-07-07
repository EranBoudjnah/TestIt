package com.mitteloupe.testit.config

import com.mitteloupe.testit.model.Configuration
import com.mitteloupe.testit.model.Mocker

class ConfigurationBuilder {
    private var mocker: Mocker? = null

    fun addProperty(key: Any, value: Any) = when (key) {
        !is String -> throw IllegalArgumentException("Ambiguous property: $key")
        "dependency.mocker" -> {
            mocker = value.toMocker()
            this
        }
        else -> throw IllegalArgumentException("Unknown property: $key")
    }

    fun build(): Configuration {
        val outMocker = mocker ?: throw IllegalStateException("Mocker type not defined")

        return Configuration(outMocker)
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