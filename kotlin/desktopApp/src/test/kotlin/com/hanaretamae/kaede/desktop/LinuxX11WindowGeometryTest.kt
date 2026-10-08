package com.hanaretamae.kaede.desktop

import java.awt.Dimension
import java.awt.Insets
import kotlin.test.Test
import kotlin.test.assertEquals

class LinuxX11WindowGeometryTest {
    @Test
    fun x11ClientSizeConvertsToAwtWindowSizeWithoutDroppingInsets() {
        assertEquals(
            Dimension(949, 1_157),
            awtWindowSizeFromX11(
                dimensions = X11WindowDimensions(939, 1_127),
                scaleX = 1.0,
                scaleY = 1.0,
                insets = Insets(25, 5, 5, 5),
            ),
        )
    }
}
