package com.hanaretamae.kaede.desktop

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val PORTAL_READ_TIMEOUT_MILLIS = 750L
private val PORTAL_UINT32_PATTERN = Regex("""\buint32\s+(\d+)\b""")
private val PORTAL_NUMBER_PATTERN = Regex("""(?<![\w.])[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?![\w.])""")

internal data class DesktopSystemAppearance(
    val darkTheme: Boolean? = null,
    val accentColor: Color? = null,
)

internal fun readDesktopSystemAppearance(): DesktopSystemAppearance {
    if (isWindowsDesktop()) {
        val personalize = """HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize"""
        val dwm = """HKCU\Software\Microsoft\Windows\DWM"""
        return DesktopSystemAppearance(
            darkTheme = parseWindowsRegistryDword(
                readWindowsRegistryValue(personalize, "AppsUseLightTheme"),
            )?.let { it == 0L },
            accentColor = parseWindowsAccentColor(
                parseWindowsRegistryDword(readWindowsRegistryValue(dwm, "AccentColor")),
            ),
        )
    }
    if (!isLinuxDesktop()) return DesktopSystemAppearance()
    val darkTheme = parsePortalDarkTheme(readPortalAppearanceSetting("color-scheme"))
    val portalAccent = parsePortalAccentColor(readPortalAppearanceSetting("accent-color"))
    val accentColor = portalAccent ?: parseGtkAccentColor(readGtkAccentColorSetting())
    return DesktopSystemAppearance(darkTheme, accentColor)
}

internal fun parsePortalDarkTheme(value: String?): Boolean? {
    val scheme = value?.let { PORTAL_UINT32_PATTERN.find(it)?.groupValues?.get(1)?.toIntOrNull() }
    return when (scheme) {
        1 -> true
        2 -> false
        else -> null
    }
}

internal fun parsePortalAccentColor(value: String?): Color? {
    if (value == null || '(' !in value || ')' !in value) return null
    val components = PORTAL_NUMBER_PATTERN.findAll(value).mapNotNull { match ->
        match.value.toFloatOrNull()
    }.toList()
    if (components.size != 3 || components.any { it !in 0f..1f }) return null
    return Color(components[0], components[1], components[2])
}

internal fun parseGtkAccentColor(value: String?): Color? {
    val normalized = value?.trim()?.removeSurrounding("'")?.removeSurrounding("\"")
        ?.lowercase() ?: return null
    val color = GTK_ACCENT_COLORS[normalized] ?: run {
        val hex = normalized.removePrefix("#")
        if (hex.length != 6 || !hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return null
        }
        hex.toLongOrNull(16)?.toInt() ?: return null
    }
    return Color(
        red = ((color shr 16) and 0xff) / 255f,
        green = ((color shr 8) and 0xff) / 255f,
        blue = (color and 0xff) / 255f,
    )
}

internal fun desktopSystemColorScheme(accent: Color?, darkTheme: Boolean): ColorScheme? =
    accent?.let {
        dynamicColorScheme(
            seedColor = it,
            isDark = darkTheme,
            style = PaletteStyle.Expressive,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
    }

private fun readPortalAppearanceSetting(key: String): String? {
    val process = try {
        ProcessBuilder(
            "gdbus",
            "call",
            "--session",
            "--dest",
            "org.freedesktop.portal.Desktop",
            "--object-path",
            "/org/freedesktop/portal/desktop",
            "--method",
            "org.freedesktop.portal.Settings.ReadOne",
            "org.freedesktop.appearance",
            key,
        ).redirectErrorStream(true).start()
    } catch (_: IOException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    return try {
        if (!process.waitFor(PORTAL_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            return null
        }
        if (process.exitValue() == 0) {
            process.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
    } catch (_: InterruptedException) {
        process.destroyForcibly()
        Thread.currentThread().interrupt()
        null
    } catch (_: IOException) {
        null
    }
}

private fun readGtkAccentColorSetting(): String? {
    val process = try {
        ProcessBuilder(
            "gsettings",
            "get",
            "org.gnome.desktop.interface",
            "accent-color",
        ).redirectErrorStream(true).start()
    } catch (_: IOException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    return try {
        if (!process.waitFor(PORTAL_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            return null
        }
        if (process.exitValue() == 0) {
            process.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
    } catch (_: InterruptedException) {
        process.destroyForcibly()
        Thread.currentThread().interrupt()
        null
    } catch (_: IOException) {
        null
    }
}

internal fun parseWindowsRegistryDword(output: String?): Long? =
    output?.let { Regex("""REG_DWORD\s+0x([0-9a-fA-F]+)""").find(it)?.groupValues?.get(1) }
        ?.toLongOrNull(16)

// DWM stores the accent as 0xAABBGGRR.
internal fun parseWindowsAccentColor(value: Long?): Color? {
    if (value == null) return null
    val red = (value and 0xff).toInt()
    val green = ((value shr 8) and 0xff).toInt()
    val blue = ((value shr 16) and 0xff).toInt()
    return Color(red, green, blue)
}

private fun readWindowsRegistryValue(key: String, name: String): String? {
    val process = try {
        ProcessBuilder("reg.exe", "query", key, "/v", name).redirectErrorStream(true).start()
    } catch (_: IOException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    return try {
        if (!process.waitFor(PORTAL_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            return null
        }
        if (process.exitValue() == 0) process.inputStream.bufferedReader().use { it.readText() } else null
    } catch (_: InterruptedException) {
        process.destroyForcibly()
        Thread.currentThread().interrupt()
        null
    } catch (_: IOException) {
        null
    }
}

private val GTK_ACCENT_COLORS = mapOf(
    "default" to 0x3584e4,
    "blue" to 0x3584e4,
    "teal" to 0x2190a4,
    "green" to 0x3a944a,
    "yellow" to 0xc88800,
    "orange" to 0xed5b00,
    "red" to 0xe62d42,
    "pink" to 0xd56199,
    "purple" to 0x9141ac,
    "slate" to 0x6f8396,
)
