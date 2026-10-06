package com.hanaretamae.kaede.core.rust

import com.hanaretamae.kaede.core.repository.GalleryScanSummary
import com.hanaretamae.kaede.core.repository.GallerySessionHandle
import com.hanaretamae.kaede.core.repository.GallerySessionRepository
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.repository.SafScanNote
import com.hanaretamae.kaede.core.model.GalleryQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

class RustGallerySessionRepository : GallerySessionRepository {
    override suspend fun openFilesystem(
        privateDataDirectory: String,
        vaultPath: String,
        indexPath: String,
        thumbnailCachePath: String,
        performScan: Boolean,
    ): RepositoryResult<GallerySessionHandle> = withContext(Dispatchers.IO) {
        if (
            vaultPath.startsWith("content://") ||
            privateDataDirectory.isBlank() ||
            indexPath.isBlank() ||
            thumbnailCachePath.isBlank()
        ) {
            return@withContext RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        }

        val session = try {
            preparePrivateData(privateDataDirectory, vaultPath)
            GallerySession.open(vaultPath, indexPath, thumbnailCachePath)
        } catch (error: GalleryException) {
            return@withContext RepositoryResult.Failure(error.toRepositoryError())
        }

        var retained = false
        try {
            val summary = if (performScan) {
                try {
                    session.scan().toModel()
                        ?: return@withContext RepositoryResult.Failure(
                            RepositoryError.OPERATION_FAILED,
                        )
                } catch (error: GalleryException) {
                    return@withContext RepositoryResult.Failure(error.toRepositoryError())
                }
            } else {
                when (
                    val page = RustGalleryRepository(session)
                        .queryPage(GalleryQuery(pageSize = 1))
                ) {
                    is RepositoryResult.Failure -> return@withContext page
                    is RepositoryResult.Success -> GalleryScanSummary(
                        notesIndexed = page.value.totalCount,
                        warnings = 0,
                    )
                }
            }
            retained = true
            RepositoryResult.Success(
                RustGallerySessionHandle(
                    session = session,
                    initialScan = summary,
                    isSaf = false,
                ),
            )
        } finally {
            if (!retained) session.destroy()
        }
    }

    override suspend fun openSaf(
        privateDataDirectory: String,
        vaultUri: String,
        indexPath: String,
        thumbnailCachePath: String,
        filePaths: List<String>,
        batches: Flow<List<SafScanNote>>,
        performScan: Boolean,
    ): RepositoryResult<GallerySessionHandle> = withContext(Dispatchers.IO) {
        if (
            !vaultUri.startsWith("content://") ||
            privateDataDirectory.isBlank() ||
            indexPath.isBlank() ||
            thumbnailCachePath.isBlank()
        ) {
            return@withContext RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        }
        val session = try {
            preparePrivateData(privateDataDirectory, vaultUri)
            GallerySession.open(vaultUri, indexPath, thumbnailCachePath)
        } catch (error: GalleryException) {
            return@withContext RepositoryResult.Failure(error.toRepositoryError())
        }
        var retained = false
        try {
            val scan = if (performScan) {
                scanSaf(session, filePaths, batches)
            } else {
                when (
                    val page = RustGalleryRepository(session)
                        .queryPage(GalleryQuery(pageSize = 1))
                ) {
                    is RepositoryResult.Failure -> page
                    is RepositoryResult.Success -> RepositoryResult.Success(
                        GalleryScanSummary(page.value.totalCount, 0),
                    )
                }
            }
            when (scan) {
                is RepositoryResult.Failure -> scan
                is RepositoryResult.Success -> {
                    retained = true
                    RepositoryResult.Success(
                        RustGallerySessionHandle(
                            session = session,
                            initialScan = scan.value,
                            isSaf = true,
                        ),
                    )
                }
            }
        } finally {
            if (!retained) session.destroy()
        }
    }
}

private class RustGallerySessionHandle(
    private val session: GallerySession,
    override val initialScan: GalleryScanSummary,
    private val isSaf: Boolean,
) : GallerySessionHandle {
    override val gallery = RustGalleryRepository(session)

    override suspend fun rescan(): RepositoryResult<GalleryScanSummary> =
        withContext(Dispatchers.IO) {
            try {
                val summary = session.scan().toModel()
                    ?: return@withContext RepositoryResult.Failure(
                        RepositoryError.OPERATION_FAILED,
                    )
                RepositoryResult.Success(summary)
            } catch (error: GalleryException) {
                RepositoryResult.Failure(error.toRepositoryError())
            }
        }

    override suspend fun rescanSaf(
        filePaths: List<String>,
        batches: Flow<List<SafScanNote>>,
    ): RepositoryResult<GalleryScanSummary> {
        if (!isSaf) return RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        return scanSaf(session, filePaths, batches)
    }

    override fun close() {
        session.destroy()
    }
}

private suspend fun scanSaf(
    session: GallerySession,
    filePaths: List<String>,
    batches: Flow<List<SafScanNote>>,
): RepositoryResult<GalleryScanSummary> {
    var completed = false
    try {
        session.beginSafScan(filePaths)
        batches.collect { batch ->
            if (batch.size > MAX_SAF_BATCH_NOTES ||
                batch.any { (it.content?.size ?: 0) > MAX_SAF_NOTE_BYTES } ||
                batch.sumOf { it.content?.size?.toLong() ?: 0L } > MAX_SAF_BATCH_BYTES
            ) {
                throw InvalidSafBatch()
            }
            session.appendSafScanBatch(
                batch.map { note ->
                    SafNote(
                        path = note.path,
                        modifiedNanos = note.modifiedNanos,
                        size = note.size,
                        content = note.content,
                    )
                },
            )
        }
        val summary = session.finishSafScan().toModel()
            ?: return RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
        completed = true
        return RepositoryResult.Success(summary)
    } catch (error: GalleryException) {
        return RepositoryResult.Failure(error.toRepositoryError())
    } catch (_: InvalidSafBatch) {
        return RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
    } catch (error: CancellationException) {
        throw error
    } finally {
        if (!completed) session.cancelSafScan()
    }
}

private class InvalidSafBatch : RuntimeException()

private fun ScanReport.toModel(): GalleryScanSummary? {
    val indexed = notesIndexed.toLongOrNull() ?: return null
    val warningCount = warnings.toLongOrNull() ?: return null
    return GalleryScanSummary(indexed, warningCount)
}

private fun GalleryException.toRepositoryError(): RepositoryError = when (this) {
    is GalleryException.VaultUnavailable -> RepositoryError.VAULT_UNAVAILABLE
    is GalleryException.InvalidVault -> RepositoryError.INVALID_VAULT
    is GalleryException.StorageUnavailable -> RepositoryError.STORAGE_UNAVAILABLE
    is GalleryException.IndexUnavailable -> RepositoryError.INDEX_UNAVAILABLE
    is GalleryException.InvalidRequest -> RepositoryError.INVALID_REQUEST
    is GalleryException.OperationFailed -> RepositoryError.OPERATION_FAILED
}

private fun ULong.toLongOrNull(): Long? =
    if (this <= Long.MAX_VALUE.toULong()) toLong() else null

private const val MAX_SAF_BATCH_NOTES = 128
private const val MAX_SAF_BATCH_BYTES = 16 * 1024 * 1024
private const val MAX_SAF_NOTE_BYTES = 2 * 1024 * 1024
