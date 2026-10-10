package com.hanaretamae.kaede.ui.gallery

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KaedeGalleryThemeTest {
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
        assertEquals(base.secondary, pureBlack.secondary)
        assertEquals(base.tertiary, pureBlack.tertiary)
        assertEquals(base.outlineVariant, pureBlack.outlineVariant)
        assertEquals(base.onSurface, pureBlack.onSurface)
    }
}
