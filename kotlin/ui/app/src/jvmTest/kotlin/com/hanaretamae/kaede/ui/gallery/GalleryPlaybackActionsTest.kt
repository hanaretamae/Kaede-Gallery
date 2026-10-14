package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GalleryPlaybackActionsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun playbackControlsProvideJapaneseLabels() {
        assertEquals("動画を再生", JapaneseGalleryPlaybackStrings.playVideo)
        assertEquals("動画を一時停止", JapaneseGalleryPlaybackStrings.pauseVideo)
        assertEquals("再生速度", JapaneseGalleryPlaybackStrings.playbackSpeed)
        assertEquals("動画をループ再生", JapaneseGalleryPlaybackStrings.loopVideo)
        assertEquals("動画をミュート", JapaneseGalleryPlaybackStrings.muteVideo)
        assertEquals("全画面表示", JapaneseGalleryPlaybackStrings.fullScreen)
    }

    @Test
    fun controlsStayOnOneLineAndTrailingActionsRemainReachable() {
        compose.setContent {
            Column {
                listOf(320, 360, 400, 800).forEach { width ->
                    Box(Modifier.width(width.dp).testTag("row-$width")) {
                        GalleryPlaybackActions(
                            leading = { Box(Modifier.size(48.dp).testTag("leading-$width")) },
                            trailing = {
                                repeat(4) { index ->
                                    Box(Modifier.size(48.dp).testTag("action-$width-$index"))
                                }
                            },
                        )
                    }
                }
            }
        }
        listOf(320, 360, 400, 800).forEach { width ->
            val row = compose.onNodeWithTag("row-$width").fetchSemanticsNode().boundsInRoot
            val leading = compose.onNodeWithTag("leading-$width").fetchSemanticsNode().boundsInRoot
            val trailing = compose.onNodeWithTag("action-$width-3").fetchSemanticsNode().boundsInRoot
            assertEquals(row.left, leading.left, 1f)
            assertTrue(trailing.right <= row.right)
            assertEquals(leading.center.y, trailing.center.y, 1f)
            if (width >= 400) assertEquals(row.right, trailing.right, 1f)
        }
    }

    @Test
    fun hiddenControlsOfferTheDesktopRevealAndFullscreenActions() {
        var revealCount = 0
        var fullscreenCount = 0
        compose.setContent {
            GalleryPlaybackHiddenActions(
                fullscreen = true,
                onShowControls = { revealCount++ },
                onToggleFullscreen = { fullscreenCount++ },
                strings = EnglishGalleryPlaybackStrings,
            )
        }

        compose.onNodeWithContentDescription("Show video controls")
            .assertIsDisplayed()
            .performClick()
        compose.onNodeWithContentDescription("Exit full screen")
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, revealCount)
        assertEquals(1, fullscreenCount)
    }
}
