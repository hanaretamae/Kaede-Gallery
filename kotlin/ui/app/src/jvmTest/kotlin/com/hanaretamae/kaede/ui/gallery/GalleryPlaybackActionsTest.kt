package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GalleryPlaybackActionsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun trailingControlsAreRightAlignedAtMinimumAndWideWindowWidths() {
        compose.setContent {
            Column {
                listOf(400, 800).forEach { width ->
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
        listOf(400, 800).forEach { width ->
            val row = compose.onNodeWithTag("row-$width").fetchSemanticsNode().boundsInRoot
            val leading = compose.onNodeWithTag("leading-$width").fetchSemanticsNode().boundsInRoot
            val trailing = compose.onNodeWithTag("action-$width-3").fetchSemanticsNode().boundsInRoot
            assertEquals(row.left, leading.left, 1f)
            assertEquals(row.right, trailing.right, 1f)
            if (width == 400) assertTrue(trailing.top >= leading.bottom)
            else assertEquals(leading.center.y, trailing.center.y, 1f)
        }
    }
}
