plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.kotlin)
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.compiler)
    implementation(libs.kotlin.script.util)

    testImplementation(libs.junit)

    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)

    testImplementation(libs.mockk)

    testImplementation(libs.assertj.core)
    testImplementation(libs.hamcrest)
}
