package com.hanaretamae.kaede.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.NativeLongByReference
import java.awt.Dimension
import java.awt.Insets
import kotlin.math.roundToInt

internal data class X11WindowDimensions(
    val width: Int,
    val height: Int,
)

internal fun awtWindowSizeFromX11(
    dimensions: X11WindowDimensions,
    scaleX: Double,
    scaleY: Double,
    insets: Insets,
): Dimension {
    require(scaleX.isFinite() && scaleX > 0.0)
    require(scaleY.isFinite() && scaleY > 0.0)
    return Dimension(
        (dimensions.width / scaleX).roundToInt() + insets.left + insets.right,
        (dimensions.height / scaleY).roundToInt() + insets.top + insets.bottom,
    )
}

internal class LinuxX11WindowGeometry private constructor(
    private val x11: X11Api,
    private val display: Pointer,
    private val window: NativeLong,
) : AutoCloseable {
    fun dimensions(): X11WindowDimensions? {
        val root = NativeLongByReference()
        val x = IntByReference()
        val y = IntByReference()
        val width = IntByReference()
        val height = IntByReference()
        val borderWidth = IntByReference()
        val depth = IntByReference()
        if (
            x11.XGetGeometry(
                display,
                window,
                root,
                x,
                y,
                width,
                height,
                borderWidth,
                depth,
            ) == 0
        ) {
            return null
        }
        if (width.value <= 0 || height.value <= 0) return null
        return X11WindowDimensions(width.value, height.value)
    }

    override fun close() {
        x11.XCloseDisplay(display)
    }

    companion object {
        fun open(windowId: Long): LinuxX11WindowGeometry? {
            if (!System.getProperty("os.name").startsWith("Linux") || windowId == 0L) {
                return null
            }
            val x11 = try {
                Native.load("X11", X11Api::class.java)
            } catch (_: LinkageError) {
                return null
            } catch (_: SecurityException) {
                return null
            }
            val display = try {
                x11.XOpenDisplay(null)
            } catch (_: LinkageError) {
                null
            } catch (_: SecurityException) {
                null
            } ?: return null
            return LinuxX11WindowGeometry(x11, display, NativeLong(windowId))
        }
    }
}

private interface X11Api : Library {
    fun XOpenDisplay(displayName: String?): Pointer?

    fun XGetGeometry(
        display: Pointer,
        drawable: NativeLong,
        rootReturn: NativeLongByReference,
        xReturn: IntByReference,
        yReturn: IntByReference,
        widthReturn: IntByReference,
        heightReturn: IntByReference,
        borderWidthReturn: IntByReference,
        depthReturn: IntByReference,
    ): Int

    fun XCloseDisplay(display: Pointer): Int
}
