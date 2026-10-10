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
require(Regex("\\d+\\.\\d+\\.\\d+(?:b\\d+)?").matches(releaseVersion)) {
    "VERSION must use MAJOR.MINOR.PATCH with an optional bN beta suffix."
}
version = releaseVersion

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
