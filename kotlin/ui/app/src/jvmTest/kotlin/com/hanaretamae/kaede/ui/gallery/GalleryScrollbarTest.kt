package com.hanaretamae.kaede.ui.gallery

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.MouseButton
import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class GalleryScrollbarTest {
    @get:Rule
    val compose = createComposeRule()

    private fun showGallery() {
        val entries = (1L..60L).map { id ->
            NoteSummary(
                id = NoteId(id),
                path = "fictional-$id.md",
                title = "Fictional note $id",
                representativeMediaId = MediaId(id),
                mediaCount = 1,
                videoCount = 0,
                memoCount = 0,
                relatedCount = 0,
            )
        }
        val repository = object : GalleryRepository {
            override suspend fun queryPage(query: GalleryQuery) =
                RepositoryResult.Success(
                    GalleryPage(
                        entries = entries.drop(query.offset.toInt()).take(query.pageSize),
                        totalCount = entries.size.toLong(),
                        offset = query.offset,
                    ),
                )

            override suspend fun categories(query: GalleryQuery) =
                RepositoryResult.Success(emptyList<com.hanaretamae.kaede.core.model.GalleryCategory>())

            override suspend fun noteDetail(
                noteId: NoteId,
                safContent: ByteArray?,
            ) = RepositoryResult.Success<com.hanaretamae.kaede.core.model.GalleryNoteDetail?>(null)

            override suspend fun notePath(noteId: NoteId) =
                RepositoryResult.Success<String?>(null)

            override suspend fun mediaLocation(mediaId: MediaId) =
                RepositoryResult.Success<String?>(null)

            override suspend fun thumbnail(mediaId: MediaId, size: Int) =
                RepositoryResult.Success<ByteArray?>(null)
        }

        compose.setContent {
            val scope = rememberCoroutineScope()
            val holder = remember { GalleryStateHolder(repository, scope) }
            KaedeGalleryTheme(AppearanceSettings()) {
                GalleryScreen(holder, onEntrySelected = {}, onSettings = {})
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("gallery-scrollbar-track")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun hoveringScrollbarTrackShowsPositionPopup() {
        showGallery()
        compose.onNodeWithTag("gallery-scrollbar-track").performMouseInput {
            enter(center)
            moveTo(center)
        }
        compose.onNodeWithTag("gallery-scrollbar-position").assertIsDisplayed()
    }

    @Test
    fun draggingScrollbarTrackWithMouseShowsPositionPopup() {
        showGallery()
        compose.onNodeWithTag("gallery-scrollbar-track").performMouseInput {
            enter(center)
            press(MouseButton.Primary)
        }
        val popup = compose.onNodeWithTag("gallery-scrollbar-position")
        popup.assertIsDisplayed()
        val popupBounds = popup.fetchSemanticsNode().boundsInRoot
        val trackBounds = compose.onNodeWithTag("gallery-scrollbar-track")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            popupBounds.right < trackBounds.left,
            "Scrollbar position popup should appear to the left of its track: " +
                "popup=$popupBounds track=$trackBounds.",
        )
        compose.onNodeWithTag("gallery-scrollbar-track").performMouseInput {
            moveTo(center + Offset(0f, 80f), delayMillis = 100)
        }
        compose.onNodeWithTag("gallery-scrollbar-position").assertIsDisplayed()
        compose.onNodeWithTag("gallery-scrollbar-track").performMouseInput {
            release(MouseButton.Primary)
        }
    }
}
