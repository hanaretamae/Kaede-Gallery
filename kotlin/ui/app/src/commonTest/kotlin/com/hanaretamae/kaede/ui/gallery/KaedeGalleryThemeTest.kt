package com.hanaretamae.kaede.ui.gallery

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KaedeGalleryThemeTest {
    @Test
    fun settingsListUsesSharedPageBackgroundAndMaterialSurfaces() {
        val light = kaedeFallbackColorScheme(darkTheme = false)
        val dark = kaedeFallbackColorScheme(darkTheme = true)
        val pureBlack = pureBlackColorScheme(dark)

        assertEquals(light.background, settingsListBackdropColor(light))
        assertEquals(light.surfaceContainer, settingsListItemColor(light))
        assertNotEquals(settingsListItemColor(light), settingsListBackdropColor(light))
        assertTrue(contrastRatio(light.onSurface, settingsListItemColor(light)) >= 4.5f)
        assertTrue(contrastRatio(light.onSurface, settingsListBackdropColor(light)) >= 4.5f)
        assertEquals(dark.background, settingsListBackdropColor(dark))
        assertEquals(dark.surfaceContainer, settingsListItemColor(dark))
        assertNotEquals(settingsListItemColor(dark), settingsListBackdropColor(dark))
        assertTrue(contrastRatio(dark.onSurface, settingsListItemColor(dark)) >= 4.5f)
        assertTrue(contrastRatio(dark.onSurface, settingsListBackdropColor(dark)) >= 4.5f)
        assertEquals(Color.Black, settingsListBackdropColor(pureBlack))
        assertEquals(dark.surfaceContainer, settingsListItemColor(pureBlack))
        assertTrue(contrastRatio(dark.onSurface, settingsListBackdropColor(pureBlack)) >= 4.5f)

        val dynamicLight = dynamicColorScheme(
            seedColor = Color(0xFF336699),
            isDark = false,
            style = PaletteStyle.Expressive,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
        assertEquals(dynamicLight.background, settingsListBackdropColor(dynamicLight))
        assertEquals(dynamicLight.surfaceContainer, settingsListItemColor(dynamicLight))
        assertNotEquals(settingsListItemColor(dynamicLight), settingsListBackdropColor(dynamicLight))

        val dynamicDark = dynamicColorScheme(
            seedColor = Color(0xFF336699),
            isDark = true,
            style = PaletteStyle.Expressive,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
        assertEquals(dynamicDark.background, settingsListBackdropColor(dynamicDark))
        assertEquals(dynamicDark.surfaceContainer, settingsListItemColor(dynamicDark))
        assertNotEquals(settingsListItemColor(dynamicDark), settingsListBackdropColor(dynamicDark))
        assertTrue(contrastRatio(dynamicDark.onSurface, settingsListItemColor(dynamicDark)) >= 4.5f)
    }

    @Test
    fun fallbackSchemesUseExpressive2025PaletteInLightAndDarkThemes() {
        for (darkTheme in listOf(false, true)) {
            val expected = dynamicColorScheme(
                seedColor = KaedeSeedColor,
                isDark = darkTheme,
                style = PaletteStyle.Expressive,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
            )
            val actual = kaedeFallbackColorScheme(darkTheme)
            assertEquals(expected.primary, actual.primary)
            assertEquals(expected.secondary, actual.secondary)
            assertEquals(expected.tertiary, actual.tertiary)
            assertEquals(expected.primaryContainer, actual.primaryContainer)
            assertEquals(expected.secondaryContainer, actual.secondaryContainer)
            assertEquals(expected.tertiaryContainer, actual.tertiaryContainer)
            assertEquals(expected.surfaceContainerHigh, actual.surfaceContainerHigh)
            assertEquals(expected.primaryFixed, actual.primaryFixed)
            assertEquals(expected.onPrimaryFixedVariant, actual.onPrimaryFixedVariant)
        }
    }

    @Test
    fun resolvesSystemColorFallbackBrightnessAndPureBlackCombinations() {
        for (darkTheme in listOf(false, true)) {
            val fallback = kaedeFallbackColorScheme(darkTheme)
            val system = fallback.copy(primary = Color.Green, background = Color.Yellow)
            for (useSystemColor in listOf(false, true)) {
                for (systemAvailable in listOf(false, true)) {
                    for (pureBlack in listOf(false, true)) {
                        val appearance = AppearanceSettings(
                            useSystemColor = useSystemColor,
                            pureBlack = pureBlack,
                        )
                        val selectedBase = if (useSystemColor && systemAvailable) system else fallback
                        val actual = resolveKaedeColorScheme(
                            appearance = appearance,
                            darkTheme = darkTheme,
                            systemColorScheme = system.takeIf { systemAvailable },
                            fallbackColorScheme = fallback,
                        )

                        assertEquals(selectedBase.primary, actual.primary)
                        assertEquals(
                            if (darkTheme && pureBlack) Color.Black else selectedBase.background,
                            actual.background,
                        )
                        assertEquals(selectedBase.surface, actual.surface)
                        assertEquals(selectedBase.secondaryContainer, actual.secondaryContainer)
                        assertEquals(selectedBase.onSecondaryContainer, actual.onSecondaryContainer)
                        assertEquals(selectedBase.primaryFixed, actual.primaryFixed)
                        assertEquals(
                            selectedBase.onTertiaryFixedVariant,
                            actual.onTertiaryFixedVariant,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun pureBlackChangesOnlyBackgroundAndKeepsMaterialYouRoles() {
        val base = kaedeFallbackColorScheme(darkTheme = true).copy(primary = Color.Green)
        val pureBlack = pureBlackColorScheme(base)

        assertEquals(Color.Black, pureBlack.background)
        assertEquals(base.surface, pureBlack.surface)
        assertEquals(base.surfaceContainerLowest, pureBlack.surfaceContainerLowest)
        assertEquals(base.surfaceContainer, pureBlack.surfaceContainer)
        assertNotEquals(Color.Black, pureBlack.surfaceBright)
        assertNotEquals(Color.Black, pureBlack.surfaceContainerHigh)
        assertNotEquals(Color.Black, pureBlack.surfaceContainerHighest)
        assertEquals(base.surfaceBright, pureBlack.surfaceBright)
        assertEquals(base.surfaceDim, pureBlack.surfaceDim)
        assertEquals(base.surfaceVariant, pureBlack.surfaceVariant)
        assertEquals(base.surfaceContainerLow, pureBlack.surfaceContainerLow)
        assertEquals(base.surfaceContainerHigh, pureBlack.surfaceContainerHigh)
        assertEquals(base.surfaceContainerHighest, pureBlack.surfaceContainerHighest)
        assertEquals(base.primary, pureBlack.primary)
        assertEquals(base.primaryContainer, pureBlack.primaryContainer)
        assertEquals(base.primaryFixed, pureBlack.primaryFixed)
        assertEquals(base.onPrimaryFixedVariant, pureBlack.onPrimaryFixedVariant)
        assertEquals(base.secondary, pureBlack.secondary)
        assertEquals(base.secondaryFixed, pureBlack.secondaryFixed)
        assertEquals(base.tertiary, pureBlack.tertiary)
        assertEquals(base.tertiaryFixed, pureBlack.tertiaryFixed)
        assertEquals(base.outlineVariant, pureBlack.outlineVariant)
        assertEquals(base.onSurface, pureBlack.onSurface)
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
