package com.mitteloupe.testit.config.model

data class Configuration(
    val mocker: Mocker,
    val mockitoRule: String,
    val classUnderTest: String,
    val actualValue: String,
    val defaultAssertion: String,
    val exceptionCaptureMethod: ExceptionCaptureMethod
)
