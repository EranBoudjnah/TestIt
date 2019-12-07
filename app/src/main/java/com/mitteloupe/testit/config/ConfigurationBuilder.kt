package com.mitteloupe.testit.config

import com.mitteloupe.testit.config.model.Configuration
import com.mitteloupe.testit.config.model.ExceptionCaptureMethod
import com.mitteloupe.testit.config.model.Mocker

class ConfigurationBuilder {
    private var mocker: Mocker? = null
    private var classUnderTest: String? = null
    private var actualValue: String? = null
    private var defaultAssertion: String? = null
    private var exceptionCaptureMethod: ExceptionCaptureMethod = ExceptionCaptureMethod.NO_CAPTURE

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
            "test.exceptioncapture" -> {
                exceptionCaptureMethod = (value as String).toExceptionCaptureMethod()
            }
            else -> throw IllegalArgumentException("Unknown property: $key")
        }

        return this
    }

    fun build(): Configuration {
        val outMocker = mocker ?: throw IllegalStateException("Mocker type not defined")
        val outClassUnderTest =
            classUnderTest ?: throw IllegalStateException("Mocker type not defined")
        val outActualValue = actualValue ?: throw IllegalStateException("Mocker type not defined")
        val outDefaultAssertion =
            defaultAssertion ?: throw IllegalStateException("Mocker type not defined")

        return Configuration(
            outMocker,
            outClassUnderTest,
            outActualValue,
            outDefaultAssertion,
            exceptionCaptureMethod
        )
    }
}

private fun String.toExceptionCaptureMethod() = when (this.toLowerCase()) {
    "annotation", "expects", "annotationexpects" -> ExceptionCaptureMethod.ANNOTATION_EXPECTS
    "try", "catch", "trycatch", "try/catch" -> ExceptionCaptureMethod.TRY_CATCH
    "no", "none", "false", "" -> ExceptionCaptureMethod.NO_CAPTURE
    else -> throw IllegalStateException("Unknown exception capture method: $this")
}

private fun Any.toMocker() = when (this) {
    is String -> when (this) {
        "mockito" -> Mocker.MOCKITO
        "mockk" -> Mocker.MOCKK
        else -> throw IllegalArgumentException("Unknown mocker type: $this")
    }
    else -> throw IllegalArgumentException("Unknown mocker type: $this")
}
