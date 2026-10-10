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
    if (!isLinuxDesktop()) return DesktopSystemAppearance()
    val darkTheme = parsePortalDarkTheme(readPortalAppearanceSetting("color-scheme"))
    val accentColor = parsePortalAccentColor(readPortalAppearanceSetting("accent-color"))
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
