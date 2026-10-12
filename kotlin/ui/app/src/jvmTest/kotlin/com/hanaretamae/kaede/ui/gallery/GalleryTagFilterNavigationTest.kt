package com.hanaretamae.kaede.ui.gallery

import com.hanaretamae.kaede.core.model.GalleryCategory
import com.hanaretamae.kaede.core.model.GalleryContent
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.GallerySortField
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.model.SortDirection
import com.hanaretamae.kaede.core.model.VirtualFilter
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryTagFilterNavigationTest {
    @Test
    fun filteringByViewerTagReplacesTagModesAndRetainsOtherQueryOptions() = runTest {
        val initialQuery = GalleryQuery(
            content = GalleryContent.MEDIA,
            searchText = "fictional",
            fuzzySearchEnabled = false,
            includeTags = setOf("fictional/old"),
            andTags = setOf("fictional/required"),
            excludedTags = setOf("fictional/hidden"),
            virtualFilters = setOf(VirtualFilter.HAS_VIDEO),
            sortField = GallerySortField.PUBLISHED,
            sortDirection = SortDirection.ASCENDING,
            pageSize = 48,
        )
        val repository = object : GalleryRepository {
            override suspend fun queryPage(query: GalleryQuery) =
                RepositoryResult.Success(GalleryPage(emptyList(), 0, query.offset))

            override suspend fun categories(query: GalleryQuery) =
                RepositoryResult.Success(emptyList<GalleryCategory>())

            override suspend fun noteDetail(noteId: NoteId, safContent: ByteArray?) =
                RepositoryResult.Success<GalleryNoteDetail?>(null)

            override suspend fun notePath(noteId: NoteId) =
                RepositoryResult.Success<String?>(null)

            override suspend fun mediaLocation(mediaId: MediaId) =
                RepositoryResult.Success<String?>(null)

            override suspend fun thumbnail(mediaId: MediaId, size: Int) =
                RepositoryResult.Success<ByteArray?>(null)
        }
        val holder = GalleryStateHolder(repository, this, initialQuery)

        holder.filterByTag("fictional/art")

        assertEquals(
            initialQuery.copy(
                includeTags = setOf("fictional/art"),
                andTags = emptySet(),
                excludedTags = emptySet(),
            ),
            holder.state.value.query,
        )
        holder.dispose()
    }
}
