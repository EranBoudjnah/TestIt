package com.mitteloupe.testit.config.model

/**
 * Created by Eran Boudjnah on 2019-07-07.
 */
data class Configuration(
    val mocker: Mocker,
    val classUnderTest: String,
    val actualValue: String,
    val defaultAssertion: String,
    val exceptionCaptureMethod: ExceptionCaptureMethod
)
