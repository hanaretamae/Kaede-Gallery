package com.hanaretamae.kaede.core.repository

import kotlinx.coroutines.flow.Flow

interface GallerySessionRepository {
    suspend fun openFilesystem(
        privateDataDirectory: String,
        vaultPath: String,
        indexPath: String,
        thumbnailCachePath: String,
        performScan: Boolean = true,
    ): RepositoryResult<GallerySessionHandle>

    suspend fun openSaf(
        privateDataDirectory: String,
        vaultUri: String,
        indexPath: String,
        thumbnailCachePath: String,
        filePaths: List<String>,
        batches: Flow<List<SafScanNote>>,
        performScan: Boolean = true,
    ): RepositoryResult<GallerySessionHandle> =
        RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
}

data class SafScanNote(
    val path: String,
    val modifiedNanos: Long,
    val size: Long,
    val content: ByteArray?,
)

data class GalleryScanSummary(
    val notesIndexed: Long,
    val warnings: Long,
)

interface GallerySessionHandle {
    val gallery: GalleryRepository
    val initialScan: GalleryScanSummary

    suspend fun rescan(): RepositoryResult<GalleryScanSummary>
    suspend fun rescanSaf(
        filePaths: List<String>,
        batches: Flow<List<SafScanNote>>,
    ): RepositoryResult<GalleryScanSummary> =
        RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
    fun close()
}
