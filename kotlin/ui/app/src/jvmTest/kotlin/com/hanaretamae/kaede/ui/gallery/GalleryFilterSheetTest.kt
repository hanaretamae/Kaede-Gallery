package com.hanaretamae.kaede.ui.gallery

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.isRoot
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.hanaretamae.kaede.core.model.GalleryCategory
import com.hanaretamae.kaede.core.model.GalleryCategoryOption
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.model.VirtualFilter
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import org.junit.Rule
import org.junit.Test

class GalleryFilterSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun galleryThumbnailPriorityIsLimitedToTilesCurrentlyOnScreen() {
        val entries = (1L..24L).map { id ->
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
                RepositoryResult.Success(GalleryPage(entries, entries.size.toLong(), query.offset))
            override suspend fun categories(query: GalleryQuery) =
                RepositoryResult.Success(emptyList<GalleryCategory>())
            override suspend fun noteDetail(noteId: NoteId, safContent: ByteArray?) =
                RepositoryResult.Success<GalleryNoteDetail?>(null)
            override suspend fun notePath(noteId: NoteId) = RepositoryResult.Success<String?>(null)
            override suspend fun mediaLocation(mediaId: MediaId) = RepositoryResult.Success<String?>(null)
            override suspend fun thumbnail(mediaId: MediaId, size: Int) =
                RepositoryResult.Success<ByteArray?>(null)
        }
        val visibility = mutableStateMapOf<MediaId, Boolean>()
        compose.setContent {
            val scope = rememberCoroutineScope()
            val holder = remember { GalleryStateHolder(repository, scope) }
            KaedeGalleryTheme(AppearanceSettings()) {
                GalleryScreen(
                    holder,
                    onEntrySelected = {},
                    onSettings = {},
                    galleryThumbnail = { mediaId, _, isVisible ->
                        SideEffect { visibility[mediaId] = isVisible }
                    },
                )
            }
        }
        compose.waitUntil(5_000) {
            visibility.values.any { it }
        }
        compose.runOnIdle {
            assertTrue(visibility.values.any { it }, "On-screen tiles should be prioritized.")
        }
    }

    @Test
    fun headingDragsSheetAndExpandedCloseDismissesIt() {
        val repository = object : GalleryRepository {
            override suspend fun queryPage(query: GalleryQuery) =
                RepositoryResult.Success(GalleryPage(emptyList(), 0, 0))
            override suspend fun categories(query: GalleryQuery) =
                RepositoryResult.Success(
                    listOf(
                        GalleryCategory(
                            path = "source",
                            displayName = "Source",
                            options = listOf(
                                GalleryCategoryOption(
                                    name = "art",
                                    fullTag = "source/art",
                                    count = 1,
                                    disabled = false,
                                    virtualFilter = VirtualFilter.MULTIPLE_MEDIA,
                                ),
                            ),
                            count = 1,
                        ),
                    ),
                )
            override suspend fun noteDetail(noteId: NoteId, safContent: ByteArray?) =
                RepositoryResult.Success<GalleryNoteDetail?>(null)
            override suspend fun notePath(noteId: NoteId) = RepositoryResult.Success<String?>(null)
            override suspend fun mediaLocation(mediaId: MediaId) = RepositoryResult.Success<String?>(null)
            override suspend fun thumbnail(mediaId: MediaId, size: Int) = RepositoryResult.Success<ByteArray?>(null)
        }
        compose.setContent {
            val scope = rememberCoroutineScope()
            val holder = remember { GalleryStateHolder(repository, scope) }
            KaedeGalleryTheme(AppearanceSettings()) {
                GalleryScreen(holder, onEntrySelected = {}, onSettings = {})
            }
        }
        val strings = EnglishGalleryStrings
        compose.onNodeWithContentDescription(strings.filters).performClick()
        compose.onNodeWithContentDescription(strings.close).assertDoesNotExist()
        val windowWidth = compose.onAllNodes(isRoot()).fetchSemanticsNodes().maxOf {
            it.boundsInRoot.width
        }
        val collapsedBounds = compose.onNodeWithTag("GalleryFilterSheet")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(windowWidth, collapsedBounds.width, 1f)
        assertEquals(0f, collapsedBounds.left, 1f)
        compose.onNodeWithText(strings.filters).performTouchInput {
            swipe(center, center - Offset(0f, 450f), durationMillis = 500)
        }
        compose.onNodeWithContentDescription(strings.close).assertIsDisplayed()
        val expandedBounds = compose.onNodeWithTag("GalleryFilterSheet").fetchSemanticsNode().boundsInRoot
        assertEquals(windowWidth, expandedBounds.width, 1f)
        assertEquals(0f, expandedBounds.left, 1f)
        val sortSurface = compose.onNodeWithTag("GalleryFilterSortSurface").fetchSemanticsNode().boundsInRoot
        assertTrue(sortSurface.left > 0f)
        assertTrue(sortSurface.right < windowWidth)
        val searchField = compose.onNodeWithText(strings.searchNotes).fetchSemanticsNode().boundsInRoot
        assertTrue(searchField.left > 0f)
        val tagSearchField = compose.onNodeWithText(strings.filterSearch).fetchSemanticsNode().boundsInRoot
        assertTrue(tagSearchField.left > 0f)
        assertTrue(tagSearchField.right < windowWidth)
        val categorySurface = compose.onNodeWithTag("GalleryFilterCategorySurface")
            .fetchSemanticsNode().boundsInRoot
        val categoryLabel = compose.onNodeWithText("Source", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(categoryLabel.left - categorySurface.left >= 8f)
        val searchAction = compose.onNodeWithContentDescription(strings.applySearch).fetchSemanticsNode().boundsInRoot
        val clearAction = compose.onNodeWithContentDescription(strings.clearAllSearchAndFilters).fetchSemanticsNode().boundsInRoot
        assertTrue(clearAction.right < windowWidth)
        assertTrue(searchAction.top < searchField.bottom)
        assertTrue(clearAction.left > searchAction.left)
        compose.onNodeWithText(strings.filters).performTouchInput {
            swipe(center, center + Offset(0f, 180f), durationMillis = 1_000)
        }
        compose.onNodeWithContentDescription(strings.close).assertDoesNotExist()
        compose.onNodeWithText(strings.filters).performTouchInput {
            swipe(center, center - Offset(0f, 450f), durationMillis = 500)
        }
        compose.onNodeWithContentDescription(strings.close).performClick()
        compose.onNodeWithText(strings.filters).assertDoesNotExist()
        compose.onNodeWithContentDescription(strings.filters).performClick()
        compose.onNodeWithText(strings.searchNotes).performTextInput("fictional")
        compose.onNodeWithContentDescription(strings.applySearch).performClick()
        compose.onNodeWithTag("GalleryFilterSheet").assertDoesNotExist()
        compose.onNodeWithContentDescription(strings.filters).assertIsDisplayed()
    }
}
