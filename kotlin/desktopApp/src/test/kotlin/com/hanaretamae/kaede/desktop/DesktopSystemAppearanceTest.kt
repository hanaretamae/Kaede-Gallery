package com.hanaretamae.kaede.desktop

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopSystemAppearanceTest {
    @Test
    fun parsesPortalColorSchemeValues() {
        assertEquals(true, parsePortalDarkTheme("<uint32 1>"))
        assertEquals(false, parsePortalDarkTheme("<uint32 2>"))
        assertNull(parsePortalDarkTheme("<uint32 0>"))
        assertNull(parsePortalDarkTheme("Error: unknown setting"))
    }

    @Test
    fun parsesPortalSrgbAccentTuple() {
        assertEquals(
            Color(0.22f, 0.41f, 0.73f),
            parsePortalAccentColor("(<0.22, 0.41, 0.73>,)"),
        )
        assertNull(parsePortalAccentColor("(<0.22, 0.41>,)"))
        assertNull(parsePortalAccentColor("(<0.22, 0.41, 1.01>,)"))
        assertNull(parsePortalAccentColor("Failed to read setting"))
    }

    @Test
    fun parsesGtkNamedAndHexAccentColors() {
        assertEquals(
            Color(0x35 / 255f, 0x84 / 255f, 0xe4 / 255f),
            parseGtkAccentColor("'blue'"),
        )
        assertEquals(
            Color(0x21 / 255f, 0x90 / 255f, 0xa4 / 255f),
            parseGtkAccentColor("'teal'"),
        )
        assertEquals(
            Color(0x12 / 255f, 0xab / 255f, 0xef / 255f),
            parseGtkAccentColor("'#12abef'"),
        )
        assertNull(parseGtkAccentColor("'unknown'"))
        assertNull(parseGtkAccentColor("not a color"))
        assertNull(parseGtkAccentColor(null))
    }

    @Test
    fun createsCompleteExpressive2025SchemeFromSystemAccent() {
        val accent = Color(0.22f, 0.41f, 0.73f)
        for (darkTheme in listOf(false, true)) {
            val scheme = requireNotNull(desktopSystemColorScheme(accent, darkTheme))
            val expected = dynamicColorScheme(
                seedColor = accent,
                isDark = darkTheme,
                style = PaletteStyle.Expressive,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
            )

            assertEquals(expected.primary, scheme.primary)
            assertEquals(expected.secondary, scheme.secondary)
            assertEquals(expected.tertiary, scheme.tertiary)
            assertNotEquals(scheme.primary, scheme.secondary)
            assertNotEquals(scheme.secondary, scheme.tertiary)
            assertTrue(scheme.primary != Color.Unspecified)
        }
        assertNull(desktopSystemColorScheme(null, darkTheme = false))
        assertNull(desktopSystemColorScheme(null, darkTheme = true))
    }
}
