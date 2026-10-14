import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
val releaseVersionMatch =
    Regex("(\\d+)\\.(\\d+)\\.(\\d+)(?:(b|rc)([1-9][0-9]{0,2}))?").matchEntire(releaseVersion)
        ?: error("VERSION must use MAJOR.MINOR.PATCH with an optional bN or rcN suffix.")
val releasePrereleaseKind = releaseVersionMatch.groupValues[4]
val releasePrereleaseNumber = releaseVersionMatch.groupValues[5].toLongOrNull()
require(
    when (releasePrereleaseKind) {
        "b" -> releasePrereleaseNumber != null && releasePrereleaseNumber in 1L..898L
        "rc" -> releasePrereleaseNumber != null && releasePrereleaseNumber in 1L..100L
        else -> true
    },
) {
    "VERSION must use b1 through b898 or rc1 through rc100 before the final release."
}
version = releaseVersion

subprojects {
    tasks.withType<KotlinJvmCompile>().configureEach {
        if (project.path == ":desktopApp" || name.contains("Jvm")) {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        if (project.path == ":desktopApp" || name.contains("Jvm")) {
            sourceCompatibility = JavaVersion.VERSION_17.toString()
            targetCompatibility = JavaVersion.VERSION_17.toString()
        }
    }
    tasks.withType<Test>().configureEach {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}

val prepareDistributionNotices = tasks.register("prepareDistributionNotices") {
    val projectLicense = rootProject.file("../LICENSE")
    val thirdPartyNotices = rootProject.file("../THIRD_PARTY_NOTICES.md")
    val japaneseThirdPartyNotices = rootProject.file("../THIRD_PARTY_NOTICES.ja.md")
    val runtimeLicenses = rootProject.file("shared-assets/licenses")
    val distributionAssets = layout.buildDirectory.dir("generated/distribution-assets")
    inputs.files(projectLicense, thirdPartyNotices, japaneseThirdPartyNotices, runtimeLicenses)
    outputs.dir(distributionAssets)
    doLast {
        val licenseDirectory = distributionAssets.get().dir("licenses").asFile
        licenseDirectory.mkdirs()
        projectLicense.copyTo(licenseDirectory.resolve("Kaede-Gallery-MIT.txt"), overwrite = true)
        thirdPartyNotices.copyTo(licenseDirectory.resolve("THIRD_PARTY_NOTICES.md"), overwrite = true)
        japaneseThirdPartyNotices.copyTo(
            licenseDirectory.resolve("THIRD_PARTY_NOTICES.ja.md"),
            overwrite = true,
        )
        val runtimeLicenseFiles = runtimeLicenses.listFiles()
            ?.filter { it.isFile && it.name != "RUST-DEPENDENCY-LICENSES.txt" }
            ?.sortedBy { it.name }
            ?: error("Runtime license directory could not be read.")
        require(runtimeLicenseFiles.isNotEmpty()) { "No runtime license texts were found." }
        licenseDirectory.resolve("Runtime-LICENSES.txt").writeText(
            runtimeLicenseFiles.joinToString("\n\n") { file ->
                "===== ${file.name} =====\n\n${file.readText().trim()}"
            } + "\n",
        )
    }
}
