package com.hanaretamae.kaede.core.settings

import com.hanaretamae.kaede.core.model.GalleryContent
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.GallerySortField
import com.hanaretamae.kaede.core.model.SortDirection
import com.hanaretamae.kaede.core.model.VirtualFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class SavedGallerySearchCodecTest {
    @Test
    fun storesCompleteSearchCriteriaWithoutPersistingPagingPosition() {
        val query = GalleryQuery(
            content = GalleryContent.MEDIA,
            searchText = "fictional",
            fuzzySearchEnabled = false,
            includeTags = setOf("fictional/art"),
            andTags = setOf("fictional/blue"),
            excludedTags = setOf("fictional/private"),
            virtualFilters = setOf(VirtualFilter.HAS_VIDEO, VirtualFilter.HAS_MEMO),
            sortField = GallerySortField.PUBLISHED,
            sortDirection = SortDirection.ASCENDING,
            offset = 96,
            pageSize = 48,
        )
        val savedSearch = SavedGallerySearch.fromQuery("  Fictional art  ", query)

        val decoded = assertNotNull(
            SavedGallerySearchCodec.decodeStorage(
                SavedGallerySearchCodec.encodeStorage(listOf(savedSearch)),
            ),
        ).single()

        assertEquals("Fictional art", decoded.name)
        assertEquals(query.copy(offset = 0), decoded.toQuery(pageSize = 48))
        val imported = assertNotNull(
            SettingsTransferCodec.decode(
                SettingsTransferCodec.encode(GallerySettings(savedSearches = listOf(savedSearch))),
            ),
        )
        assertEquals(listOf(savedSearch), imported.savedSearches)
    }

    @Test
    fun rejectsDuplicateNamesAndMalformedStoredSearches() {
        val query = GalleryQuery(searchText = "fictional")
        val savedSearch = SavedGallerySearch.fromQuery("Preset", query)

        assertFalse(SavedGallerySearchCodec.isValid(listOf(savedSearch, savedSearch.copy(name = "preset"))))
        assertNull(SavedGallerySearchCodec.decodeStorage("""[{"name":"incomplete"}]"""))
    }
}
