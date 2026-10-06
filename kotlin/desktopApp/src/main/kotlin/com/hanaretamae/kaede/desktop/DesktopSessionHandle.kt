package com.hanaretamae.kaede.desktop

import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.GalleryScanSummary
import com.hanaretamae.kaede.core.repository.GallerySessionHandle
import com.hanaretamae.kaede.core.repository.RepositoryResult
import java.util.concurrent.atomic.AtomicBoolean

internal class DesktopSessionHandle(
    private val delegate: GallerySessionHandle,
) : GallerySessionHandle {
    private val closed = AtomicBoolean(false)

    override val gallery: GalleryRepository
        get() = delegate.gallery

    override val initialScan: GalleryScanSummary
        get() = delegate.initialScan

    override suspend fun rescan(): RepositoryResult<GalleryScanSummary> =
        delegate.rescan()

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            delegate.close()
        }
    }
}
