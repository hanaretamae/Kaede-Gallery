package com.hanaretamae.kaede.desktop

import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal data class PortalFileChooserResponse(
    val responseCode: Int,
    val selectedUri: URI?,
)

internal fun linuxPortalOpenDirectoryCommand(title: String): List<String> = listOf(
    "gdbus",
    "call",
    "--session",
    "--dest=org.freedesktop.portal.Desktop",
    "--object-path=/org/freedesktop/portal/desktop",
    "--method=org.freedesktop.portal.FileChooser.OpenFile",
    "",
    title,
    "{'directory': <true>, 'multiple': <false>, 'modal': <true>}",
)

internal fun parsePortalRequestHandle(output: String): String? =
    Regex("""objectpath '([^']+)'""")
        .find(output)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.startsWith("/org/freedesktop/portal/desktop/request/") }

internal fun parsePortalResponseLine(
    requestHandle: String,
    line: String,
): PortalFileChooserResponse? {
    val prefix = "$requestHandle: org.freedesktop.portal.Request.Response "
    if (!line.startsWith(prefix)) return null

    val responseCode = Regex("""\(uint32 (\d+),""")
        .find(line)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?: return null
    val selectedUri = if (responseCode == PORTAL_RESPONSE_SUCCESS) {
        Regex("""'uris':\s*<\['([^']+)'\]>""")
            .find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { value -> runCatching { URI(value) }.getOrNull() }
    } else {
        null
    }
    return PortalFileChooserResponse(responseCode, selectedUri)
}

internal fun resolvePortalDirectory(uri: URI): Path? {
    if (!uri.scheme.equals("file", ignoreCase = true)) return null
    return try {
        Path.of(uri).toRealPath().takeIf {
            Files.isDirectory(it, LinkOption.NOFOLLOW_LINKS)
        }
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

internal object LinuxPortalDirectoryPicker {
    fun chooseDirectory(title: String): Path? {
        val monitor = PortalSignalMonitor()
        monitor.use {
            if (!monitor.awaitReady()) {
                throw IOException("The system directory picker is unavailable.")
            }
            val handle = parsePortalRequestHandle(
                runGdbus(linuxPortalOpenDirectoryCommand(title)),
            ) ?: throw IOException("The system directory picker is unavailable.")
            val response = monitor.awaitResponse(handle)
            if (response == null) {
                closePortalRequest(handle)
                throw IOException("The system directory picker did not respond.")
            }
            return when (response.responseCode) {
                PORTAL_RESPONSE_SUCCESS -> response.selectedUri
                    ?.let(::resolvePortalDirectory)
                    ?: throw IOException("The selected folder is unavailable.")
                PORTAL_RESPONSE_CANCELLED -> null
                else -> throw IOException("The system directory picker failed.")
            }
        }
    }

    private fun runGdbus(command: List<String>): String {
        val process = try {
            ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (_: IOException) {
            throw IOException("The system directory picker is unavailable.")
        } catch (_: SecurityException) {
            throw IOException("The system directory picker is unavailable.")
        }
        try {
            if (!process.waitFor(GDBUS_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                throw IOException("The system directory picker is unavailable.")
            }
            val output = process.inputStream.readNBytes(MAX_GDBUS_OUTPUT_BYTES + 1)
            if (output.size > MAX_GDBUS_OUTPUT_BYTES || process.exitValue() != 0) {
                throw IOException("The system directory picker is unavailable.")
            }
            return output.toString(Charsets.UTF_8)
        } catch (interrupted: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            throw IOException("The system directory picker was interrupted.")
        }
    }

    private fun closePortalRequest(handle: String) {
        val process = try {
            ProcessBuilder(
                "gdbus",
                "call",
                "--session",
                "--dest=org.freedesktop.portal.Desktop",
                "--object-path=$handle",
                "--method=org.freedesktop.portal.Request.Close",
            )
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (_: IOException) {
            return
        } catch (_: SecurityException) {
            return
        }
        try {
            if (!process.waitFor(GDBUS_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
        } catch (_: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
        }
    }

    private class PortalSignalMonitor : AutoCloseable {
        private val process = try {
            ProcessBuilder(
                "gdbus",
                "monitor",
                "--session",
                "--dest=org.freedesktop.portal.Desktop",
            )
                .redirectErrorStream(true)
                .start()
        } catch (_: IOException) {
            throw IOException("The system directory picker is unavailable.")
        } catch (_: SecurityException) {
            throw IOException("The system directory picker is unavailable.")
        }
        private val lines = LinkedBlockingQueue<String>(MAX_MONITOR_LINES)
        private val reader = thread(
            name = "kaede-linux-portal-monitor",
            isDaemon = true,
        ) {
            process.inputStream.bufferedReader().useLines { output ->
                output.forEach { line ->
                    if (line.length <= MAX_MONITOR_LINE_LENGTH) lines.offer(line)
                }
            }
        }

        fun awaitReady(): Boolean {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(MONITOR_START_TIMEOUT_SECONDS)
            while (System.nanoTime() < deadline) {
                val line = lines.poll(MONITOR_POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                if (line?.startsWith(MONITOR_READY_PREFIX) == true) return true
                if (!process.isAlive) return false
            }
            return false
        }

        fun awaitResponse(requestHandle: String): PortalFileChooserResponse? {
            val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(PORTAL_RESPONSE_TIMEOUT_MINUTES)
            while (System.nanoTime() < deadline) {
                val line = lines.poll(MONITOR_POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                if (line != null) {
                    parsePortalResponseLine(requestHandle, line)?.let { return it }
                }
                if (!process.isAlive) return null
            }
            return null
        }

        override fun close() {
            process.destroy()
            try {
                if (!process.waitFor(MONITOR_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    process.waitFor(MONITOR_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                }
                reader.join(TimeUnit.SECONDS.toMillis(MONITOR_STOP_TIMEOUT_SECONDS))
            } catch (_: InterruptedException) {
                process.destroyForcibly()
                Thread.currentThread().interrupt()
            }
        }
    }
}

private const val PORTAL_RESPONSE_SUCCESS = 0
private const val PORTAL_RESPONSE_CANCELLED = 1
private const val GDBUS_CALL_TIMEOUT_SECONDS = 15L
private const val MONITOR_START_TIMEOUT_SECONDS = 5L
private const val PORTAL_RESPONSE_TIMEOUT_MINUTES = 10L
private const val MONITOR_STOP_TIMEOUT_SECONDS = 1L
private const val MONITOR_POLL_INTERVAL_MILLIS = 200L
private const val MAX_GDBUS_OUTPUT_BYTES = 4_096
private const val MAX_MONITOR_LINES = 32
private const val MAX_MONITOR_LINE_LENGTH = 16_384
private const val MONITOR_READY_PREFIX = "Monitoring signals from all objects owned by org.freedesktop.portal.Desktop"
