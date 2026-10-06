plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "com.hanaretamae.kaede.core.settings"
        compileSdk = 37
        buildToolsVersion = "37.0.0"
        minSdk = 21
        withHostTestBuilder { }.configure { }
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":core:repository"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
