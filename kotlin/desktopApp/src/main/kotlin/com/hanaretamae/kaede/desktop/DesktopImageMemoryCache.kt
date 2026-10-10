package com.hanaretamae.kaede.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.rust.SingleFlight
import com.hanaretamae.kaede.core.rust.ThumbnailLoadPriority
import com.hanaretamae.kaede.core.rust.ThumbnailLoadScheduler
import org.jetbrains.skia.Image

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
