package com.hanaretamae.kaede.desktop

import java.nio.file.Path

// Rust's canonicalize returns Windows verbatim paths that java.nio rejects.
internal fun stripWindowsVerbatimPrefix(location: String): String = when {
    location.startsWith("\\\\?\\UNC\\") -> "\\\\" + location.substring(8)
    location.startsWith("\\\\?\\") && location.length > 6 && location[5] == ':' ->
        location.substring(4)
    else -> location
}

internal fun desktopPathOf(location: String): Path = Path.of(stripWindowsVerbatimPrefix(location))

// Prefers a tool bundled in <app>/tools (Windows release) over the PATH.
internal fun desktopToolCommand(
    name: String,
    osName: String = System.getProperty("os.name"),
): String {
    val windows = osName.lowercase().contains("windows")
    val fileName = if (windows) "$name.exe" else name
    val bundled = try {
        System.getProperty("java.home")
            ?.let { Path.of(it).toAbsolutePath().parent?.resolve("tools")?.resolve(fileName) }
            ?.takeIf { java.nio.file.Files.isRegularFile(it) }
    } catch (_: RuntimeException) {
        null
    }
    return bundled?.toString() ?: name
}
