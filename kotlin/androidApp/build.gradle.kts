plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.hanaretamae.kaede.android"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.hanaretamae.kaede"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".kmpdebug"
        }
    }

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
