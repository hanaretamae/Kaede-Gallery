package com.hanaretamae.kaede.ui.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryScreenTest {
    @Test
    fun adaptiveGalleryKeepsMinimumColumnsAndRespectsCellWidth() {
        assertEquals(2, galleryAdaptiveColumnCount(360f))
        assertEquals(4, galleryAdaptiveColumnCount(866f))
        assertEquals(5, galleryAdaptiveColumnCount(1_100f))
        assertEquals(3, galleryAdaptiveColumnCount(700f))
        assertEquals(4, galleryAdaptiveColumnCount(700f, 160f))
        assertEquals(6, galleryAdaptiveColumnCount(1_100f, 160f))
        assertEquals(2, galleryAdaptiveColumnCount(700f, 0f))
    }

    @Test
    fun desktopGalleryFitsFourColumnsAtReportedWindowWidthAcrossDisplayScales() {
        listOf(1f, 1.2f, 1.25f, 1.5f, 2f).forEach { density ->
            assertEquals(
                4,
                galleryAdaptiveColumnCount(866f / density, 180f / density),
                "Expected four columns at 866 physical pixels with density $density.",
            )
        }
    }
}
