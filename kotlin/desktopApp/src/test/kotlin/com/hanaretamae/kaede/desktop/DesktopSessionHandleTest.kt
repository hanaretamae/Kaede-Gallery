package com.hanaretamae.kaede.desktop

import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.GalleryScanSummary
import com.hanaretamae.kaede.core.repository.GallerySessionHandle
import com.hanaretamae.kaede.core.repository.RepositoryResult
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSessionHandleTest {
    @Test
    fun closesNativeSessionOnlyOnce() {
        val delegate = FakeSessionHandle()
        val handle = DesktopSessionHandle(delegate)

        handle.close()
        handle.close()

        assertEquals(1, delegate.closeCalls)
    }

    private class FakeSessionHandle : GallerySessionHandle {
        override val gallery: GalleryRepository
            get() = error("Gallery is not used in this test")
        override val initialScan = GalleryScanSummary(notesIndexed = 0, warnings = 0)
        var closeCalls = 0
            private set

        override suspend fun rescan(): RepositoryResult<GalleryScanSummary> =
            error("Rescan is not used in this test")

        override fun close() {
            closeCalls++
        }
    }
}
