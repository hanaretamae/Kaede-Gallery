plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

private val releaseVersion = rootProject.version.toString()
private val releaseVersionMatch =
    Regex("(\\d+)\\.(\\d+)\\.(\\d+)(?:b(\\d+))?").matchEntire(releaseVersion)
        ?: error("VERSION must use MAJOR.MINOR.PATCH with an optional bN beta suffix.")
private val releaseVersionParts = (1..3).map { releaseVersionMatch.groupValues[it].toLong() }
private val betaNumber = releaseVersionMatch.groupValues[4].takeIf(String::isNotEmpty)?.toLong()
private val releaseVersionCodeBase =
    releaseVersionParts[0] * 1_000_000L + releaseVersionParts[1] * 1_000L + releaseVersionParts[2]
private val releaseVersionCodeSuffix = betaNumber ?: 999L

require(releaseVersionParts[1] < 1_000L && releaseVersionParts[2] < 1_000L) {
    "VERSION minor and patch components must be below 1000."
}
require(betaNumber == null || betaNumber in 1L..998L) {
    "VERSION beta suffix must be b1 through b998 to reserve a higher code for the final release."
}
private val releaseVersionCodeLong = releaseVersionCodeBase * 1_000L + releaseVersionCodeSuffix
require(releaseVersionCodeLong in 1L..2_100_000_000L) {
    "VERSION produces an Android versionCode outside the supported range."
}
private val releaseVersionCode = releaseVersionCodeLong.toInt()

android {
    namespace = "com.hanaretamae.kaede.android"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.hanaretamae.kaede"
        minSdk = 23
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersion
    }

    signingConfigs {
        create("release") {
            storeFile = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull?.let(::file)
            storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".kmpdebug"
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
        create("acceptance") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".kmpacceptance"
            matchingFallbacks += listOf("debug")
        }
    }

    sourceSets.getByName("main").assets.directories.add(rootProject.file("shared-assets").path)

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:repository"))
    implementation(project(":core:rust"))
    implementation(project(":core:settings"))
    implementation(project(":ui:app"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation.android)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.ui.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
}
