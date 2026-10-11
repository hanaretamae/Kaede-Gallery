package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
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
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.math.abs

class GalleryViewerGestureTest {
    @get:Rule
    val compose = createComposeRule()

    private fun showViewer(
        withLongBody: Boolean = false,
        onHolderCreated: (GalleryViewerStateHolder) -> Unit = {},
        onPlaybackOverlayVisibilityChanged: (((Boolean) -> Unit) -> Unit) = {},
    ) {
        val media = (1L..2L).map {
            MediaSummary(MediaId(it), NoteId(1), "fictional.md", false, true, 2, 0, 0)
        }
        val note = GalleryNoteDetail(
            NoteId(1), "fictional.md", "Fictional note", null, null, null,
            null, null, null, emptyList(),
            if (withLongBody) "Fictional paragraph. ".repeat(300) else "",
            emptyList(), emptyList(), media,
        )
        val repository = object : GalleryRepository {
            override suspend fun queryPage(query: GalleryQuery) =
                RepositoryResult.Success(GalleryPage(emptyList(), 0, 0))
            override suspend fun categories(query: GalleryQuery) =
                RepositoryResult.Success(emptyList<GalleryCategory>())
            override suspend fun noteDetail(noteId: NoteId, safContent: ByteArray?) =
                RepositoryResult.Success(note)
            override suspend fun notePath(noteId: NoteId) = RepositoryResult.Success("fictional.md")
            override suspend fun mediaLocation(mediaId: MediaId) = RepositoryResult.Success("fictional.png")
            override suspend fun thumbnail(mediaId: MediaId, size: Int) = RepositoryResult.Success(null)
        }
        compose.setContent {
            val scope = rememberCoroutineScope()
            val holder = remember {
                GalleryViewerStateHolder(repository, scope, media.first()).also(onHolderCreated)
            }
            KaedeGalleryTheme(AppearanceSettings()) {
                GalleryViewerScreen(
                    stateHolder = holder, onClose = {}, onExternalLink = {},
                    mediaContent = { mediaState ->
                        mediaState.onPlaybackOverlayVisibilityChanged
                            ?.let(onPlaybackOverlayVisibilityChanged)
                        Box(
                            Modifier.fillMaxSize().testTag("media-area"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.size(120.dp).background(Color.Red).testTag("image"),
                            )
                        }
                    },
                )
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("media-area", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun horizontalTrackpadScrollNavigatesWithoutRequiringDozensOfWheelTicks() {
        showViewer()
        compose.onNodeWithTag("media-area", useUnmergedTree = true).performMouseInput {
            enter(center)
            scroll(-3f, ScrollWheel.Horizontal)
        }
        compose.onNodeWithText("2 / 2").assertExists()
    }

    @Test
    fun horizontalTrackpadGestureKeepsAccumulatingAcrossBriefPauses() {
        showViewer()
        compose.onNodeWithTag("media-area", useUnmergedTree = true).performMouseInput {
            enter(center)
            scroll(-1f, ScrollWheel.Horizontal)
            advanceEventTime(350)
            scroll(-1f, ScrollWheel.Horizontal)
        }
        compose.onNodeWithText("2 / 2").assertExists()
    }

    @Test
    fun playbackSpeedChoicesDoNotResizeTheMediaArea() {
        lateinit var setPlaybackOverlayVisible: (Boolean) -> Unit
        showViewer(
            onPlaybackOverlayVisibilityChanged = { callback ->
                setPlaybackOverlayVisible = callback
            },
        )
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("viewer-note-details")
                .fetchSemanticsNodes().isNotEmpty()
        }
        val panel = compose.onNodeWithTag("viewer-note-details")
        val mediaArea = compose.onNodeWithTag("media-area", useUnmergedTree = true)
        val initialMediaHeight = mediaArea
            .fetchSemanticsNode().boundsInRoot.height
        val initialPanelTop = panel.fetchSemanticsNode().boundsInRoot.top

        compose.runOnIdle { setPlaybackOverlayVisible(true) }
        compose.waitUntil(5_000) {
            mediaArea.fetchSemanticsNode().boundsInRoot.height > initialMediaHeight + 1f
        }
        val openMediaHeight = mediaArea.fetchSemanticsNode().boundsInRoot.height
        val openPanelTop = panel.fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            openMediaHeight >= initialMediaHeight,
            "Playback choices must not shrink the media viewport.",
        )
        assertTrue(openPanelTop > initialPanelTop)

        compose.runOnIdle { setPlaybackOverlayVisible(false) }
        compose.waitUntil(5_000) {
            mediaArea.fetchSemanticsNode().boundsInRoot.height <= initialMediaHeight + 1f
        }
        val restoredMediaHeight = mediaArea.fetchSemanticsNode().boundsInRoot.height
        assertTrue(abs(restoredMediaHeight - initialMediaHeight) < 1f)
    }

    @Test
    fun scrollingNoteDetailsAlsoResizesTheMediaArea() {
        showViewer(withLongBody = true)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("viewer-note-details")
                .fetchSemanticsNodes().isNotEmpty()
        }
        val details = compose.onNodeWithTag("viewer-note-details")
        val initialTop = details.fetchSemanticsNode().boundsInRoot.top
        details.performMouseInput {
            enter(center)
            scroll(3f, ScrollWheel.Vertical)
        }
        compose.waitUntil(5_000) {
            details.fetchSemanticsNode().boundsInRoot.top < initialTop
        }
    }

    @Test
    fun scrollingDetailsWithoutOverflowDoesNotShrinkTheImageArea() {
        showViewer()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("viewer-note-details")
                .fetchSemanticsNodes().isNotEmpty()
        }
        val details = compose.onNodeWithTag("viewer-note-details")
        val mediaArea = compose.onNodeWithTag("media-area", useUnmergedTree = true)
        val initialDetailsTop = details.fetchSemanticsNode().boundsInRoot.top
        val initialMediaHeight = mediaArea.fetchSemanticsNode().boundsInRoot.height

        details.performMouseInput {
            enter(center)
            scroll(8f, ScrollWheel.Vertical)
        }
        compose.waitForIdle()

        assertTrue(
            abs(details.fetchSemanticsNode().boundsInRoot.top - initialDetailsTop) < 1f,
            "Details without overflowing content must not change panel size.",
        )
        assertTrue(
            abs(mediaArea.fetchSemanticsNode().boundsInRoot.height - initialMediaHeight) < 1f,
            "Scrolling an empty details panel must not shrink the image area.",
        )
    }

    @Test
    fun viewerToolbarAndMediaViewportEnterWithoutLayoutJump() {
        showViewer()
        val toolbar = compose.onNodeWithTag("viewer-details-toolbar")
        val mediaArea = compose.onNodeWithTag("media-area", useUnmergedTree = true)
        mediaArea.performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("viewer-details-toolbar")
                .fetchSemanticsNodes().isEmpty()
        }
        compose.mainClock.autoAdvance = false
        mediaArea.performClick()
        compose.mainClock.advanceTimeByFrame()
        val initialToolbarHeight = toolbar.fetchSemanticsNode().boundsInRoot.height
        val initialMediaTop = mediaArea.fetchSemanticsNode().boundsInRoot.top

        compose.mainClock.advanceTimeBy(80)
        val intermediateToolbarHeight = toolbar.fetchSemanticsNode().boundsInRoot.height
        val intermediateMediaTop = mediaArea.fetchSemanticsNode().boundsInRoot.top

        compose.mainClock.advanceTimeBy(1_000)
        val finalToolbarHeight = toolbar.fetchSemanticsNode().boundsInRoot.height
        val finalMediaTop = mediaArea.fetchSemanticsNode().boundsInRoot.top

        assertTrue(initialToolbarHeight < intermediateToolbarHeight)
        assertTrue(intermediateToolbarHeight < finalToolbarHeight)
        assertTrue(initialMediaTop < intermediateMediaTop)
        assertTrue(intermediateMediaTop < finalMediaTop)
    }

    @Test
    fun switchingImagesKeepsBothFramesComposedDuringTheTransition() {
        var holder: GalleryViewerStateHolder? = null
        showViewer(onHolderCreated = { holder = it })
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { requireNotNull(holder).showNextMedia() }

        compose.mainClock.advanceTimeBy(50)
        compose.waitForIdle()
        val transitioningImages = compose.onAllNodesWithTag("image", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue(
            transitioningImages.size >= 2,
            "Both media frames should be composed during the animated switch.",
        )

        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        assertTrue(
            compose.onAllNodesWithTag("image", useUnmergedTree = true)
                .fetchSemanticsNodes().size == 1,
            "The outgoing media frame should be removed when the transition ends.",
        )
    }

    @Test
    fun twoFingerPinchIsNotConsumedByDetailPanelDragging() {
        showViewer()
        val initial = compose.onNodeWithTag("image", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.width
        compose.onNodeWithTag("media-area", useUnmergedTree = true).performTouchInput {
            val middle = center
            down(0, middle - Offset(40f, 0f))
            down(1, middle + Offset(40f, 0f))
            repeat(8) {
                moveTo(0, middle - Offset(50f + it * 8f, 0f))
                moveTo(1, middle + Offset(50f + it * 8f, 0f))
            }
            up(0)
            up(1)
        }
        val zoomed = compose.onNodeWithTag("image", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.width
        assertTrue(zoomed > initial * 1.5f, "Pinch should enlarge the image: $initial -> $zoomed")
    }

    @Test
    fun controlScrollZoomKeepsTheImagePointUnderTheCursor() {
        showViewer()
        val image = compose.onNodeWithTag("image", useUnmergedTree = true)
        val before = image.fetchSemanticsNode().boundsInRoot
        val area = compose.onNodeWithTag("media-area", useUnmergedTree = true)
        val areaBounds = area.fetchSemanticsNode().boundsInRoot
        val focus = Offset(before.right - 20f, before.center.y)
        compose.onRoot().performKeyInput { keyDown(Key.CtrlLeft) }
        area.performMouseInput {
            enter(focus - areaBounds.topLeft)
            scroll(-3f)
        }
        compose.onRoot().performKeyInput { keyUp(Key.CtrlLeft) }
        val after = image.fetchSemanticsNode().boundsInRoot
        assertTrue(after.width > before.width * 1.3f)
        val originalPoint = (focus.x - before.left) / before.width
        val zoomedPoint = (focus.x - after.left) / after.width
        assertTrue(abs(originalPoint - zoomedPoint) < 0.03f, "$before -> $after at $focus")
    }
}
