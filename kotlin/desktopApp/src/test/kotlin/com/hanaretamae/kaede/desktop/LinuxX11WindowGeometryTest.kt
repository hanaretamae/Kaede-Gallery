package com.hanaretamae.kaede.desktop

import java.awt.Dimension
import java.awt.Insets
import java.awt.geom.AffineTransform
import kotlin.test.Test
import kotlin.test.assertEquals

class LinuxX11WindowGeometryTest {
    @Test
    fun x11ClientSizeConvertsToAwtWindowSizeWithoutDroppingInsets() {
        assertEquals(
            Dimension(949, 1_157),
            awtWindowSizeFromX11(
                dimensions = X11WindowDimensions(939, 1_127),
                defaultTransform = AffineTransform(),
                insets = Insets(25, 5, 5, 5),
            ),
        )
    }

    @Test
    fun x11ClientSizeConvertsAcrossRotationsAndScales() {
        val logicalWidth = 1_600
        val logicalHeight = 1_000
        val rotations = listOf(0.0, 45.0, 90.0, 180.0, 270.0)
        val scales = listOf(
            1.0 to 1.0,
            1.2 to 1.2,
            1.5 to 1.25,
            0.8 to 2.0,
        )

        rotations.forEach { rotation ->
            scales.forEach { (scaleX, scaleY) ->
                val transform = AffineTransform
                    .getRotateInstance(Math.toRadians(rotation))
                    .apply { scale(scaleX, scaleY) }
                val dimensions = X11WindowDimensions(
                    width = (logicalWidth * scaleX).toInt(),
                    height = (logicalHeight * scaleY).toInt(),
                )

                assertEquals(
                    Dimension(logicalWidth, logicalHeight),
                    awtWindowSizeFromX11(
                        dimensions = dimensions,
                        defaultTransform = transform,
                        insets = Insets(0, 0, 0, 0),
                    ),
                    "Expected logical size at rotation=$rotation and scale=$scaleX,$scaleY.",
                )
            }
        }
    }
}
