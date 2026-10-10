package com.hanaretamae.kaede.desktop

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.hanaretamae.kaede.core.model.GalleryCategory
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.MediaSummary
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.ui.gallery.GalleryViewerMediaState
import com.hanaretamae.kaede.ui.gallery.KaedeGalleryTheme
import java.awt.Component
import java.awt.Container
import java.awt.Frame
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

@OptIn(androidx.compose.runtime.tooling.ComposeToolingApi::class)
class DesktopVideoWindowIntegrationTest {
    @Test(timeout = 30_000)
    fun maximizedNativePlayerAcceptsSpeedSelectionAndFurtherInput() {
        val fixture = System.getenv("KAEDE_TEST_UI_VIDEO")
        assumeTrue("Requires a fictional video and X11 display", fixture != null)
        val maximized = mutableStateOf(true)
        val repository = object : GalleryRepository {
            override suspend fun queryPage(query: GalleryQuery) =
                RepositoryResult.Success(GalleryPage(emptyList(), 0, 0))
            override suspend fun categories(query: GalleryQuery) =
                RepositoryResult.Success(emptyList<GalleryCategory>())
            override suspend fun noteDetail(noteId: NoteId, safContent: ByteArray?) =
                RepositoryResult.Success<GalleryNoteDetail?>(null)
            override suspend fun notePath(noteId: NoteId) = RepositoryResult.Success<String?>(null)
            override suspend fun mediaLocation(mediaId: MediaId) = RepositoryResult.Success(fixture)
            override suspend fun thumbnail(mediaId: MediaId, size: Int) =
                RepositoryResult.Success<ByteArray?>(null)
        }
        lateinit var window: ComposeWindow
        SwingUtilities.invokeAndWait {
            window = ComposeWindow().apply {
                title = "Kaede fictional native speed regression"
                setSize(800, 600)
                setContent {
                    KaedeGalleryTheme(AppearanceSettings()) {
                        DesktopMediaContent(
                            repository,
                            GalleryViewerMediaState(
                                media = MediaSummary(MediaId(1), NoteId(1), "fictional.md", true, true, 1, 0, 0),
                                location = fixture,
                                locationLoading = false,
                                fullscreen = maximized.value,
                                onToggleFullscreen = {
                                    maximized.value = !maximized.value
                                    window.placement = if (maximized.value) {
                                        androidx.compose.ui.window.WindowPlacement.Maximized
                                    } else {
                                        androidx.compose.ui.window.WindowPlacement.Floating
                                    }
                                },
                            ),
                        )
                    }
                }
                isVisible = true
                extendedState = Frame.MAXIMIZED_BOTH
            }
        }
        fun await(message: String, predicate: () -> Boolean) {
            val deadline = System.nanoTime() + 8_000_000_000L
            while (!predicate() && System.nanoTime() < deadline) Thread.sleep(20)
            assertTrue(predicate(), message)
        }
        fun findPlayer(component: Component): MpvVideoPanel? =
            (component as? MpvVideoPanel) ?: (component as? Container)?.components
                ?.firstNotNullOfOrNull(::findPlayer)
        fun click(x: Int, y: Int) {
            SwingUtilities.invokeAndWait {
                val target = requireNotNull(SwingUtilities.getDeepestComponentAt(window.contentPane, x, y))
                val point = SwingUtilities.convertPoint(window.contentPane, x, y, target)
                val time = System.currentTimeMillis()
                target.dispatchEvent(MouseEvent(target, MouseEvent.MOUSE_PRESSED, time,
                    MouseEvent.BUTTON1_DOWN_MASK, point.x, point.y, 1, false, MouseEvent.BUTTON1))
                target.dispatchEvent(MouseEvent(target, MouseEvent.MOUSE_RELEASED, time + 10,
                    0, point.x, point.y, 1, false, MouseEvent.BUTTON1))
            }
        }
        fun findNode(label: String): SemanticsNode? {
            fun match(node: SemanticsNode): SemanticsNode? {
                val text = node.config.getOrNull(SemanticsProperties.Text)
                val description = node.config.getOrNull(SemanticsProperties.ContentDescription)
                return if (text?.any { it.text == label } == true || description?.contains(label) == true) {
                    node
                } else node.children.firstNotNullOfOrNull(::match)
            }
            return window.semanticsOwners.firstNotNullOfOrNull { match(it.rootSemanticsNode) }
        }
        fun clickLabel(label: String) {
            var node: SemanticsNode? = null
            await("Control is present: $label") {
                SwingUtilities.invokeAndWait { node = findNode(label) }
                node != null
            }
            var center = androidx.compose.ui.geometry.Offset.Zero
            SwingUtilities.invokeAndWait { center = requireNotNull(node).boundsInRoot.center }
            click(center.x.toInt(), center.y.toInt())
        }
        try {
            var player: MpvVideoPanel? = null
            await("Native playback connects") {
                SwingUtilities.invokeAndWait { player = findPlayer(window) }
                player?.playbackState?.value?.connected == true
            }
            val active = requireNotNull(player)
            SwingUtilities.invokeAndWait {
                assertEquals(androidx.compose.ui.window.WindowPlacement.Maximized, window.placement)
            }
            await("Native video is playing") { active.playbackState.value.playing }
            clickLabel("1.0x")
            Thread.sleep(3_500)
            clickLabel("0.5x")
            await("Menu stays interactive past the controls auto-hide delay") {
                active.playbackState.value.playbackSpeed == 0.5f && active.playbackState.value.playing
            }
            clickLabel("Pause video")
            await("Playback pauses") { !active.playbackState.value.playing }
            clickLabel("0.5x")
            clickLabel("2.0x")
            await("Speed selection reaches native playback") { active.playbackState.value.playbackSpeed == 2f }
            clickLabel("Mute video")
            await("Mute remains operable after speed selection") { active.playbackState.value.muted }
            clickLabel("Restore application window")
            await("Restore remains operable after speed selection") { !maximized.value }
            SwingUtilities.invokeAndWait {
                assertEquals(androidx.compose.ui.window.WindowPlacement.Floating, window.placement)
            }
        } finally {
            SwingUtilities.invokeAndWait { window.dispose() }
        }
    }
}
