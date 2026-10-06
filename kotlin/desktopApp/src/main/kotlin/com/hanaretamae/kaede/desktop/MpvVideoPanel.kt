package com.hanaretamae.kaede.desktop

import com.sun.jna.Native
import java.awt.BorderLayout
import java.awt.Canvas
import java.awt.Color
import java.awt.Graphics
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

internal object MpvVideoCommand {
    fun arguments(windowId: Long, mediaPath: String): List<String> = listOf(
        "mpv",
        "--no-config",
        "--no-load-scripts",
        "--no-ytdl",
        "--autoload-files=no",
        "--sub-auto=no",
        "--audio-file-auto=no",
        "--force-window=yes",
        "--wid=$windowId",
        "--",
        mediaPath,
    )
}

internal class MpvVideoPanel : JPanel(BorderLayout()) {
    private val videoCanvas = VideoCanvas()
    private val status = JLabel("", SwingConstants.CENTER)
    private var player: Process? = null
    private var activePath: String? = null

    init {
        background = Color.BLACK
        status.foreground = Color.WHITE
        status.background = Color(0x18, 0x18, 0x18)
        status.isOpaque = true
        add(videoCanvas, BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
    }

    fun play(mediaPath: String?) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { play(mediaPath) }
            return
        }
        if (mediaPath == activePath) return
        stopPlayer()
        activePath = mediaPath
        if (mediaPath == null) {
            status.text = "Video location is unavailable."
            return
        }
        if (!System.getProperty("os.name").lowercase().contains("linux")) {
            status.text = "Embedded mpv playback is available on Linux only."
            return
        }
        val path = try {
            Path.of(mediaPath)
        } catch (_: RuntimeException) {
            status.text = "The selected video is unavailable."
            return
        }
        val readableVideo = try {
            Files.isRegularFile(path) && Files.isReadable(path)
        } catch (_: SecurityException) {
            false
        }
        if (!readableVideo) {
            status.text = "The selected video is unavailable."
            return
        }
        val windowId = try {
            if (!videoCanvas.isDisplayable) 0L else Native.getComponentID(videoCanvas)
        } catch (_: RuntimeException) {
            0L
        } catch (_: UnsatisfiedLinkError) {
            0L
        }
        if (windowId == 0L) {
            status.text = "Embedded video playback is unavailable in this window system."
            return
        }
        var process: Process? = null
        try {
            val startedProcess = ProcessBuilder(MpvVideoCommand.arguments(windowId, path.toString()))
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            process = startedProcess
            startedProcess.outputStream.close()
            player = startedProcess
            status.text = ""
            Thread({
                val exitCode = startedProcess.waitFor()
                SwingUtilities.invokeLater {
                    if (player === startedProcess) {
                        player = null
                        status.text = if (exitCode == 0) {
                            "Video playback finished."
                        } else {
                            "mpv could not play the selected video."
                        }
                    }
                }
            }, "kaede-mpv-exit").apply {
                isDaemon = true
                start()
            }
        } catch (_: IOException) {
            process?.destroyForcibly()
            if (player === process) player = null
            status.text = "mpv could not be started. Check that mpv is installed."
        } catch (_: SecurityException) {
            process?.destroyForcibly()
            if (player === process) player = null
            status.text = "Video playback could not be started."
        }
    }

    fun close() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { close() }
            return
        }
        activePath = null
        stopPlayer()
    }

    private fun stopPlayer() {
        player?.let { process ->
            process.destroy()
            Thread({
                if (!process.waitFor(1, TimeUnit.SECONDS)) process.destroyForcibly()
            }, "kaede-mpv-shutdown").apply {
                isDaemon = true
                start()
            }
        }
        player = null
    }

    private class VideoCanvas : Canvas() {
        init {
            background = Color.BLACK
        }

        override fun paint(graphics: Graphics) {
            graphics.color = background
            graphics.fillRect(0, 0, width, height)
        }

        override fun update(graphics: Graphics) = paint(graphics)
    }
}
