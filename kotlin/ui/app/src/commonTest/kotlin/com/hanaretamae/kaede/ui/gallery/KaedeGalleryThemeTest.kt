package com.hanaretamae.kaede.ui.gallery

import androidx.compose.ui.graphics.Color
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KaedeGalleryThemeTest {
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
}
