plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.hanaretamae.kaede.ui.gallery.resources"
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = layout.buildDirectory.dir("generated/brandingResources"),
    )
}

val generateGalleryBranding by tasks.registering {
    val versionFile = rootProject.file("../VERSION")
    val iconFile = rootProject.file("shared-assets/branding/kaede-gallery-icon.png")
    val sharedResources = file("src/commonMain/composeResources")
    val resources = layout.buildDirectory.dir("generated/brandingResources")
    val sources = layout.buildDirectory.dir("generated/galleryBuildInfo")
    inputs.files(versionFile, iconFile)
    inputs.dir(sharedResources)
    outputs.dirs(resources, sources)
    doLast {
        sharedResources.copyRecursively(resources.get().asFile, overwrite = true)
        val drawable = resources.get().file("drawable/kaede_gallery_icon.png").asFile
        drawable.parentFile.mkdirs()
        iconFile.copyTo(drawable, overwrite = true)
        val source = sources.get().file("GalleryBuildInfo.kt").asFile
        source.parentFile.mkdirs()
        source.writeText(
            "package com.hanaretamae.kaede.ui.gallery\n\n" +
                "internal const val GalleryAppVersion = \"${versionFile.readText().trim()}\"\n",
        )
    }
}
tasks.configureEach {
    if (
        name.startsWith("compile") ||
        name in setOf(
            "prepareComposeResourcesTaskForCommonMain",
            "copyNonXmlValueResourcesForCommonMain",
            "convertXmlValueResourcesForCommonMain",
        )
    ) {
        dependsOn(generateGalleryBranding)
    }
}

kotlin {
    android {
        namespace = "com.hanaretamae.kaede.ui"
        compileSdk {
            version = release(37) {
                minorApiLevel = 1
            }
        }
        buildToolsVersion = "37.0.0"
        minSdk = 21
        withHostTestBuilder { }.configure { }
    }
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/galleryBuildInfo"))
        }
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:repository"))
            api(project(":core:settings"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(
                "org.jetbrains.compose.components:components-resources:" +
                    libs.versions.compose.multiplatform.get(),
            )
            implementation(libs.material.kolor.material3)
            implementation(libs.compose.colorpicker)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.uiTestJUnit4)
            implementation(compose.desktop.currentOs)
        }
    }
}
