package com.hanaretamae.kaede.ui.gallery

import com.hanaretamae.kaede.core.model.GalleryContent
import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.GalleryCategory
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.model.VirtualFilter
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryStateHolderTest {
    @Test
    fun visibleRangeTracksConfiguredPageRatherThanPrefetchedItems() {
        assertEquals(1L..24L, galleryVisibleRange(0, 80, 100, 5, 24))
        assertEquals(25L..48L, galleryVisibleRange(0, 80, 100, 30, 24))
        assertEquals(73L..80L, galleryVisibleRange(72, 8, 80, 7, 24))
        assertEquals(null, galleryVisibleRange(0, 0, 80, 0, 24))
    }

    @Test
    fun loadsPagesAndAppendsUntilTotalCount() = runTest {
        val repository = FakeGalleryRepository { query ->
            val item = note(query.offset + 1)
            RepositoryResult.Success(
                GalleryPage(
                    entries = listOf(item),
                    totalCount = 2,
                    offset = query.offset,
                ),
            )
        }
        val holder = GalleryStateHolder(repository, this)

        holder.start()
        advanceUntilIdle()
        assertEquals(listOf(0L), repository.queries.map { it.offset })
        assertTrue(holder.state.value.canLoadMore)

        holder.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(0L, 1L), repository.queries.map { it.offset })
        assertEquals(listOf(NoteId(1), NoteId(2)), holder.state.value.entries.map { (it as NoteSummary).id })
        assertFalse(holder.state.value.canLoadMore)
    }

    @Test
    fun jumpLoadsPreviousAndTargetPagesThenContinuesForward() = runTest {
        val repository = FakeGalleryRepository { query ->
            RepositoryResult.Success(
                GalleryPage(
                    entries = (query.offset + 1..minOf(query.offset + query.pageSize, 18))
                        .map(::note),
                    totalCount = 18,
                    offset = query.offset,
                ),
            )
        }
        val holder = GalleryStateHolder(
            repository,
            this,
            initialQuery = GalleryQuery(pageSize = 3),
        )

        holder.start()
        advanceUntilIdle()
        holder.jumpTo(10)
        advanceUntilIdle()

        assertEquals(listOf(0L, 6L, 9L), repository.queries.map { it.offset })
        assertEquals(6L, holder.state.value.pageOffset)
        assertEquals(
            (7L..12L).map(::noteId),
            holder.state.value.entries.map { (it as NoteSummary).id },
        )
        assertTrue(holder.state.value.canLoadMore)

        holder.loadMore()
        advanceUntilIdle()

        assertEquals(12L, repository.queries.last().offset)
        assertEquals(6L, holder.state.value.pageOffset)
        assertEquals(NoteId(15), (holder.state.value.entries.last() as NoteSummary).id)
    }

    @Test
    fun loadsPreviousPageAndKeepsTheWindowContiguous() = runTest {
        val repository = FakeGalleryRepository { query ->
            RepositoryResult.Success(
                GalleryPage(
                    entries = (query.offset + 1..minOf(query.offset + query.pageSize, 20))
                        .map(::note),
                    totalCount = 20,
                    offset = query.offset,
                ),
            )
        }
        val holder = GalleryStateHolder(repository, this, GalleryQuery(pageSize = 3))
        holder.start()
        advanceUntilIdle()
        holder.jumpTo(10)
        advanceUntilIdle()

        holder.loadPrevious()
        advanceUntilIdle()

        assertEquals(3L, holder.state.value.pageOffset)
        assertEquals(
            (4L..12L).map(::noteId),
            holder.state.value.entries.map { (it as NoteSummary).id },
        )
    }

    @Test
    fun cancellingJumpRestoresPreviouslyLoadedResults() = runTest {
        val repository = FakeGalleryRepository { query ->
            RepositoryResult.Success(
                GalleryPage(
                    entries = (query.offset + 1..minOf(query.offset + query.pageSize, 12))
                        .map(::note),
                    totalCount = 12,
                    offset = query.offset,
                ),
            )
        }
        val holder = GalleryStateHolder(
            repository,
            this,
            GalleryQuery(pageSize = 3),
        )
        holder.start()
        advanceUntilIdle()
        val originalIds = holder.state.value.entries.map { (it as NoteSummary).id }

        holder.jumpTo(10)
        holder.cancelJump()
        advanceUntilIdle()

        assertEquals(originalIds, holder.state.value.entries.map { (it as NoteSummary).id })
        assertEquals(0L, holder.state.value.pageOffset)
        assertFalse(holder.state.value.loading)
    }

    @Test
    fun rejectsJumpWhenTargetPageNoLongerContainsRequestedItem() = runTest {
        val repository = FakeGalleryRepository { query ->
            val currentTotal = if (query.offset == 9L) 4L else 12L
            RepositoryResult.Success(
                GalleryPage(
                    entries = if (query.offset >= currentTotal) {
                        emptyList()
                    } else {
                        (query.offset + 1..minOf(query.offset + query.pageSize, currentTotal))
                            .map(::note)
                    },
                    totalCount = currentTotal,
                    offset = query.offset,
                ),
            )
        }
        val holder = GalleryStateHolder(
            repository,
            this,
            initialQuery = GalleryQuery(pageSize = 3),
        )
        holder.start()
        advanceUntilIdle()

        holder.jumpTo(10)
        advanceUntilIdle()

        assertEquals(RepositoryError.INVALID_REQUEST, holder.state.value.error)
        assertFalse(holder.state.value.loading)
    }

    @Test
    fun rejectsJumpOutsideKnownResultRange() = runTest {
        val repository = FakeGalleryRepository {
            RepositoryResult.Success(
                GalleryPage((1L..4L).map(::note), totalCount = 4, offset = it.offset),
            )
        }
        val holder = GalleryStateHolder(repository, this)
        holder.start()
        advanceUntilIdle()

        holder.jumpTo(5)

        assertEquals(1, repository.queries.size)
        assertEquals(RepositoryError.INVALID_REQUEST, holder.state.value.error)
    }

    @Test
    fun rejectsJumpWhenResultCountShrinksAfterThePositionWasValidated() = runTest {
        var currentTotal = 12L
        val repository = FakeGalleryRepository { query ->
            RepositoryResult.Success(
                GalleryPage(
                    entries = (query.offset + 1..currentTotal).map(::note),
                    totalCount = currentTotal,
                    offset = query.offset,
                ),
            )
        }
        val holder = GalleryStateHolder(repository, this)
        holder.start()
        advanceUntilIdle()

        currentTotal = 4
        holder.jumpTo(10)
        advanceUntilIdle()

        assertEquals(RepositoryError.INVALID_REQUEST, holder.state.value.error)
        assertFalse(holder.state.value.loading)
        assertTrue(holder.state.value.entries.isEmpty())
    }

    @Test
    fun debouncesSearchAndResetsPaging() = runTest {
        val repository = FakeGalleryRepository { query ->
            RepositoryResult.Success(
                GalleryPage(emptyList(), totalCount = 0, offset = query.offset),
            )
        }
        val holder = GalleryStateHolder(
            repository,
            this,
            initialQuery = GalleryQuery(offset = 12),
        )

        holder.start()
        advanceUntilIdle()
        holder.setQuery(
            holder.state.value.query.copy(searchText = "#tag"),
            debounceSearch = true,
        )
        advanceTimeBy(249)
        assertEquals(1, repository.queries.size)
        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(2, repository.queries.size)
        assertEquals("#tag", repository.queries.last().searchText)
        assertEquals(0, repository.queries.last().offset)
    }

    @Test
    fun exposesSanitizedRepositoryFailure() = runTest {
        val repository = FakeGalleryRepository {
            RepositoryResult.Failure(RepositoryError.INDEX_UNAVAILABLE)
        }
        val holder = GalleryStateHolder(repository, this)

        holder.start()
        advanceUntilIdle()

        assertEquals(RepositoryError.INDEX_UNAVAILABLE, holder.state.value.error)
        assertFalse(holder.state.value.loading)
    }

    @Test
    fun rescanRefreshesGalleryOnlyAfterSuccessfulScan() = runTest {
        val repository = FakeGalleryRepository {
            RepositoryResult.Success(GalleryPage(emptyList(), totalCount = 0, offset = it.offset))
        }
        val holder = GalleryStateHolder(repository, this)
        holder.start()
        advanceUntilIdle()
        var scanCount = 0

        holder.rescan {
            scanCount++
            RepositoryResult.Success(Unit)
        }
        assertTrue(holder.state.value.rescanning)
        advanceUntilIdle()

        assertEquals(1, scanCount)
        assertEquals(2, repository.queries.size)
        assertFalse(holder.state.value.rescanning)
        assertEquals(null, holder.state.value.rescanError)
    }

    @Test
    fun failedRescanKeepsCurrentGalleryAndExposesSanitizedError() = runTest {
        val repository = FakeGalleryRepository {
            RepositoryResult.Success(GalleryPage(emptyList(), totalCount = 0, offset = it.offset))
        }
        val holder = GalleryStateHolder(repository, this)
        holder.start()
        advanceUntilIdle()

        holder.rescan {
            RepositoryResult.Failure(RepositoryError.INDEX_UNAVAILABLE)
        }
        advanceUntilIdle()

        assertEquals(1, repository.queries.size)
        assertFalse(holder.state.value.rescanning)
        assertEquals(RepositoryError.INDEX_UNAVAILABLE, holder.state.value.rescanError)
    }

    @Test
    fun cyclesTagSelectionModesAndClearsAllFilters() = runTest {
        val repository = FakeGalleryRepository {
            RepositoryResult.Success(GalleryPage(emptyList(), totalCount = 0, offset = 0))
        }
        val holder = GalleryStateHolder(repository, this)

        holder.cycleTagSelection("type/image")
        assertEquals(setOf("type/image"), holder.state.value.query.includeTags)
        holder.cycleTagSelection("type/image")
        assertEquals(setOf("type/image"), holder.state.value.query.andTags)
        holder.cycleTagSelection("type/image")
        assertEquals(setOf("type/image"), holder.state.value.query.excludedTags)
        holder.setVirtualFilter(VirtualFilter.HAS_VIDEO, enabled = true)
        assertEquals(setOf(VirtualFilter.HAS_VIDEO), holder.state.value.query.virtualFilters)

        holder.clearFilters()

        assertTrue(holder.state.value.query.includeTags.isEmpty())
        assertTrue(holder.state.value.query.andTags.isEmpty())
        assertTrue(holder.state.value.query.excludedTags.isEmpty())
        assertTrue(holder.state.value.query.virtualFilters.isEmpty())
    }
}

private class FakeGalleryRepository(
    private val query: suspend (GalleryQuery) -> RepositoryResult<GalleryPage>,
) : GalleryRepository {
    val queries = mutableListOf<GalleryQuery>()

    override suspend fun queryPage(query: GalleryQuery): RepositoryResult<GalleryPage> {
        queries += query
        return this.query(query)
    }

    override suspend fun categories(
        query: GalleryQuery,
    ): RepositoryResult<List<GalleryCategory>> = RepositoryResult.Success(emptyList())

    override suspend fun noteDetail(
        noteId: NoteId,
        safContent: ByteArray?,
    ): RepositoryResult<GalleryNoteDetail?> = RepositoryResult.Success(null)

    override suspend fun notePath(noteId: NoteId): RepositoryResult<String?> =
        RepositoryResult.Success(null)

    override suspend fun mediaLocation(mediaId: MediaId): RepositoryResult<String?> =
        RepositoryResult.Success(null)

    override suspend fun thumbnail(mediaId: MediaId, size: Int): RepositoryResult<ByteArray?> =
        RepositoryResult.Success(null)
}

private fun note(id: Long) = NoteSummary(
    id = NoteId(id),
    path = "notes/$id.md",
    title = "Note $id",
    representativeMediaId = null,
    mediaCount = 0,
    videoCount = 0,
    memoCount = 0,
    relatedCount = 0,
)

private fun noteId(id: Long) = NoteId(id)
