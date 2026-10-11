plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

sourceSets.main {
    resources.srcDir(rootProject.file("shared-assets"))
    resources.srcDir(rootProject.layout.buildDirectory.dir("generated/distribution-assets"))
}

tasks.named("processResources") {
    dependsOn(rootProject.tasks.named("prepareDistributionNotices"))
}

val targetLinuxArchitecture = providers.gradleProperty("kaede.desktop.linuxArchitecture").orNull
val targetDesktopDependency = when (targetLinuxArchitecture) {
    null -> null
    "aarch64" -> "org.jetbrains.compose.desktop:desktop-jvm-linux-arm64:${libs.versions.compose.multiplatform.get()}"
    else -> error("Unsupported Kaede Linux desktop architecture: $targetLinuxArchitecture")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:repository"))
    implementation(project(":core:settings"))
    implementation(project(":core:rust"))
    implementation(project(":ui:app"))
    if (targetDesktopDependency == null) {
        implementation(compose.desktop.currentOs)
    } else {
        implementation(targetDesktopDependency)
    }
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.material.kolor.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jna)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(compose.desktop.uiTestJUnit4)
}

compose.desktop {
    application {
        mainClass = "com.hanaretamae.kaede.desktop.MainKt"
        nativeDistributions {
            windows {
                iconFile.set(rootProject.file("shared-assets/branding/kaede-gallery-icon.ico"))
            }
        }
    }
}
