plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "com.hanaretamae.kaede.core.model"
        compileSdk = 37
        buildToolsVersion = "37.0.0"
        minSdk = 21
        withHostTestBuilder { }.configure { }
    }
    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
