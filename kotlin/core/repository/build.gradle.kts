plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "com.hanaretamae.kaede.core.repository"
        compileSdk = 37
        buildToolsVersion = "37.0.0"
        minSdk = 21
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(libs.kotlinx.coroutines.core)
        }
    }
}
