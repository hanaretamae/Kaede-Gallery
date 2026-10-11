package com.hanaretamae.kaede.desktop

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.input.key.Key
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
import com.hanaretamae.kaede.ui.gallery.GalleryViewerScreen
import com.hanaretamae.kaede.ui.gallery.GalleryViewerStateHolder
import com.hanaretamae.kaede.ui.gallery.KaedeGalleryTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class DesktopVideoSeekingTest {
    @get:Rule
    val compose = createComposeRule()

    private fun showViewer(
        seeks: MutableList<Float> = mutableListOf(),
        onFullscreenChanged: (Boolean) -> Unit = {},
        openedMedia: MutableList<MediaId> = mutableListOf(),
    ) {
        val media = MediaSummary(MediaId(1), NoteId(1), "fictional.md", true, true, 1, 0, 0)
        val detail = GalleryNoteDetail(
            NoteId(1), "fictional.md", "Fictional video", null, null, null,
            null, null, null, emptyList(), "", emptyList(), emptyList(), listOf(media),
        )
        val repository = object : GalleryRepository {
            override suspend fun queryPage(query: GalleryQuery) =
                RepositoryResult.Success(GalleryPage(emptyList(), 0, 0))
            override suspend fun categories(query: GalleryQuery) =
                RepositoryResult.Success(emptyList<GalleryCategory>())
            override suspend fun noteDetail(noteId: NoteId, safContent: ByteArray?) =
                RepositoryResult.Success(detail)
            override suspend fun notePath(noteId: NoteId) = RepositoryResult.Success("fictional.md")
            override suspend fun mediaLocation(mediaId: MediaId) =
                RepositoryResult.Success("fictional.mp4")
            override suspend fun thumbnail(mediaId: MediaId, size: Int) = RepositoryResult.Success(null)
        }
        compose.setContent {
            val scope = rememberCoroutineScope()
            val holder = remember { GalleryViewerStateHolder(repository, scope, media) }
            val speed = remember { androidx.compose.runtime.mutableStateOf(1f) }
            KaedeGalleryTheme(AppearanceSettings()) {
                GalleryViewerScreen(
                    holder, onClose = {}, onExternalLink = {},
                    onFullscreenChanged = onFullscreenChanged,
                    onOpenMedia = { openedMedia.add(it.media.id) },
                    mediaContent = { mediaState ->
                        DesktopVideoControls(
                            state = DesktopVideoPlaybackState(
                                connected = true, durationSeconds = 100f, playing = true,
                                playbackSpeed = speed.value,
                            ),
                            onPlayPause = {}, onSeek = { seeks.add(it) },
                            onSpeed = { speed.value = it }, onLoop = {}, onMute = {},
                            fullscreen = mediaState.fullscreen,
                            onToggleFullscreen = mediaState.onToggleFullscreen,
                        )
                    },
                )
            }
        }
        val sliderMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
        compose.waitUntil(5_000) {
            compose.onAllNodes(sliderMatcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun mediaActionsExpandInLayoutAboveTheVideoAndRemainSelectable() {
        val openedMedia = mutableListOf<MediaId>()
        showViewer(openedMedia = openedMedia)
        compose.onNodeWithContentDescription("Open media").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Open media").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("Open media") and hasClickAction())
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle { assertEquals(listOf(MediaId(1)), openedMedia) }
    }

    @Test
    fun viewerAllowsDraggingVideoSliderContinuouslyBeforeReleasingIt() {
        val seeks = mutableListOf<Float>()
        showViewer(seeks)
        val sliderMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
        val slider = compose.onNode(sliderMatcher)
        slider.performTouchInput {
            down(Offset(width * 0.25f, center.y))
            moveTo(Offset(width * 0.4f, center.y))
        }
        compose.runOnIdle { assertTrue(seeks.isNotEmpty()) }
        val first = seeks.last()
        slider.performTouchInput { moveTo(Offset(width * 0.8f, center.y)) }
        compose.runOnIdle { assertTrue(seeks.last() > first + 20f, "$seeks") }
        slider.performTouchInput { moveTo(Offset(width * 0.3f, center.y)) }
        compose.runOnIdle { assertTrue(seeks.last() < first, "$seeks") }
        slider.performTouchInput { up() }
    }

    @Test
    fun maximizedVideoControlsRemainAccessibleAndEscapeRestoresViewer() {
        var maximized = false
        showViewer(onFullscreenChanged = { maximized = it })
        compose.onNodeWithContentDescription("Maximize application window").performClick()
        compose.runOnIdle { assertEquals(true, maximized) }
        compose.onNodeWithContentDescription("Pause video").assertIsDisplayed()
        compose.onNodeWithContentDescription("Restore application window").assertIsDisplayed()
        compose.onNodeWithText("1.0x").performClick()
        compose.onNodeWithText("0.25x").assertIsDisplayed()
        compose.onNodeWithText("2.0x").assertIsDisplayed().performClick()
        compose.onNodeWithText("2.0x").assertIsDisplayed()
        compose.onNodeWithText("0.25x").assertDoesNotExist()
        compose.onNodeWithContentDescription("Pause video").assertIsDisplayed()
        compose.onNodeWithContentDescription("Playback speed").performClick()
        compose.onNodeWithText("0.75x").assertIsDisplayed()
        compose.onNodeWithText("1.75x").assertIsDisplayed().performClick()
        compose.onNodeWithText("1.75x").assertIsDisplayed()
        compose.onNodeWithText("0.75x").assertDoesNotExist()
        compose.onNodeWithContentDescription("Playback speed").performClick()
        compose.onNodeWithText("0.75x").assertIsDisplayed()
        compose.onNodeWithContentDescription("Playback speed").performClick()
        compose.onNodeWithText("0.75x").assertDoesNotExist()
        compose.onRoot().performKeyInput {
            keyDown(Key.Escape)
            keyUp(Key.Escape)
        }
        compose.runOnIdle { assertEquals(false, maximized) }
        compose.onNodeWithContentDescription("Restore application window").assertDoesNotExist()
        compose.onNodeWithContentDescription("Maximize application window").assertIsDisplayed()
    }

}
