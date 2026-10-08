package com.hanaretamae.kaede.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopWindowSizingTest {
    @Test
    fun minimumWindowSizeUsesThreeByFourAspectRatio() {
        assertEquals(
            0f,
            DESKTOP_MINIMUM_WINDOW_SIZE.width.value * 4f -
                DESKTOP_MINIMUM_WINDOW_SIZE.height.value * 3f,
        )
    }
}
