plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

sourceSets.main {
    resources.srcDir(rootProject.file("shared-assets"))
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:repository"))
    implementation(project(":core:settings"))
    implementation(project(":core:rust"))
    implementation(project(":ui:app"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.jna)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

compose.desktop {
    application {
        mainClass = "com.hanaretamae.kaede.desktop.MainKt"
    }
}
