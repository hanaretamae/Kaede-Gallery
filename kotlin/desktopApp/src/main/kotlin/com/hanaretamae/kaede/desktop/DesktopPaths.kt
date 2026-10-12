package com.hanaretamae.kaede.desktop

import java.nio.file.Files
import java.nio.file.Path

// Rust's canonicalize returns Windows verbatim paths that java.nio rejects.
internal fun stripWindowsVerbatimPrefix(location: String): String = when {
    location.startsWith("\\\\?\\UNC\\") -> "\\\\" + location.substring(8)
    location.startsWith("\\\\?\\") && location.length > 6 && location[5] == ':' ->
        location.substring(4)
    else -> location
}

internal fun desktopPathOf(location: String): Path = Path.of(stripWindowsVerbatimPrefix(location))

// Windows distributions put tools beside the jpackage launcher, not inside its runtime.
internal fun desktopToolCommand(
    name: String,
    osName: String = System.getProperty("os.name"),
    javaHome: String? = System.getProperty("java.home"),
    launcherPath: String? = System.getProperty("jpackage.app-path"),
): String {
    val windows = osName.lowercase().contains("windows")
    val fileName = if (windows) "$name.exe" else name
    val bundled = try {
        if (windows) {
            val launcherRoot = launcherPath?.let { Path.of(it).toAbsolutePath() }
                ?.takeIf { it.fileName.toString().endsWith(".exe", ignoreCase = true) && Files.isRegularFile(it) }
                ?.parent
            val runtimeRoot = javaHome?.let { Path.of(it).toAbsolutePath() }
                ?.takeIf { it.fileName?.toString().equals("runtime", ignoreCase = true) }
                ?.parent
            sequenceOf(launcherRoot, runtimeRoot)
                .filterNotNull()
                .map { it.resolve("tools").resolve(fileName) }
                .firstOrNull { Files.isRegularFile(it) }
        } else {
            javaHome?.let { Path.of(it).toAbsolutePath().parent?.resolve("tools")?.resolve(fileName) }
                ?.takeIf { Files.isRegularFile(it) }
        }
    } catch (_: RuntimeException) {
        null
    }
    return bundled?.toString() ?: name
}
