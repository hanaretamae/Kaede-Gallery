package com.hanaretamae.kaede.desktop

import java.nio.file.Path
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class MpvVideoPanelIntegrationTest {
    @Test
    fun embeddedPlayerSupportsPauseSeekLoopAndRestartAfterEof() {
        val fixture = System.getenv("KAEDE_TEST_VIDEO")
        assumeTrue("Requires a generated fictional video and an X11 display", fixture != null)
        val path = Path.of(requireNotNull(fixture))
        lateinit var panel: MpvVideoPanel
        lateinit var frame: JFrame
        SwingUtilities.invokeAndWait {
            panel = MpvVideoPanel()
            frame = JFrame("Kaede fictional playback test").apply {
                contentPane.add(panel)
                setSize(400, 300)
                isVisible = true
            }
            panel.play(path.toString())
        }
        fun awaitState(predicate: (DesktopVideoPlaybackState) -> Boolean) {
            val deadline = System.nanoTime() + 8_000_000_000L
            while (!predicate(panel.playbackState.value) && System.nanoTime() < deadline) {
                Thread.sleep(20)
            }
            assertTrue(predicate(panel.playbackState.value), panel.playbackState.value.toString())
        }
        try {
            awaitState { it.connected && it.playing && it.durationSeconds > 0f }
            panel.togglePlayback()
            awaitState { !it.playing }
            panel.setPlaybackSpeed(1.75f)
            awaitState { it.playbackSpeed == 1.75f }
            panel.setPlaybackSpeed(2f)
            awaitState { it.playbackSpeed == 2f }
            panel.setPlaybackSpeed(1f)
            awaitState { it.playbackSpeed == 1f }
            panel.seekTo(1f)
            awaitState { abs(it.positionSeconds - 1f) < 0.2f }
            panel.togglePlayback()
            awaitState { it.playing }
            panel.setLooping(true)
            awaitState { it.looping }
            Thread.sleep(3_000)
            assertTrue(panel.playbackState.value.connected)
            assertTrue(panel.playbackState.value.playing)
            assertTrue(!panel.playbackState.value.ended)
            panel.setLooping(false)
            awaitState {
                !it.looping && it.ended &&
                    abs(it.positionSeconds - it.durationSeconds) < 0.05f
            }
            panel.togglePlayback()
            awaitState { it.playing && !it.ended && it.positionSeconds < 1f }
            awaitState { it.ended }
            panel.seekTo(0.5f)
            awaitState { !it.ended && abs(it.positionSeconds - 0.5f) < 0.2f }
            panel.togglePlayback()
            awaitState { it.playing }
        } finally {
            SwingUtilities.invokeAndWait {
                panel.close()
                frame.dispose()
            }
        }
    }
}
