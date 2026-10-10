package com.hanaretamae.kaede.ui.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryScreenTest {
    @Test
    fun adaptiveGalleryKeepsTwoColumnsAndUsesFourAtNarrowDesktopWidth() {
        assertEquals(2, galleryAdaptiveColumnCount(360f))
        assertEquals(4, galleryAdaptiveColumnCount(866f))
        assertEquals(5, galleryAdaptiveColumnCount(1_100f))
    }
}
