package com.hanaretamae.kaede.desktop

import com.sun.jna.Native
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.awt.BorderLayout
import java.awt.Canvas
import java.awt.Color
import java.awt.Graphics
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.net.StandardProtocolFamily
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

internal object MpvVideoCommand {
    fun supportsEmbeddedPlayback(osName: String): Boolean {
        val normalizedName = osName.lowercase()
        return normalizedName.contains("linux") || normalizedName.contains("windows")
    }

    fun supportsInAppControls(osName: String = System.getProperty("os.name")): Boolean =
        osName.lowercase().contains("linux")

    fun arguments(
        windowId: Long,
        mediaPath: String,
        osName: String = System.getProperty("os.name"),
        ipcSocketPath: String? = null,
    ): List<String> = buildList {
        add("mpv")
        add("--no-config")
        add("--no-load-scripts")
        add("--no-ytdl")
        add("--autoload-files=no")
        add("--sub-auto=no")
        add("--audio-file-auto=no")
        add("--loop-file=no")
        add("--force-window=yes")
        if (osName.lowercase().contains("linux")) add("--vo=x11")
        if (ipcSocketPath != null) {
            add("--osc=no")
            add("--input-default-bindings=no")
            add("--input-ipc-server=$ipcSocketPath")
        }
        add("--wid=$windowId")
        add("--")
        add(mediaPath)
    }
}

internal data class DesktopVideoPlaybackState(
    val connected: Boolean = false,
    val playing: Boolean = false,
    val looping: Boolean = false,
    val muted: Boolean = false,
    val volume: Float = 100f,
    val positionSeconds: Float = 0f,
    val durationSeconds: Float = 0f,
    val playbackSpeed: Float = 1f,
    val errorMessage: String? = null,
)

internal fun formatDesktopVideoDuration(seconds: Float): String {
    val totalSeconds = seconds.takeIf(Float::isFinite)?.coerceAtLeast(0f)?.toInt() ?: 0
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds / 60 % 60
    val remainder = totalSeconds % 60
    val minuteText = minutes.toString().padStart(2, '0')
    val secondText = remainder.toString().padStart(2, '0')
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:$minuteText:$secondText"
    } else {
        "$minuteText:$secondText"
    }
}

internal class MpvVideoPanel : JPanel(BorderLayout()) {
    private val videoCanvas = VideoCanvas()
    private val status = JLabel("", SwingConstants.CENTER)
    private val state = MutableStateFlow(DesktopVideoPlaybackState())
    val playbackState: StateFlow<DesktopVideoPlaybackState> = state.asStateFlow()
    private var player: Process? = null
    private var activePath: String? = null
    private var ipcWriter: BufferedWriter? = null
    private var ipcChannel: SocketChannel? = null
    private var ipcSocketDirectory: Path? = null
    private var lastUnmutedVolume = 100f
    private val ipcLock = Any()

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
            setPlaybackError("Video location is unavailable.")
            return
        }
        if (!MpvVideoCommand.supportsEmbeddedPlayback(System.getProperty("os.name"))) {
            setPlaybackError("Embedded video playback is unavailable on this platform.")
            return
        }
        val path = try {
            Path.of(mediaPath)
        } catch (_: RuntimeException) {
            setPlaybackError("The selected video is unavailable.")
            return
        }
        val readableVideo = try {
            Files.isRegularFile(path) && Files.isReadable(path)
        } catch (_: SecurityException) {
            false
        }
        if (!readableVideo) {
            setPlaybackError("The selected video is unavailable.")
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
            setPlaybackError("Embedded video playback is unavailable in this window system.")
            return
        }
        val isLinux = System.getProperty("os.name").lowercase().contains("linux")
        val ipcSocketPath = if (isLinux) {
            try {
                createPrivateIpcSocketPath().also { ipcSocketDirectory = it.parent }
            } catch (_: IOException) {
                setPlaybackError("In-app video controls could not be initialized.")
                return
            } catch (_: UnsupportedOperationException) {
                setPlaybackError("In-app video controls are unavailable on this filesystem.")
                return
            } catch (_: SecurityException) {
                setPlaybackError("In-app video controls could not be initialized.")
                return
            }
        } else {
            null
        }
        var process: Process? = null
        try {
            val startedProcess = ProcessBuilder(
                MpvVideoCommand.arguments(windowId, path.toString(), ipcSocketPath = ipcSocketPath?.toString()),
            )
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            process = startedProcess
            startedProcess.outputStream.close()
            player = startedProcess
            status.text = ""
            state.value = DesktopVideoPlaybackState()
            if (ipcSocketPath != null) startIpcReader(startedProcess, ipcSocketPath)
            Thread({
                val exitCode = startedProcess.waitFor()
                SwingUtilities.invokeLater {
                    if (player === startedProcess) {
                        player = null
                        state.value = state.value.copy(
                            connected = false,
                            errorMessage = if (exitCode == 0) null else "Video playback failed.",
                        )
                        status.text = if (exitCode == 0) "" else "Video playback failed."
                        cleanupIpc()
                        if (exitCode != 0) {
                            repaint()
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
            cleanupIpc()
            setPlaybackError("The in-app video player could not be started.")
        } catch (_: SecurityException) {
            process?.destroyForcibly()
            if (player === process) player = null
            cleanupIpc()
            setPlaybackError("Video playback could not be started.")
        }
    }

    fun togglePlayback() {
        sendCommand("cycle", "pause")
    }

    fun setLooping(enabled: Boolean) {
        sendCommand("set_property", "loop-file", if (enabled) "inf" else "no")
    }

    fun setMuted(enabled: Boolean) {
        if (enabled) {
            if (!state.value.muted) lastUnmutedVolume = state.value.volume
            sendCommand("set_property", "volume", 0)
        } else {
            sendCommand("set_property", "volume", lastUnmutedVolume)
        }
    }

    fun seekTo(seconds: Float) {
        val target = seconds.takeIf(Float::isFinite)?.coerceIn(0f, state.value.durationSeconds)
            ?: return
        sendCommand("set_property", "time-pos", target)
    }

    fun setPlaybackSpeed(speed: Float) {
        if (speed !in SUPPORTED_PLAYBACK_SPEEDS) return
        sendCommand("set_property", "speed", speed)
    }

    fun close() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { close() }
            return
        }
        activePath = null
        stopPlayer()
        state.value = DesktopVideoPlaybackState()
    }

    private fun stopPlayer() {
        cleanupIpc()
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

    private fun createPrivateIpcSocketPath(): Path {
        val permissions = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
        )
        return Files.createTempDirectory(
            "kg-mpv-",
            PosixFilePermissions.asFileAttribute(permissions),
        ).resolve("control.sock")
    }

    private fun startIpcReader(process: Process, socketPath: Path) {
        Thread({
            val channel = connectToIpc(process, socketPath)
            if (channel == null) {
                SwingUtilities.invokeLater {
                    if (player === process) {
                        state.value = state.value.copy(
                            errorMessage = "In-app video controls could not connect to the player.",
                        )
                    }
                }
                process.destroy()
                return@Thread
            }
            val writer = BufferedWriter(
                OutputStreamWriter(Channels.newOutputStream(channel), StandardCharsets.UTF_8),
            )
            val reader = BufferedReader(
                InputStreamReader(Channels.newInputStream(channel), StandardCharsets.UTF_8),
            )
            synchronized(ipcLock) {
                if (player !== process) {
                    channel.close()
                    return@synchronized
                }
                ipcChannel = channel
                ipcWriter = writer
            }
            if (!channel.isOpen) return@Thread
            state.value = state.value.copy(connected = true, errorMessage = null)
            listOf(
                "pause",
                "time-pos",
                "duration",
                "volume",
                "speed",
                "loop-file",
            ).forEachIndexed { index, property ->
                sendCommand("observe_property", index + 1, property)
            }
            try {
                while (process.isAlive) {
                    val line = reader.readLine() ?: break
                    readPropertyChange(line)
                }
            } catch (_: IOException) {
                if (process.isAlive) {
                    state.value = state.value.copy(
                        connected = false,
                        errorMessage = "In-app video controls lost their player connection.",
                    )
                }
            } finally {
                synchronized(ipcLock) {
                    if (ipcChannel === channel) {
                        ipcWriter = null
                        ipcChannel = null
                    }
                }
                try {
                    channel.close()
                } catch (_: IOException) {
                    Unit
                }
                state.value = state.value.copy(connected = false)
            }
        }, "kaede-mpv-ipc").apply {
            isDaemon = true
            start()
        }
    }

    private fun connectToIpc(process: Process, socketPath: Path): SocketChannel? {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (process.isAlive && System.nanoTime() < deadline) {
            val channel = try {
                SocketChannel.open(StandardProtocolFamily.UNIX).also {
                    it.connect(UnixDomainSocketAddress.of(socketPath))
                }
            } catch (_: IOException) {
                null
            }
            if (channel != null) return channel
            try {
                Thread.sleep(50)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return null
            }
        }
        return null
    }

    private fun readPropertyChange(line: String) {
        val event = try {
            Json.parseToJsonElement(line).jsonObject
        } catch (_: SerializationException) {
            return
        } catch (_: IllegalArgumentException) {
            return
        }
        if (event["event"]?.jsonPrimitive?.contentOrNull != "property-change") return
        val property = event["name"]?.jsonPrimitive?.contentOrNull ?: return
        val value = event["data"] as? JsonPrimitive ?: return
        when (property) {
            "pause" -> value.jsonPrimitive.booleanOrNull?.let { paused ->
                state.value = state.value.copy(playing = !paused)
            }
            "time-pos" -> value.jsonPrimitive.floatOrNull?.let { seconds ->
                state.value = state.value.copy(positionSeconds = seconds.coerceAtLeast(0f))
            }
            "duration" -> value.jsonPrimitive.floatOrNull?.let { seconds ->
                state.value = state.value.copy(durationSeconds = seconds.coerceAtLeast(0f))
            }
            "volume" -> value.jsonPrimitive.floatOrNull?.let { volume ->
                state.value = state.value.copy(muted = volume <= 0f, volume = volume)
                if (volume > 0f) lastUnmutedVolume = volume
            }
            "speed" -> value.jsonPrimitive.floatOrNull?.let { speed ->
                state.value = state.value.copy(playbackSpeed = speed)
            }
            "loop-file" -> state.value = state.value.copy(
                looping = value.jsonPrimitive.contentOrNull?.let { it != "no" } ?: false,
            )
        }
    }

    private fun sendCommand(vararg values: Any) {
        val command = buildJsonArray {
            values.forEach { value ->
                when (value) {
                    is String -> add(JsonPrimitive(value))
                    is Int -> add(JsonPrimitive(value))
                    is Float -> add(JsonPrimitive(value))
                    else -> error("Unsupported mpv command value.")
                }
            }
        }
        synchronized(ipcLock) {
            val writer = ipcWriter ?: return
            try {
                writer.write(buildJsonObject { put("command", command) }.toString())
                writer.newLine()
                writer.flush()
            } catch (_: IOException) {
                state.value = state.value.copy(
                    connected = false,
                    errorMessage = "In-app video controls lost their player connection.",
                )
            }
        }
    }

    private fun cleanupIpc() {
        synchronized(ipcLock) {
            ipcWriter = null
            try {
                ipcChannel?.close()
            } catch (_: IOException) {
                Unit
            }
            ipcChannel = null
        }
        ipcSocketDirectory?.let { directory ->
            try {
                Files.deleteIfExists(directory.resolve("control.sock"))
                Files.deleteIfExists(directory)
            } catch (_: IOException) {
                Unit
            } catch (_: SecurityException) {
                Unit
            }
        }
        ipcSocketDirectory = null
    }

    private fun setPlaybackError(message: String) {
        status.text = message
        state.value = DesktopVideoPlaybackState(errorMessage = message)
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

    private companion object {
        val SUPPORTED_PLAYBACK_SPEEDS = setOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    }
}
