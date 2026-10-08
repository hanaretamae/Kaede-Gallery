plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.uniffi.kotlin.multiplatform) apply false
}

val releaseVersion = file("../VERSION").readText().trim()
require(Regex("\\d+\\.\\d+\\.\\d+(?:b\\d+)?").matches(releaseVersion)) {
    "VERSION must use MAJOR.MINOR.PATCH with an optional bN beta suffix."
}
version = releaseVersion
