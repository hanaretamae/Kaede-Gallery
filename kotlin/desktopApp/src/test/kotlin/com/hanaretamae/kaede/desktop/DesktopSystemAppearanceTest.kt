package com.hanaretamae.kaede.desktop

import androidx.compose.ui.graphics.Color
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
    fun createsCompleteExpressive2025SchemeFromSystemAccent() {
        val accent = Color(0.22f, 0.41f, 0.73f)
        val scheme = requireNotNull(desktopSystemColorScheme(accent, darkTheme = false))

        assertNotEquals(scheme.primary, scheme.secondary)
        assertNotEquals(scheme.secondary, scheme.tertiary)
        assertTrue(scheme.primary != Color.Unspecified)
        assertNull(desktopSystemColorScheme(null, darkTheme = false))
    }
}
