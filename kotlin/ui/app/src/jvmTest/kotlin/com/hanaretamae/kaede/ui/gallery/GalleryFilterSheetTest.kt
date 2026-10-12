package com.hanaretamae.kaede.ui.gallery

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.isRoot
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.hanaretamae.kaede.core.model.GalleryCategory
import com.hanaretamae.kaede.core.model.GalleryCategoryOption
import com.hanaretamae.kaede.core.model.GalleryContent
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
import com.hanaretamae.kaede.core.settings.SavedGallerySearch
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
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
    fun headingDragsSheetAndMaterialDragHandleDismissesIt() {
        val repository = object : GalleryRepository {
            override suspend fun queryPage(query: GalleryQuery) =
                RepositoryResult.Success(GalleryPage(emptyList(), 0, 0))
            override suspend fun categories(query: GalleryQuery) =
                RepositoryResult.Success(
                    listOf(
                        GalleryCategory(
                            path = "@content",
                            displayName = "コンテンツ",
                            options = listOf(
                                GalleryCategoryOption(
                                    name = "multiple media",
                                    fullTag = "",
                                    count = 1,
                                    disabled = false,
                                    virtualFilter = VirtualFilter.MULTIPLE_MEDIA,
                                ),
                            ),
                            count = 4,
                        ),
                        GalleryCategory(
                            path = "source",
                            displayName = "Source",
                            options = listOf(
                                GalleryCategoryOption(
                                    name = "art",
                                    fullTag = "source/art",
                                    count = 1,
                                    disabled = false,
                                    virtualFilter = null,
                                ),
                                GalleryCategoryOption(
                                    name = "blue",
                                    fullTag = "source/blue",
                                    count = 1,
                                    disabled = false,
                                    virtualFilter = null,
                                ),
                                GalleryCategoryOption(
                                    name = "draft",
                                    fullTag = "source/draft",
                                    count = 1,
                                    disabled = false,
                                    virtualFilter = null,
                                ),
                            ),
                            count = 4,
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
            val holder = remember {
                GalleryStateHolder(
                    repository,
                    scope,
                    GalleryQuery(
                        includeTags = setOf("source/art"),
                        andTags = setOf("source/blue"),
                        excludedTags = setOf("source/draft"),
                        virtualFilters = setOf(VirtualFilter.MULTIPLE_MEDIA),
                    ),
                )
            }
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
        val maximumSheetWidth = with(compose.density) {
            BottomSheetDefaults.SheetMaxWidth.toPx()
        }
        assertTrue(windowWidth > maximumSheetWidth)
        val expectedSheetWidth = minOf(windowWidth, maximumSheetWidth)
        val collapsedBounds = compose.onNodeWithTag("GalleryFilterSheet")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(expectedSheetWidth, collapsedBounds.width, 1f)
        assertEquals((windowWidth - expectedSheetWidth) / 2f, collapsedBounds.left, 1f)
        compose.onNodeWithText(strings.filters).performTouchInput {
            swipe(center, center - Offset(0f, 450f), durationMillis = 500)
        }
        compose.onNodeWithContentDescription(strings.close).assertDoesNotExist()
        val expandedBounds = compose.onNodeWithTag("GalleryFilterSheet").fetchSemanticsNode().boundsInRoot
        assertEquals(expectedSheetWidth, expandedBounds.width, 1f)
        assertEquals((windowWidth - expectedSheetWidth) / 2f, expandedBounds.left, 1f)
        val sortSurface = compose.onNodeWithTag("GalleryFilterSortSurface").fetchSemanticsNode().boundsInRoot
        assertTrue(sortSurface.left > 0f)
        assertTrue(sortSurface.right < windowWidth)
        val searchField = compose.onNodeWithText(strings.searchNotes).fetchSemanticsNode().boundsInRoot
        assertTrue(searchField.left > 0f)
        val tagSearchField = compose.onNodeWithText(strings.filterSearch).fetchSemanticsNode().boundsInRoot
        assertTrue(tagSearchField.left > 0f)
        assertTrue(tagSearchField.right < windowWidth)
        val categorySurface = compose.onAllNodesWithTag("GalleryFilterCategorySurface").get(0)
            .fetchSemanticsNode().boundsInRoot
        val categoryLabel = compose.onNodeWithText("コンテンツ", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val selectedContentFilterCount = compose.onNodeWithText("1", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(categoryLabel.left - categorySurface.left >= 8f)
        assertTrue(selectedContentFilterCount.right < categoryLabel.left)
        val sourceLabel = compose.onNodeWithText("Source", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val selectedSourceTagCount = compose.onNodeWithText("3", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(selectedSourceTagCount.right < sourceLabel.left)
        val searchAction = compose.onNodeWithContentDescription(strings.applySearch).fetchSemanticsNode().boundsInRoot
        val clearAction = compose.onNodeWithContentDescription(strings.clearAllSearchAndFilters).fetchSemanticsNode().boundsInRoot
        assertTrue(clearAction.right < windowWidth)
        assertTrue(searchAction.top < searchField.bottom)
        assertTrue(clearAction.left > searchAction.left)
        val fuzzySearchBounds = compose.onNodeWithText(strings.fuzzySearch)
            .fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText(strings.fuzzySearchTerm).assertIsDisplayed()
        compose.onNodeWithText(strings.exactWordTerm).assertIsDisplayed()
        assertTrue(fuzzySearchBounds.top > sortSurface.bottom)
        val descendingBounds = compose.onNodeWithText(strings.descending)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(fuzzySearchBounds.top > descendingBounds.bottom)
        assertTrue(fuzzySearchBounds.bottom < searchField.top)
        compose.onNodeWithText(strings.filters).performTouchInput {
            swipe(center, center + Offset(0f, 180f), durationMillis = 1_000)
        }
        compose.onNodeWithContentDescription(strings.close).assertDoesNotExist()
        compose.onNodeWithTag("GalleryFilterSheet").assertExists()
        compose.onNodeWithText(strings.filters).performTouchInput {
            swipe(center, center + Offset(0f, 450f), durationMillis = 500)
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("GalleryFilterSheet").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText(strings.filters).assertDoesNotExist()
        compose.onNodeWithContentDescription(strings.filters).performClick()
        compose.onNodeWithText(strings.searchNotes).performTextInput("fictional")
        compose.onNodeWithContentDescription(strings.applySearch).performClick()
        compose.onNodeWithTag("GalleryFilterSheet").assertDoesNotExist()
        compose.onNodeWithContentDescription(strings.filters).assertIsDisplayed()
    }

    @Test
    fun savesRestoresAndEditsNamedSearches() {
        val repository = object : GalleryRepository {
            override suspend fun queryPage(query: GalleryQuery) =
                RepositoryResult.Success(GalleryPage(emptyList(), 0, query.offset))
            override suspend fun categories(query: GalleryQuery) =
                RepositoryResult.Success(emptyList<GalleryCategory>())
            override suspend fun noteDetail(noteId: NoteId, safContent: ByteArray?) =
                RepositoryResult.Success<GalleryNoteDetail?>(null)
            override suspend fun notePath(noteId: NoteId) = RepositoryResult.Success<String?>(null)
            override suspend fun mediaLocation(mediaId: MediaId) =
                RepositoryResult.Success<String?>(null)
            override suspend fun thumbnail(mediaId: MediaId, size: Int) =
                RepositoryResult.Success<ByteArray?>(null)
        }
        val storedSearches = mutableStateOf(emptyList<SavedGallerySearch>())
        var holder: GalleryStateHolder? = null
        compose.setContent {
            val scope = rememberCoroutineScope()
            val stateHolder = remember {
                GalleryStateHolder(
                    repository,
                    scope,
                    GalleryQuery(
                        content = GalleryContent.NOTES,
                        searchText = "fictional",
                        includeTags = setOf("fictional/art"),
                        pageSize = 48,
                    ),
                )
            }
            SideEffect { holder = stateHolder }
            KaedeGalleryTheme(AppearanceSettings()) {
                GalleryScreen(
                    stateHolder,
                    onEntrySelected = {},
                    onSettings = {},
                    savedSearches = storedSearches.value,
                    onSaveSearch = { savedSearch ->
                        storedSearches.value = storedSearches.value
                            .filterNot { it.name.equals(savedSearch.name, ignoreCase = true) } +
                            savedSearch
                        RepositoryResult.Success(Unit)
                    },
                )
            }
        }
        val strings = EnglishGalleryStrings
        compose.onNodeWithContentDescription(strings.filters).performClick()
        compose.onNodeWithContentDescription(strings.savedSearches).performClick()
        compose.onNodeWithText(strings.saveSearch).performClick()
        compose.onNodeWithTag("saved-search-name").performTextInput("Fictional art")
        compose.onNodeWithText(strings.saveSearch).performClick()
        compose.waitUntil(5_000) {
            storedSearches.value.size == 1 && holder?.state?.value?.searching == false
        }

        compose.runOnIdle {
            holder?.filterByTag("fictional/other")
        }
        compose.onNodeWithContentDescription(strings.filters).performClick()
        compose.onNodeWithContentDescription(strings.savedSearches).performClick()
        compose.onNodeWithContentDescription(
            "${strings.viewSavedSearch}: Fictional art",
        ).performClick()
        compose.onNodeWithTag("saved-search-details").assertIsDisplayed()
        compose.onNodeWithText("fictional/art").assertIsDisplayed()
        compose.onNodeWithText(strings.editSavedSearch).performClick()
        compose.waitUntil(5_000) {
            holder?.state?.value?.query?.includeTags == setOf("fictional/art") &&
                compose.onAllNodesWithTag("editing-saved-search")
                    .fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            assertEquals("fictional", holder?.state?.value?.query?.searchText)
            assertEquals(48, holder?.state?.value?.query?.pageSize)
            holder?.filterByTag("fictional/other")
        }
        compose.onNodeWithContentDescription(strings.savedSearches).performClick()
        compose.onNodeWithText(
            "${strings.updateSavedSearch}: Fictional art",
        ).performClick()
        compose.waitUntil(5_000) {
            storedSearches.value.singleOrNull()?.includeTags == setOf("fictional/other")
        }

        compose.runOnIdle {
            holder?.filterByTag("fictional/art")
        }
        compose.onNodeWithContentDescription(strings.filters).performClick()
        compose.onNodeWithContentDescription(strings.savedSearches).performClick()
        compose.onNodeWithText("Fictional art").performClick()
        compose.waitUntil(5_000) {
            holder?.state?.value?.query?.includeTags == setOf("fictional/other")
        }
        compose.runOnIdle {
            assertEquals("fictional", holder?.state?.value?.query?.searchText)
            assertEquals(48, holder?.state?.value?.query?.pageSize)
        }
    }
}
