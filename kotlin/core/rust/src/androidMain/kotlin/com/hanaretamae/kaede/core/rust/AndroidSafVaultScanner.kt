package com.hanaretamae.kaede.core.rust

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.DocumentsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayDeque

data class AndroidSafScanSnapshot(
    val filePaths: List<String>,
    val batches: Flow<List<com.hanaretamae.kaede.core.repository.SafScanNote>>,
)

class AndroidSafVaultScanner(context: Context) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    fun hasPersistedReadPermission(treeUri: Uri): Boolean = try {
        resolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }
    } catch (_: SecurityException) {
        false
    }

    suspend fun snapshot(treeUri: Uri): AndroidSafScanSnapshot = withContext(Dispatchers.IO) {
        val startedAt = SystemClock.elapsedRealtime()
        validateTreeUri(treeUri)
        val treeDocumentId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: IllegalArgumentException) {
            throw SafAccessException()
        }
        validateDocumentId(treeDocumentId)
        val filePaths = ArrayList<String>()
        val notes = ArrayList<NoteDocument>()
        val pendingDirectories = ArrayDeque<Directory>()
        pendingDirectories.add(Directory(treeDocumentId, "", 0))
        var aggregatePathBytes = 0L
        var aggregateDocumentIdBytes = 0L
        var aggregateDocumentUriBytes = 0L

        while (pendingDirectories.isNotEmpty()) {
            val parent = pendingDirectories.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                parent.documentId,
            )
            val cursor = query(childrenUri)
            cursor.use {
                val idIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                if (idIndex < 0 || nameIndex < 0 || mimeIndex < 0) {
                    throw SafAccessException()
                }
                val sizeIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = it.getColumnIndex(
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                )
                while (it.moveToNext()) {
                    if (filePaths.size >= MAX_DOCUMENTS) throw SafAccessException()
                    val name = it.getString(nameIndex) ?: throw SafAccessException()
                    validateSegment(name)
                    val relativePath = if (parent.relativePath.isEmpty()) {
                        name
                    } else {
                        "${parent.relativePath}/$name"
                    }
                    if (parent.depth + 1 > MAX_DEPTH ||
                        relativePath.toByteArray(Charsets.UTF_8).size > MAX_PATH_BYTES
                    ) {
                        throw SafAccessException()
                    }
                    aggregatePathBytes += relativePath.toByteArray(Charsets.UTF_8).size
                    if (aggregatePathBytes > MAX_AGGREGATE_PATH_BYTES) {
                        throw SafAccessException()
                    }
                    filePaths.add(relativePath)
                    val childDocumentId = it.getString(idIndex) ?: throw SafAccessException()
                    validateDocumentId(childDocumentId)
                    aggregateDocumentIdBytes += childDocumentId.toByteArray(
                        Charsets.UTF_8,
                    ).size
                    if (aggregateDocumentIdBytes > MAX_AGGREGATE_PATH_BYTES) {
                        throw SafAccessException()
                    }
                    when (it.getString(mimeIndex)) {
                        DocumentsContract.Document.MIME_TYPE_DIR -> {
                            pendingDirectories.add(
                                Directory(childDocumentId, relativePath, parent.depth + 1),
                            )
                        }
                        else -> if (relativePath.endsWith(".md", ignoreCase = true)) {
                            val size = if (sizeIndex >= 0 && !it.isNull(sizeIndex)) {
                                it.getLong(sizeIndex).coerceAtLeast(0)
                            } else {
                                0L
                            }
                            val modifiedMillis = if (
                                modifiedIndex >= 0 && !it.isNull(modifiedIndex)
                            ) {
                                it.getLong(modifiedIndex).coerceAtLeast(0)
                            } else {
                                0L
                            }
                            val modifiedNanos = try {
                                Math.multiplyExact(modifiedMillis, NANOS_PER_MILLI)
                            } catch (_: ArithmeticException) {
                                0L
                            }
                            val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                                treeUri,
                                childDocumentId,
                            )
                            aggregateDocumentUriBytes += documentUri.toString()
                                .toByteArray(Charsets.UTF_8).size
                            if (aggregateDocumentUriBytes > MAX_AGGREGATE_PATH_BYTES) {
                                throw SafAccessException()
                            }
                            notes.add(
                                NoteDocument(
                                    relativePath,
                                    documentUri,
                                    modifiedNanos,
                                    size,
                                ),
                            )
                        }
                    }
                }
            }
        }

        Log.d(
            LOG_TAG,
            "SAF enumeration durationMs=${SystemClock.elapsedRealtime() - startedAt}",
        )
        AndroidSafScanSnapshot(
            filePaths = filePaths,
            batches = readBatches(notes),
        )
    }

    suspend fun resolveMedia(treeUri: Uri, relativePath: String): Uri =
        withContext(Dispatchers.IO) {
            validateTreeUri(treeUri)
            val segments = validateRelativePath(relativePath)
            var parentDocumentId = try {
                DocumentsContract.getTreeDocumentId(treeUri)
            } catch (_: IllegalArgumentException) {
                throw SafAccessException()
            }
            for ((index, segment) in segments.withIndex()) {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    parentDocumentId,
                )
                val cursor = query(childrenUri)
                val nextDocumentId = cursor.use {
                    val idIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = it.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    )
                    if (idIndex < 0 || nameIndex < 0) throw SafAccessException()
                    var found: String? = null
                    var childrenSeen = 0
                    while (it.moveToNext()) {
                        if (childrenSeen >= MAX_DOCUMENTS) throw SafAccessException()
                        childrenSeen += 1
                        if (it.getString(nameIndex) == segment) {
                            if (found != null) throw SafAccessException()
                            found = it.getString(idIndex)
                        }
                    }
                    found
                } ?: throw SafAccessException()
                parentDocumentId = nextDocumentId
                if (index == segments.lastIndex) {
                return@withContext DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    nextDocumentId,
                )
                }
            }
            throw SafAccessException()
        }

    suspend fun readNoteContent(treeUri: Uri, relativePath: String): ByteArray =
        withContext(Dispatchers.IO) {
            if (!relativePath.endsWith(".md", ignoreCase = true)) throw SafAccessException()
            try {
                val uri = resolveMedia(treeUri, relativePath)
                val input = resolver.openInputStream(uri) ?: throw SafAccessException()
                val bytes = input.use { it.readBounded(MAX_NOTE_BYTES) }
                if (bytes.size > MAX_NOTE_BYTES) throw SafAccessException()
                bytes
            } catch (_: SecurityException) {
                throw SafAccessException()
            } catch (_: IOException) {
                throw SafAccessException()
            }
        }

    suspend fun decodeImage(
        treeUri: Uri,
        relativePath: String,
        maxDimension: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (maxDimension !in 1..MAX_IMAGE_DIMENSION) throw SafAccessException()
        val uri = resolveMedia(treeUri, relativePath)
        val input = resolver.openInputStream(uri) ?: return@withContext null
        val bytes = input.use { it.readBounded(MAX_MEDIA_BYTES) }
        if (bytes.size > MAX_MEDIA_BYTES) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (
            width <= 0 ||
            height <= 0 ||
            width > MAX_IMAGE_DIMENSION ||
            height > MAX_IMAGE_DIMENSION ||
            width.toLong() * height > MAX_IMAGE_PIXELS
        ) {
            return@withContext null
        }
        val sampleSize = calculateSampleSize(width, height, maxDimension)
        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    }

    suspend fun decodeVideoFrame(
        treeUri: Uri,
        relativePath: String,
        maxDimension: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 ||
            maxDimension !in 1..MAX_IMAGE_DIMENSION
        ) {
            return@withContext null
        }
        val uri = resolveMedia(treeUri, relativePath)
        val descriptor = try {
            resolver.openFileDescriptor(uri, "r") ?: return@withContext null
        } catch (_: SecurityException) {
            throw SafAccessException()
        } catch (_: IOException) {
            throw SafAccessException()
        }
        descriptor.use { file ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.fileDescriptor)
                retriever.getScaledFrameAtTime(
                    0,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    maxDimension,
                    maxDimension,
                )
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: IllegalStateException) {
                null
            } finally {
                retriever.release()
            }
        }
    }

    private fun readBatches(
        documents: List<NoteDocument>,
    ): Flow<List<com.hanaretamae.kaede.core.repository.SafScanNote>> = flow {
        val startedAt = SystemClock.elapsedRealtime()
        var scannedContentBytes = 0L
        try {
            for (batch in documents.chunked(MAX_BATCH_NOTES)) {
                val result = ArrayList<com.hanaretamae.kaede.core.repository.SafScanNote>(
                    batch.size,
                )
                var reservedBatchBytes = 0L
                val reads = coroutineScope {
                    val readLimit = Semaphore(AndroidSafLimits.MAX_CONCURRENT_NOTE_READS)
                    batch.map { document ->
                        val expectedBytes = if (document.size > 0) {
                            document.size
                        } else {
                            MAX_NOTE_BYTES.toLong()
                        }
                        val remaining = minOf(
                            MAX_BATCH_BYTES - reservedBatchBytes,
                            MAX_SCAN_BYTES - scannedContentBytes,
                        ).coerceAtLeast(0)
                        val allocation = minOf(
                            expectedBytes,
                            MAX_NOTE_BYTES.toLong(),
                            remaining,
                        )
                        reservedBatchBytes += allocation
                        async(Dispatchers.IO) {
                            readLimit.withPermit {
                                readNote(document, allocation)
                            }
                        }
                    }
                        .awaitAll()
                }
                for (read in reads) {
                    val content = read.content
                    if (content != null) {
                        scannedContentBytes += content.size
                    }
                    result.add(
                        com.hanaretamae.kaede.core.repository.SafScanNote(
                            path = read.document.path,
                            modifiedNanos = read.document.modifiedNanos,
                            size = read.document.size,
                            content = content,
                        ),
                    )
                }
                emit(result)
            }
        } finally {
            Log.d(
                LOG_TAG,
                "SAF note-read durationMs=${SystemClock.elapsedRealtime() - startedAt}",
            )
        }
    }

    private suspend fun readNote(document: NoteDocument, byteBudget: Long): NoteRead =
        withContext(Dispatchers.IO) {
            if (
                document.size > MAX_NOTE_BYTES ||
                byteBudget <= 0 ||
                (document.size > 0 && document.size > byteBudget)
            ) {
                return@withContext NoteRead(document, null)
            }
            val limit = minOf(MAX_NOTE_BYTES.toLong(), byteBudget).toInt()
            try {
                val input = resolver.openInputStream(document.uri) ?: return@withContext NoteRead(
                    document,
                    null,
                )
                val content = input.use { it.readBounded(limit) }
                if (content.size > limit ||
                    (document.size > 0 && content.size.toLong() != document.size)
                ) {
                    NoteRead(document, null)
                } else {
                    NoteRead(document, content)
                }
            } catch (_: SecurityException) {
                NoteRead(document, null)
            } catch (_: IOException) {
                NoteRead(document, null)
            }
        }

    private fun InputStream.readBounded(limit: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(limit, 16 * 1024))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer, 0, minOf(buffer.size, limit - output.size() + 1))
            if (read < 0) break
            if (read == 0) continue
            output.write(buffer, 0, read)
            if (output.size() > limit) break
        }
        return output.toByteArray()
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun validateTreeUri(uri: Uri) {
        if (
            uri.scheme != "content" ||
            uri.authority.isNullOrBlank() ||
            "tree" !in uri.pathSegments
        ) {
            throw SafAccessException()
        }
    }

    private fun query(uri: Uri): Cursor = try {
        resolver.query(uri, DOCUMENT_COLUMNS, null, null, null) ?: throw SafAccessException()
    } catch (_: SecurityException) {
        throw SafAccessException()
    } catch (_: IllegalArgumentException) {
        throw SafAccessException()
    } catch (_: IllegalStateException) {
        throw SafAccessException()
    }

    private fun validateSegment(segment: String) {
        if (
            segment.isEmpty() ||
            segment == "." ||
            segment == ".." ||
            segment.contains('/') ||
            segment.contains('\\') ||
            segment.contains('\u0000')
        ) {
            throw SafAccessException()
        }
    }

    private fun validateDocumentId(documentId: String) {
        if (
            documentId.isEmpty() ||
            documentId.toByteArray(Charsets.UTF_8).size > MAX_PATH_BYTES
        ) {
            throw SafAccessException()
        }
    }

    private fun validateRelativePath(path: String): List<String> {
        if (path.toByteArray(Charsets.UTF_8).size > MAX_PATH_BYTES) throw SafAccessException()
        val segments = path.split('/')
        if (segments.size > MAX_DEPTH) throw SafAccessException()
        segments.forEach(::validateSegment)
        return segments
    }

    private data class Directory(
        val documentId: String,
        val relativePath: String,
        val depth: Int,
    )

    private data class NoteDocument(
        val path: String,
        val uri: Uri,
        val modifiedNanos: Long,
        val size: Long,
    )

    private data class NoteRead(
        val document: NoteDocument,
        val content: ByteArray?,
    )

    class SafAccessException : IOException("The selected folder could not be read.")

    private companion object {
        const val LOG_TAG = "KaedeGallerySAF"
        const val MAX_DOCUMENTS = 100_000
        const val MAX_DEPTH = 64
        const val MAX_PATH_BYTES = 4_096
        const val MAX_AGGREGATE_PATH_BYTES = 32L * 1024 * 1024
        const val MAX_NOTE_BYTES = 2 * 1024 * 1024
        const val MAX_BATCH_NOTES = 128
        const val MAX_BATCH_BYTES = 16L * 1024 * 1024
        const val MAX_SCAN_BYTES = 128L * 1024 * 1024
        const val MAX_MEDIA_BYTES = 64 * 1024 * 1024
        const val MAX_IMAGE_DIMENSION = 16_384
        const val MAX_IMAGE_PIXELS = 32L * 1024 * 1024
        const val NANOS_PER_MILLI = 1_000_000L
        val DOCUMENT_COLUMNS = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}
