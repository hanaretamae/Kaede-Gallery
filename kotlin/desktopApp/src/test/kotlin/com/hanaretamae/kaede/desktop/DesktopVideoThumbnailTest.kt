package com.hanaretamae.kaede.desktop

import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopVideoThumbnailTest {
    @Test
    fun extractsOneBoundedFrameFromFictionalVideoWhenFfmpegIsAvailable() {
        if (!System.getProperty("os.name").lowercase().contains("linux")) return
        val directory = Files.createTempDirectory("kaede-video-thumbnail-test")
        val video = directory.resolve("fictional-color.mp4")
        try {
            if (!createFictionalVideo(video.toString())) return
            val thumbnail = DesktopVideoThumbnail.extract(video.toString())
            assertNotNull(thumbnail)
            assertTrue(thumbnail.size <= DesktopVideoThumbnail.MAX_IMAGE_BYTES)
            val image = ImageIO.read(ByteArrayInputStream(thumbnail))
            assertNotNull(image)
            assertEquals(512, image.width)
            assertEquals(384, image.height)
        } finally {
            Files.deleteIfExists(video)
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun ffmpegCommandDisablesNetworkInputsAndBoundsOutput() {
        val command = DesktopVideoThumbnail.command("/tmp/fictional-video.mp4")
        assertEquals("ffmpeg", command.first())
        assertTrue(command.containsAll(listOf("-nostdin", "-protocol_whitelist", "file")))
        assertTrue(command.containsAll(listOf("-frames:v", "1", "-threads", "1")))
        assertEquals("pipe:1", command.last())
        assertEquals(
            "C:\\fictional\\color.mp4",
            DesktopVideoThumbnail.command("C:\\fictional\\color.mp4")[10],
        )
    }

    private fun createFictionalVideo(path: String): Boolean {
        val process = try {
            ProcessBuilder(
                "ffmpeg",
                "-nostdin",
                "-hide_banner",
                "-loglevel",
                "error",
                "-f",
                "lavfi",
                "-i",
                "color=c=magenta:s=1024x768:r=1:d=2",
                "-frames:v",
                "2",
                "-c:v",
                "libx264",
                "-pix_fmt",
                "yuv420p",
                "-y",
                path,
            )
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (_: IOException) {
            return false
        } catch (_: SecurityException) {
            return false
        }
        return try {
            process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0
        } catch (_: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            false
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }
}
