package com.hanaretamae.kaede.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class MpvVideoCommandTest {
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
                "--force-window=yes",
                "--wid=42",
                "--",
                mediaPath,
            ),
            MpvVideoCommand.arguments(42, mediaPath),
        )
    }
}
