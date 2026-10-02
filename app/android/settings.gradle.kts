pluginManagement {
    val flutterSdkPath =
        run {
            val properties = java.util.Properties()
            file("local.properties").inputStream().use { properties.load(it) }
            val flutterSdkPath = properties.getProperty("flutter.sdk")
            require(flutterSdkPath != null) { "flutter.sdk not set in local.properties" }
            flutterSdkPath
        }

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
}

findProject(":integration_test")?.let { integrationTest ->
    if (!integrationTest.projectDir.canWrite()) {
        val source = integrationTest.projectDir.toPath()
        val writable = rootDir.toPath().resolve("build/writable-integration-test")
        java.nio.file.Files.createDirectories(writable)
        java.nio.file.Files.walk(source).use { paths ->
            paths.forEach { path ->
                val destination = writable.resolve(source.relativize(path))
                if (java.nio.file.Files.isDirectory(path)) {
                    java.nio.file.Files.createDirectories(destination)
                } else {
                    java.nio.file.Files.createDirectories(destination.parent)
                    java.nio.file.Files.copy(
                        path.toRealPath(),
                        destination,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    )
                }
            }
        }
        integrationTest.projectDir = writable.toFile()
    }
}

include(":app")
