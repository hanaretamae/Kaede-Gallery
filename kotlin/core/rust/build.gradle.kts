plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.uniffi.kotlin.multiplatform)
}

val nixBindgenSource = providers.gradleProperty("kaede.uniffi.bindgenSource").orNull

uniffi {
    nixBindgenSource?.let { sourcePath ->
        bindgenFromPath(rootProject.layout.projectDirectory.dir("$sourcePath/bindgen"))
    }
    generateFromLibrary {
        packageName = "com.hanaretamae.kaede.core.rust"
    }
}

cargo {
    packageDirectory = rootProject.layout.projectDirectory.dir("../crates/gallery-ffi")
    ndkVersion = "28.2.13676358"
}

kotlin {
    android {
        namespace = "com.hanaretamae.kaede.core.rust"
        compileSdk = 37
        buildToolsVersion = "37.0.0"
        minSdk = 21
        withHostTestBuilder { }.configure { }
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:repository"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
