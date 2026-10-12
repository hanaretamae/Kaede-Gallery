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
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import java.net.StandardProtocolFamily
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

internal interface MpvIpcConnection : Closeable {
    val input: InputStream
    val output: OutputStream
}

internal object MpvVideoCommand {
    fun supportsEmbeddedPlayback(osName: String): Boolean {
        val normalizedName = osName.lowercase()
        return normalizedName.contains("linux") || normalizedName.contains("windows")
    }

    fun supportsInAppControls(osName: String = System.getProperty("os.name")): Boolean =
        supportsEmbeddedPlayback(osName)

    fun arguments(
        windowId: Long,
        mediaPath: String,
        osName: String = System.getProperty("os.name"),
        ipcSocketPath: String? = null,
    ): List<String> = buildList {
        add(desktopToolCommand("mpv", osName))
        add("--no-config")
        add("--no-load-scripts")
        add("--osc=no")
        add("--osd-level=1")
        add("--input-default-bindings=no")
        add("--no-ytdl")
        add("--autoload-files=no")
        add("--sub-auto=no")
        add("--audio-file-auto=no")
        add("--loop-file=no")
        add("--keep-open=yes")
        add("--force-window=yes")
        if (osName.lowercase().contains("linux")) add("--vo=x11")
        if (ipcSocketPath != null) {
            add("--input-ipc-server=$ipcSocketPath")
        }
        add("--wid=$windowId")
        add("--")
        add(mediaPath)
    }
}

internal fun encodeMpvCommand(values: Iterable<Any>): String =
    buildJsonObject {
        put(
            "command",
            buildJsonArray {
                values.forEach { value ->
                    when (value) {
                        is String -> add(JsonPrimitive(value))
                        is Int -> add(JsonPrimitive(value))
                        is Float -> add(JsonPrimitive(value))
                        is Boolean -> add(JsonPrimitive(value))
                        else -> error("Unsupported mpv command value.")
                    }
                }
            },
        )
    }.toString()

// Channels.newInputStream holds the channel's blocking lock while waiting for
// data, which prevents the IPC writer from sending commands on the same socket.
internal fun mpvSocketInputStream(channel: SocketChannel): InputStream = object : InputStream() {
    override fun read(): Int {
        val byte = ByteArray(1)
        return if (read(byte, 0, 1) == -1) -1 else byte[0].toInt() and 0xff
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int =
        if (length == 0) 0 else channel.read(ByteBuffer.wrap(bytes, offset, length))
}

internal data class DesktopVideoPlaybackState(
    val connected: Boolean = false,
    val playing: Boolean = false,
    val ended: Boolean = false,
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
    @Volatile
    private var player: Process? = null
    private var activePath: String? = null
    private var ipcWriter: BufferedWriter? = null
    private var ipcConnection: MpvIpcConnection? = null
    private var ipcSocketDirectory: Path? = null
    private var lastUnmutedVolume = 100f
    private val ipcLock = Any()
    private val commandExecutor = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(32),
        { task ->
            Thread(task, "kaede-mpv-command").apply { isDaemon = true }
        },
    )

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
            desktopPathOf(mediaPath)
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
        val ipcEndpoint = if (isLinux) {
            try {
                createPrivateIpcSocketPath().also { ipcSocketDirectory = it.parent }.toString()
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
        } else windowsMpvPipePath()
        var process: Process? = null
        try {
            val startedProcess = ProcessBuilder(
                MpvVideoCommand.arguments(windowId, path.toString(), ipcSocketPath = ipcEndpoint),
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
            startIpcReader(startedProcess, ipcEndpoint, !isLinux)
            Thread({
                val exitCode = startedProcess.waitFor()
                SwingUtilities.invokeLater {
                    if (player === startedProcess) {
                        player = null
                        val playbackError = state.value.errorMessage
                            ?: if (exitCode == 0) null else "Video playback failed."
                        state.value = state.value.copy(
                            connected = false,
                            errorMessage = playbackError,
                        )
                        status.text = playbackError ?: ""
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
        if (state.value.ended) {
            state.value = state.value.copy(playing = true, ended = false, positionSeconds = 0f)
            sendCommand("seek", 0f, "absolute+exact")
            sendCommand("set_property", "pause", false)
        } else {
            sendCommand("cycle", "pause")
        }
    }

    fun setLooping(enabled: Boolean) {
        state.value = state.value.copy(looping = enabled)
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
        state.value = state.value.copy(positionSeconds = target, ended = false)
        sendCommand("seek", target, "absolute+exact")
    }

    fun setPlaybackSpeed(speed: Float) {
        if (speed !in SUPPORTED_PLAYBACK_SPEEDS) return
        sendCommand("set_property", "speed", speed)
    }

    fun close() {
        activePath = null
        stopPlayer()
        state.value = DesktopVideoPlaybackState()
        commandExecutor.shutdownNow()
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

    private fun startIpcReader(process: Process, endpoint: String, windows: Boolean) {
        Thread({
            val connection = try {
                connectToIpc(process, endpoint, windows)
            } catch (_: LinkageError) {
                null
            }
            if (connection == null) {
                SwingUtilities.invokeLater {
                    if (player === process) {
                        state.value = state.value.copy(
                            errorMessage = "In-app video controls could not connect to the player.",
                        )
                        status.text = "In-app video controls could not connect to the player."
                    }
                }
                process.destroy()
                return@Thread
            }
            val writer = BufferedWriter(
                OutputStreamWriter(connection.output, StandardCharsets.UTF_8),
            )
            val reader = BufferedReader(
                InputStreamReader(connection.input, StandardCharsets.UTF_8),
            )
            val active = synchronized(ipcLock) {
                if (player !== process) false else {
                    ipcConnection = connection
                    ipcWriter = writer
                    true
                }
            }
            if (!active) {
                connection.close()
                return@Thread
            }
            state.value = state.value.copy(connected = true, errorMessage = null)
            listOf(
                "pause",
                "time-pos",
                "duration",
                "volume",
                "speed",
                "loop-file",
                "eof-reached",
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
                    if (ipcConnection === connection) {
                        ipcWriter = null
                        ipcConnection = null
                    }
                }
                try {
                    connection.close()
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

    private fun connectToIpc(process: Process, endpoint: String, windows: Boolean): MpvIpcConnection? {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (process.isAlive && System.nanoTime() < deadline) {
            val connection = try {
                if (windows) WindowsMpvPipe.open(endpoint) else {
                    val channel = SocketChannel.open(StandardProtocolFamily.UNIX)
                    try {
                        channel.connect(UnixDomainSocketAddress.of(Path.of(endpoint)))
                    } catch (failure: IOException) {
                        channel.close()
                        throw failure
                    }
                    object : MpvIpcConnection {
                        override val input = mpvSocketInputStream(channel)
                        override val output = Channels.newOutputStream(channel)
                        override fun close() = channel.close()
                    }
                }
            } catch (_: IOException) {
                null
            }
            if (connection != null) return connection
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
            "eof-reached" -> value.jsonPrimitive.booleanOrNull?.let { ended ->
                val current = state.value
                state.value = current.copy(
                    ended = ended,
                    playing = if (ended) false else state.value.playing,
                    positionSeconds = if (ended && current.durationSeconds > 0f) {
                        current.durationSeconds
                    } else {
                        current.positionSeconds
                    },
                )
            }
            "volume" -> value.jsonPrimitive.floatOrNull?.let { volume ->
                state.value = state.value.copy(muted = volume <= 0f, volume = volume)
                if (volume > 0f) lastUnmutedVolume = volume
            }
            "speed" -> value.jsonPrimitive.floatOrNull?.let { speed ->
                state.value = state.value.copy(playbackSpeed = speed)
            }
            "loop-file" -> state.value = state.value.copy(
                looping = value.contentOrNull == "inf" ||
                    (value.floatOrNull ?: 0f) > 0f,
            )
        }
    }

    private fun sendCommand(vararg values: Any) {
        val expectedPlayer = player ?: return
        val command = encodeMpvCommand(values.asIterable())
        try {
            commandExecutor.execute {
                val writer = synchronized(ipcLock) {
                    if (player === expectedPlayer) ipcWriter else null
                } ?: return@execute
                try {
                    writer.write(command)
                    writer.newLine()
                    writer.flush()
                } catch (_: IOException) {
                    if (player === expectedPlayer) {
                        state.value = state.value.copy(
                            connected = false,
                            errorMessage = "In-app video controls lost their player connection.",
                        )
                    }
                }
            }
        } catch (_: RejectedExecutionException) {
            if (player === expectedPlayer) {
                state.value = state.value.copy(
                    errorMessage = "In-app video controls are busy.",
                )
            }
        }
    }

    private fun cleanupIpc() {
        val connection = synchronized(ipcLock) {
            ipcWriter = null
            ipcConnection.also { ipcConnection = null }
        }
        try {
            connection?.close()
        } catch (_: IOException) {
            Unit
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
        val SUPPORTED_PLAYBACK_SPEEDS = setOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    }
}
