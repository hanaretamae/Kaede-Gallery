package com.hanaretamae.kaede.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MpvVideoCommandTest {
    @Test
    fun embeddedPlaybackIsLimitedToSupportedDesktopPlatforms() {
        assertTrue(MpvVideoCommand.supportsEmbeddedPlayback("Linux"))
        assertTrue(MpvVideoCommand.supportsEmbeddedPlayback("Windows 11"))
        assertFalse(MpvVideoCommand.supportsEmbeddedPlayback("Mac OS X"))
    }

    @Test
    fun inAppPlaybackControlsAreEnabledOnlyWhereIpcIsAvailable() {
        assertTrue(MpvVideoCommand.supportsInAppControls("Linux"))
        assertFalse(MpvVideoCommand.supportsInAppControls("Windows 11"))
        assertFalse(MpvVideoCommand.supportsInAppControls("Mac OS X"))
    }

    @Test
    fun launchesOnlyTheSelectedLocalMediaWithoutUserConfigOrScripts() {
        val mediaPath = "-example vault/video clip.mp4"

        assertEquals(
            listOf(
                "mpv",
                "--no-config",
                "--no-load-scripts",
                "--no-ytdl",
                "--autoload-files=no",
                "--sub-auto=no",
                "--audio-file-auto=no",
                "--loop-file=no",
                "--force-window=yes",
                "--vo=x11",
                "--wid=42",
                "--",
                mediaPath,
            ),
            MpvVideoCommand.arguments(42, mediaPath, "Linux"),
        )
    }

    @Test
    fun windowsUsesNativeEmbeddedVideoOutput() {
        assertFalse("--vo=x11" in MpvVideoCommand.arguments(42, "video.mp4", "Windows"))
    }

    @Test
    fun linuxUsesOnlyTheAppOwnedIpcControlsInsteadOfMpvOsc() {
        val arguments = MpvVideoCommand.arguments(
            windowId = 42,
            mediaPath = "video.mp4",
            osName = "Linux",
            ipcSocketPath = "/tmp/kg-private/control.sock",
        )

        assertTrue("--osc=no" in arguments)
        assertTrue("--input-default-bindings=no" in arguments)
        assertTrue("--input-ipc-server=/tmp/kg-private/control.sock" in arguments)
    }

    @Test
    fun playbackDurationUsesClockFormatting() {
        assertEquals("01:05", formatDesktopVideoDuration(65f))
        assertEquals("01:02:03", formatDesktopVideoDuration(3_723f))
        assertEquals("00:00", formatDesktopVideoDuration(Float.NaN))
    }
}
