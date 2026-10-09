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
                "--loop-file=inf",
                "--force-window=yes",
                "--wid=42",
                "--",
                mediaPath,
            ),
            MpvVideoCommand.arguments(42, mediaPath),
        )
    }
}
