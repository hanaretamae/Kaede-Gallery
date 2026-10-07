package com.hanaretamae.kaede.desktop

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.LinkedHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal object DesktopVideoThumbnail {
    const val MAX_IMAGE_BYTES = 4 * 1024 * 1024
    private const val CACHE_ENTRIES = 8
    private const val TIMEOUT_SECONDS = 10L
    private val processSlots = Semaphore(2)
    private val cache = LinkedHashMap<String, ByteArray>(CACHE_ENTRIES, 0.75f, true)

    fun command(mediaPath: String): List<String> = listOf(
        "ffmpeg",
        "-nostdin",
        "-hide_banner",
        "-loglevel",
        "error",
        "-protocol_whitelist",
        "file",
        "-ss",
        "0",
        "-i",
        mediaPath,
        "-map",
        "0:v:0",
        "-an",
        "-sn",
        "-dn",
        "-frames:v",
        "1",
        "-vf",
        "scale=w='min(512,iw)':h='min(512,ih)':force_original_aspect_ratio=decrease",
        "-threads",
        "1",
        "-f",
        "image2pipe",
        "-vcodec",
        "png",
        "pipe:1",
    )

    fun extract(mediaLocation: String): ByteArray? {
        val operatingSystem = System.getProperty("os.name").lowercase()
        if (!operatingSystem.contains("linux") && !operatingSystem.contains("windows")) {
            return null
        }
        val path = try {
            Path.of(mediaLocation).toRealPath()
        } catch (_: IOException) {
            return null
        } catch (_: RuntimeException) {
            return null
        }
        val attributes = try {
            Files.readAttributes(path, BasicFileAttributes::class.java)
        } catch (_: IOException) {
            return null
        } catch (_: SecurityException) {
            return null
        }
        val isReadable = try {
            Files.isReadable(path)
        } catch (_: SecurityException) {
            false
        }
        if (!attributes.isRegularFile || !isReadable) return null

        val canonicalPath =
            "${path}\u0000${attributes.size()}\u0000${attributes.lastModifiedTime().toMillis()}"
        synchronized(cache) {
            cache.remove(canonicalPath)?.let { cached ->
                cache[canonicalPath] = cached
                return cached
            }
        }
        try {
            processSlots.acquire()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return null
        }
        try {
            val bytes = extractFromFile(path) ?: return null
            synchronized(cache) {
                cache[canonicalPath] = bytes
                if (cache.size > CACHE_ENTRIES) {
                    cache.entries.iterator().run {
                        next()
                        remove()
                    }
                }
            }
            return bytes
        } finally {
            processSlots.release()
        }
    }

    private fun extractFromFile(path: Path): ByteArray? {
        val process = try {
            ProcessBuilder(command(path.toString()))
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (_: IOException) {
            return null
        } catch (_: SecurityException) {
            return null
        }

        val output = AtomicReference<ByteArray?>()
        val captureError = AtomicReference<IOException?>()
        val capture = Thread({
            try {
                output.set(process.inputStream.use { it.readNBytes(MAX_IMAGE_BYTES + 1) })
            } catch (error: IOException) {
                captureError.set(error)
            }
        }, "kaede-video-thumbnail-output").apply {
            isDaemon = true
            start()
        }

        try {
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                capture.join(1_000)
                return null
            }
            capture.join(1_000)
            if (capture.isAlive || captureError.get() != null || process.exitValue() != 0) {
                return null
            }
            return output.get()?.takeIf { it.isNotEmpty() && it.size <= MAX_IMAGE_BYTES }
        } catch (_: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            return null
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }
}
