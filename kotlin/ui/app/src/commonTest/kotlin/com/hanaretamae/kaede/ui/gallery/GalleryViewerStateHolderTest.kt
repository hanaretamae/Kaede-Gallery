package com.hanaretamae.kaede.ui.gallery

import com.hanaretamae.kaede.core.model.GalleryCategory
import com.hanaretamae.kaede.core.model.GalleryDetailLine
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.MediaSummary
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewerStateHolderTest {
    @Test
    fun loadsDetailsAndNavigatesMediaWithoutExposingLocationInStateErrors() = runTest {
        val first = media(21)
        val second = media(22)
        val repository = ViewerRepository(
            detail = detail(listOf(first, second)),
            locations = mapOf(first.id to "media/first.jpg", second.id to "media/second.jpg"),
        )
        val holder = GalleryViewerStateHolder(repository, this, note(first.id))

        holder.start()
        advanceUntilIdle()

        assertEquals("Sample note", holder.state.value.note?.title)
        assertEquals(first, holder.state.value.selectedMedia)
        assertEquals("media/first.jpg", holder.state.value.mediaLocation)
        assertFalse(holder.state.value.mediaLocationLoading)

        holder.showNextMedia()
        advanceUntilIdle()
        assertEquals(second, holder.state.value.selectedMedia)
        assertEquals("media/second.jpg", holder.state.value.mediaLocation)
        assertFalse(holder.state.value.mediaLocationLoading)

        holder.showNextMedia()
        assertEquals(second, holder.state.value.selectedMedia)
        holder.dispose()
    }

    @Test
    fun missingDetailReturnsSanitizedFailure() = runTest {
        val holder = GalleryViewerStateHolder(
            ViewerRepository(detail = null),
            this,
            note(MediaId(21)),
        )

        holder.start()
        advanceUntilIdle()

        assertEquals(RepositoryError.OPERATION_FAILED, holder.state.value.error)
        assertFalse(holder.state.value.loading)
        assertNull(holder.state.value.note)
    }

    @Test
    fun mediaEntrySelectsItsOwnMedia() = runTest {
        val first = media(21)
        val selected = media(22)
        val holder = GalleryViewerStateHolder(
            ViewerRepository(detail = detail(listOf(first, selected))),
            this,
            selected,
        )

        holder.start()
        advanceUntilIdle()

        assertEquals(selected, holder.state.value.selectedMedia)
        assertEquals(1, holder.state.value.selectedMediaIndex)
    }
}

private class ViewerRepository(
    private val detail: GalleryNoteDetail?,
    private val locations: Map<MediaId, String?> = emptyMap(),
) : GalleryRepository {
    override suspend fun queryPage(query: GalleryQuery) =
        RepositoryResult.Success(GalleryPage(emptyList(), 0, query.offset))

    override suspend fun categories(query: GalleryQuery) =
        RepositoryResult.Success(emptyList<GalleryCategory>())

    override suspend fun noteDetail(
        noteId: NoteId,
        safContent: ByteArray?,
    ) = RepositoryResult.Success(detail)

    override suspend fun mediaLocation(mediaId: MediaId) =
        RepositoryResult.Success(locations[mediaId])

    override suspend fun thumbnail(mediaId: MediaId, size: Int): RepositoryResult<ByteArray?> =
        RepositoryResult.Success(null)
}

private fun note(representative: MediaId) = NoteSummary(
    id = NoteId(7),
    path = "notes/sample.md",
    title = "Sample note",
    representativeMediaId = representative,
    mediaCount = 2,
    videoCount = 0,
    memoCount = 1,
    relatedCount = 1,
)

private fun media(id: Long) = MediaSummary(
    id = MediaId(id),
    noteId = NoteId(7),
    notePath = "notes/sample.md",
    isVideo = false,
    exists = true,
    mediaCount = 2,
    memoCount = 1,
    relatedCount = 1,
)

private fun detail(media: List<MediaSummary>) = GalleryNoteDetail(
    id = NoteId(7),
    path = "notes/sample.md",
    title = "Sample note",
    author = "Fictional author",
    authorUrl = null,
    url = null,
    published = null,
    created = null,
    updated = null,
    tags = emptyList(),
    bodyText = "Fictional body text",
    memoLines = listOf(GalleryDetailLine("Fictional memo", emptyList(), false, 0, null)),
    relatedLines = emptyList(),
    media = media,
)
