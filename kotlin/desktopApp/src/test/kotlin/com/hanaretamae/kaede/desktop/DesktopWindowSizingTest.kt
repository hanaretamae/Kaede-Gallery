package com.hanaretamae.kaede.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopWindowSizingTest {
    @Test
    fun defaultWindowStartsAtCompactDpSize() {
        assertEquals(1_000f, DESKTOP_DEFAULT_WINDOW_SIZE.width.value)
        assertEquals(700f, DESKTOP_DEFAULT_WINDOW_SIZE.height.value)
    }

    @Test
    fun minimumWindowSizeAllowsCompactWindowWithoutAspectRatioLock() {
        assertEquals(400f, DESKTOP_MINIMUM_WINDOW_SIZE.width.value)
        assertEquals(500f, DESKTOP_MINIMUM_WINDOW_SIZE.height.value)
    }
}
