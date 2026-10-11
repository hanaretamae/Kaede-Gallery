package com.hanaretamae.kaede.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.rust.SingleFlight
import com.hanaretamae.kaede.core.rust.ThumbnailLoadPriority
import com.hanaretamae.kaede.core.rust.ThumbnailLoadScheduler
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.math.roundToInt

/** Bounded in-memory cache of decoded media bitmaps; cleared whenever the index can change. */
internal object DesktopImageMemoryCache {
    private const val MAX_BYTES = 128L * 1024 * 1024
    private const val MAX_CONCURRENT_LOADS = 4

    private data class LoadKey(val mediaId: Long, val size: Int, val generation: Long)

    private val entries = LinkedHashMap<Pair<Long, Int>, ImageBitmap>(64, 0.75f, true)
    private val requests = SingleFlight<LoadKey, DesktopImageState>()
    private val scheduler = ThumbnailLoadScheduler(MAX_CONCURRENT_LOADS)
    private var totalBytes = 0L
    private var generation = 0L

    @Synchronized
    fun get(mediaId: MediaId, size: Int): ImageBitmap? = entries[mediaId.value to size]

    @Synchronized
    fun preview(mediaId: MediaId): ImageBitmap? {
        val key = entries.keys.filter { it.first == mediaId.value }.maxByOrNull { it.second }
            ?: return null
        return entries[key]
    }

    @Synchronized
    fun put(mediaId: MediaId, size: Int, bitmap: ImageBitmap) {
        val bytes = bitmap.byteCount()
        if (bytes > MAX_BYTES) return
        entries.put(mediaId.value to size, bitmap)?.let { totalBytes -= it.byteCount() }
        totalBytes += bytes
        val iterator = entries.entries.iterator()
        while (totalBytes > MAX_BYTES && iterator.hasNext()) {
            totalBytes -= iterator.next().value.byteCount()
            iterator.remove()
        }
    }

    suspend fun load(
        mediaId: MediaId,
        size: Int,
        priority: ThumbnailLoadPriority,
        loader: suspend () -> DesktopImageState,
    ): DesktopImageState {
        get(mediaId, size)?.let { return DesktopImageState(bitmap = it) }
        val key = synchronized(this) { LoadKey(mediaId.value, size, generation) }
        return requests.run(key) {
            get(mediaId, size)?.let { return@run DesktopImageState(bitmap = it) }
            scheduler.withPermit(priority) {
                get(mediaId, size)?.let { return@withPermit DesktopImageState(bitmap = it) }
                loader().also { loaded ->
                    val bitmap = loaded.bitmap
                    if (bitmap != null) {
                        synchronized(this) {
                            if (generation == key.generation) put(mediaId, size, bitmap)
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    fun clear() {
        entries.clear()
        totalBytes = 0
        generation += 1
    }

    private fun ImageBitmap.byteCount(): Long = width.toLong() * height * 4
}

internal data class DesktopImageState(
    val bitmap: ImageBitmap? = null,
    val error: String? = null,
)

/** Decodes with Skia, which is considerably faster than ImageIO; returns null for unsupported data. */
internal fun decodeDesktopImage(bytes: ByteArray): ImageBitmap? = try {
    Image.makeFromEncoded(bytes).toComposeImageBitmap()
} catch (_: IllegalArgumentException) {
    null
}

internal fun decodeDesktopViewerImage(path: String?, maxDimension: Int): ImageBitmap? {
    if (path.isNullOrBlank() || maxDimension !in 1..MAX_VIEWER_IMAGE_DIMENSION) return null
    val file = try {
        Path.of(path).toRealPath()
    } catch (_: IOException) {
        return null
    } catch (_: RuntimeException) {
        return null
    }
    val attributes = try {
        Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes::class.java)
    } catch (_: IOException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    val readable = try {
        Files.isReadable(file)
    } catch (_: SecurityException) {
        false
    }
    if (!attributes.isRegularFile || !readable || attributes.size() !in 1..MAX_VIEWER_IMAGE_BYTES) {
        return null
    }
    val bytes = try {
        Files.newInputStream(file).use { it.readNBytes(MAX_VIEWER_IMAGE_BYTES.toInt() + 1) }
    } catch (_: IOException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    if (bytes.size > MAX_VIEWER_IMAGE_BYTES) return null
    val image = try {
        Image.makeFromEncoded(bytes)
    } catch (_: IllegalArgumentException) {
        return null
    } catch (_: IllegalStateException) {
        return null
    }
    image.use {
        val width = it.width
        val height = it.height
        if (
            width !in 1..MAX_VIEWER_IMAGE_DIMENSION ||
            height !in 1..MAX_VIEWER_IMAGE_DIMENSION ||
            width.toLong() * height > MAX_VIEWER_IMAGE_PIXELS
        ) {
            return null
        }
        val scale = minOf(1f, maxDimension.toFloat() / maxOf(width, height))
        val outputWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val outputHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap().use { bitmap ->
            if (!bitmap.allocN32Pixels(outputWidth, outputHeight)) return null
            val pixels = bitmap.peekPixels() ?: return null
            if (!it.scalePixels(pixels, SamplingMode.LINEAR, false)) return null
            Image.makeFromBitmap(bitmap).toComposeImageBitmap()
        }
    }
}

private const val MAX_VIEWER_IMAGE_BYTES = 64L * 1024 * 1024
private const val MAX_VIEWER_IMAGE_DIMENSION = 16_384
private const val MAX_VIEWER_IMAGE_PIXELS = 32L * 1024 * 1024
