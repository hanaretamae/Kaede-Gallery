package com.hanaretamae.kaede.desktop

import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.rust.ThumbnailLoadPriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesktopImageMemoryCacheTest {
    @Test
    fun viewerDecodesTheSourceImageAtAUsefulSize() {
        val path = Files.createTempFile("kaede-fictional-viewer-", ".png")
        try {
            val source = BufferedImage(640, 320, BufferedImage.TYPE_INT_ARGB)
            assertEquals(true, ImageIO.write(source, "png", path.toFile()))

            val decoded = decodeDesktopViewerImage(path.toString(), 320)

            assertEquals(320, decoded?.width)
            assertEquals(160, decoded?.height)
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun viewerDecodesWebpImagesAtABoundedSize() {
        val path = Files.createTempFile("kaede-fictional-viewer-", ".webp")
        try {
            val encoded = Bitmap().use { source ->
                source.allocN32Pixels(640, 320)
                Image.makeFromBitmap(source).use {
                    it.encodeToData(EncodedImageFormat.WEBP, 100)
                }
            }
            assertNotNull(encoded)
            encoded.use { Files.write(path, it.bytes) }

            val decoded = decodeDesktopViewerImage(path.toString(), 320)

            assertEquals(320, decoded?.width)
            assertEquals(160, decoded?.height)
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun previewReusesGridImageAndUpgradesToLargestCachedSize() {
        DesktopImageMemoryCache.clear()
        try {
            val grid = Bitmap().use {
                it.allocN32Pixels(24, 16)
                Image.makeFromBitmap(it).toComposeImageBitmap()
            }
            val viewer = Bitmap().use {
                it.allocN32Pixels(48, 32)
                Image.makeFromBitmap(it).toComposeImageBitmap()
            }
            DesktopImageMemoryCache.put(MediaId(1), 320, grid)
            assertEquals(grid, DesktopImageMemoryCache.preview(MediaId(1)))
            assertNull(DesktopImageMemoryCache.preview(MediaId(2)))
            DesktopImageMemoryCache.put(MediaId(1), 1600, viewer)
            assertEquals(viewer, DesktopImageMemoryCache.preview(MediaId(1)))
        } finally {
            DesktopImageMemoryCache.clear()
        }
    }

    @Test
    fun concurrentLoadsShareTheSameDecodedBitmap() = runBlocking {
        DesktopImageMemoryCache.clear()
        val gate = CompletableDeferred<Unit>()
        val bitmap = Bitmap().use {
            it.allocN32Pixels(24, 16)
            Image.makeFromBitmap(it).toComposeImageBitmap()
        }
        var loadCount = 0

        try {
            val first = async {
                DesktopImageMemoryCache.load(
                    MediaId(3),
                    512,
                    ThumbnailLoadPriority.VISIBLE,
                ) {
                    loadCount += 1
                    gate.await()
                    DesktopImageState(bitmap = bitmap)
                }
            }
            yield()
            val second = async {
                DesktopImageMemoryCache.load(
                    MediaId(3),
                    512,
                    ThumbnailLoadPriority.PREFETCH,
                ) {
                    loadCount += 1
                    DesktopImageState()
                }
            }
            yield()
            gate.complete(Unit)

            assertEquals(bitmap, first.await().bitmap)
            assertEquals(bitmap, second.await().bitmap)
            assertEquals(1, loadCount)
        } finally {
            DesktopImageMemoryCache.clear()
        }
    }
}
