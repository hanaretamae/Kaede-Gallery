package com.hanaretamae.kaede.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryModelsTest {
    @Test
    fun queryHasBoundedPagingDefaults() {
        val query = GalleryQuery()

        assertEquals(0, query.offset)
        assertEquals(24, query.pageSize)
        assertFailsWith<IllegalArgumentException> { GalleryQuery(offset = -1) }
        assertEquals(500, GalleryQuery.MAX_PAGE_SIZE)
        assertFailsWith<IllegalArgumentException> {
            GalleryQuery(pageSize = GalleryQuery.MAX_PAGE_SIZE + 1)
        }
    }

    @Test
    fun pageReportsWhetherMoreEntriesRemain() {
        val firstPage = GalleryPage(entries = emptyList(), totalCount = 25, offset = 0)
        val finalPage = GalleryPage(entries = emptyList(), totalCount = 24, offset = 24)

        assertTrue(firstPage.hasNextPage)
        assertFalse(finalPage.hasNextPage)
    }
}
