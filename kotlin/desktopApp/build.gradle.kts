import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

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
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
        nativeDistributions {
            windows {
                iconFile.set(rootProject.file("shared-assets/branding/kaede-gallery-icon.ico"))
            }
        }
    }
}

tasks.configureEach {
    if (name == "createDistributable") {
        doLast {
            if (!System.getProperty("os.name").startsWith("Linux", ignoreCase = true)) {
                return@doLast
            }

            val appLibDir = project.layout.buildDirectory
                .dir("compose/binaries/main/app/desktopApp/lib/app")
                .get()
                .asFile
            val extendedJar = appLibDir.listFiles().orEmpty().singleOrNull {
                it.isFile &&
                    it.name.startsWith("material-icons-extended-desktop-") &&
                    it.extension == "jar"
            } ?: throw GradleException("Expected one Material Icons Extended desktop JAR in $appLibDir")
            val coreJar = appLibDir.listFiles().orEmpty().singleOrNull {
                it.isFile &&
                    it.name.startsWith("material-icons-core-desktop-") &&
                    it.extension == "jar"
            } ?: throw GradleException("Expected one Material Icons Core desktop JAR in $appLibDir")

            val iconStyles = setOf("Filled", "Outlined", "Rounded", "Sharp", "TwoTone")
            val iconPackages = iconStyles.flatMap { style ->
                listOf(
                    "androidx/compose/material/icons/${style.lowercase()}",
                    "androidx/compose/material/icons/automirrored/${style.lowercase()}",
                )
            }.toSet()
            val requestedIcons = mutableSetOf<String>()
            fun addIcon(autoMirrored: Boolean, style: String, name: String) {
                val iconPackage = if (autoMirrored) {
                    "androidx/compose/material/icons/automirrored/${style.lowercase()}"
                } else {
                    "androidx/compose/material/icons/${style.lowercase()}"
                }
                requestedIcons += "$iconPackage/${name}Kt.class"
            }

            val iconReference = Regex(
                """Icons\.(AutoMirrored\.)?(Filled|Outlined|Rounded|Sharp|TwoTone)\.([A-Za-z0-9_]+)""",
            )
            val iconSources = project(":ui:app")
                .fileTree("src/commonMain/kotlin")
                .matching { include("**/*.kt") }
                .files +
                project.fileTree("src/main/kotlin")
                    .matching { include("**/*.kt") }
                    .files
            iconSources.forEach { source ->
                val sourceText = source.readText(Charsets.UTF_8)
                iconReference.findAll(sourceText).forEach { match ->
                    addIcon(
                        match.groupValues[1].isNotEmpty(),
                        match.groupValues[2],
                        match.groupValues[3],
                    )
                }
            }
            if (requestedIcons.isEmpty()) {
                throw GradleException("No Material Icons references found for the Linux distribution")
            }

            fun jarEntries(file: File): Set<String> =
                JarFile(file).use { jar ->
                    buildSet {
                        val entries = jar.entries()
                        while (entries.hasMoreElements()) {
                            add(entries.nextElement().name)
                        }
                    }
                }

            val coreEntries = jarEntries(coreJar)
            val extendedEntries = jarEntries(extendedJar)
            val missingIcons = requestedIcons.filterNot {
                it in coreEntries || it in extendedEntries
            }
            if (missingIcons.isNotEmpty()) {
                throw GradleException(
                    "Material Icons classes are missing from the desktop JARs: ${missingIcons.sorted()}",
                )
            }

            val requiredExtendedIcons = requestedIcons.filterTo(mutableSetOf()) { it !in coreEntries }
            fun iconClassPath(entryName: String): String? {
                if (!entryName.endsWith(".class")) return null
                val packagePath = entryName.substringBeforeLast('/', missingDelimiterValue = "")
                if (packagePath !in iconPackages) return null
                val className = entryName
                    .substringAfterLast('/')
                    .removeSuffix(".class")
                    .substringBefore('$')
                if (!className.endsWith("Kt")) return null
                return "$packagePath/$className.class"
            }

            val originalSize = extendedJar.length()
            val temporaryJar = File(extendedJar.parentFile, "${extendedJar.name}.trimmed")
            try {
                JarFile(extendedJar).use { input ->
                    JarOutputStream(temporaryJar.outputStream()).use { output ->
                        val entries = input.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val iconClass = iconClassPath(entry.name)
                            if (iconClass == null || iconClass in requiredExtendedIcons) {
                                val outputEntry = JarEntry(entry.name)
                                if (entry.time >= 0) outputEntry.time = entry.time
                                output.putNextEntry(outputEntry)
                                if (!entry.isDirectory) {
                                    input.getInputStream(entry).use { it.copyTo(output) }
                                }
                                output.closeEntry()
                            }
                        }
                    }
                }
                Files.move(
                    temporaryJar.toPath(),
                    extendedJar.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } finally {
                temporaryJar.delete()
            }

            val retainedEntries = jarEntries(extendedJar)
            val iconsMissingAfterTrim = requiredExtendedIcons.filterNot { it in retainedEntries }
            if (iconsMissingAfterTrim.isNotEmpty()) {
                throw GradleException(
                    "Material Icons classes were lost while trimming: ${iconsMissingAfterTrim.sorted()}",
                )
            }
            logger.lifecycle(
                "Trimmed Linux Material Icons Extended JAR: ${requestedIcons.size} referenced icons, " +
                    "${originalSize - extendedJar.length()} bytes removed",
            )
        }
    }
}
