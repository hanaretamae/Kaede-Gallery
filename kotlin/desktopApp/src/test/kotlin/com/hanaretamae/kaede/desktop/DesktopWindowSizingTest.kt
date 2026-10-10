package com.hanaretamae.kaede.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopWindowSizingTest {
    @Test
    fun minimumWindowSizeAllowsCompactWindowWithoutAspectRatioLock() {
        assertEquals(400f, DESKTOP_MINIMUM_WINDOW_SIZE.width.value)
        assertEquals(500f, DESKTOP_MINIMUM_WINDOW_SIZE.height.value)
    }
}
