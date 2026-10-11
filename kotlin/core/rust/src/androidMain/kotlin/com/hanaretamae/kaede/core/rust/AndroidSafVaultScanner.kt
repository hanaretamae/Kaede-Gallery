package com.hanaretamae.kaede.core.rust

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.sqlite.SQLiteException
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import android.provider.DocumentsContract
import android.util.Log
import android.util.LruCache
import android.util.Size
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

const val MAX_GALLERY_THUMBNAIL_DIMENSION = 512

data class AndroidSafScanSnapshot(
    val filePaths: List<String>,
    val batches: Flow<List<com.hanaretamae.kaede.core.repository.SafScanNote>>,
)

class AndroidSafVaultScanner(context: Context) {
    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver
    private val mediaUriCache = AndroidSafMediaUriCache(context)
    private val thumbnailLoadScheduler = ThumbnailLoadScheduler(MAX_CONCURRENT_THUMBNAIL_DECODES)
    private val imageRequests = SingleFlight<String, Bitmap?>()
    private val videoFrameRequests = SingleFlight<String, Bitmap?>()
    private val decodedThumbnails = object : LruCache<String, Bitmap>(MAX_THUMBNAIL_MEMORY_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val decodedViewerImages = object : LruCache<String, Bitmap>(viewerImageMemoryBytes()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    /** Returns an already decoded image without touching storage, or null. */
    fun peekDecodedImage(treeUri: Uri, relativePath: String, maxDimension: Int): Bitmap? =
        imageMemoryCache(maxDimension).get(thumbnailCacheKey("image", treeUri, relativePath, maxDimension))

    private fun imageMemoryCache(maxDimension: Int): LruCache<String, Bitmap> =
        if (maxDimension > MAX_GALLERY_THUMBNAIL_DIMENSION) decodedViewerImages else decodedThumbnails

    suspend fun <T> withThumbnailPermit(
        priority: ThumbnailLoadPriority = ThumbnailLoadPriority.VISIBLE,
        block: suspend () -> T,
    ): T = thumbnailLoadScheduler.withPermit(priority, block)

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
        val documentIdsByPath = LinkedHashMap<String, String>()
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
                    if (documentIdsByPath.put(relativePath, childDocumentId) != null) {
                        throw SafAccessException()
                    }
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
        try {
            mediaUriCache.replace(treeUri.toString(), documentIdsByPath)
            decodedThumbnails.evictAll()
            decodedViewerImages.evictAll()
        } catch (_: SQLiteException) {
            throw SafAccessException()
        }
        AndroidSafScanSnapshot(
            filePaths = filePaths,
            batches = readBatches(notes),
        )
    }

    suspend fun resolveMedia(treeUri: Uri, relativePath: String): Uri =
        withContext(Dispatchers.IO) {
            validateTreeUri(treeUri)
            val segments = validateRelativePath(relativePath)
            val cachedDocumentId = try {
                mediaUriCache.get(treeUri.toString(), relativePath)
            } catch (_: SQLiteException) {
                throw SafAccessException()
            }
            cachedDocumentId?.let { documentId ->
                validateDocumentId(documentId)
                return@withContext DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
            }
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
                    try {
                        mediaUriCache.put(treeUri.toString(), relativePath, nextDocumentId)
                    } catch (_: SQLiteException) {
                        throw SafAccessException()
                    }
                    return@withContext DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        nextDocumentId,
                    )
                }
            }
            throw SafAccessException()
        }

    suspend fun clearMediaUriCache(treeUri: Uri) = withContext(Dispatchers.IO) {
        validateTreeUri(treeUri)
        try {
            mediaUriCache.clear(treeUri.toString())
            decodedThumbnails.evictAll()
        } catch (_: SQLiteException) {
            throw SafAccessException()
        }
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
    ): Bitmap? {
        if (maxDimension !in 1..MAX_IMAGE_DIMENSION) throw SafAccessException()
        val key = thumbnailCacheKey("image", treeUri, relativePath, maxDimension)
        return imageRequests.run(key) {
            decodeImageUncached(treeUri, relativePath, maxDimension)
        }
    }

    private suspend fun decodeImageUncached(
        treeUri: Uri,
        relativePath: String,
        maxDimension: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (maxDimension !in 1..MAX_IMAGE_DIMENSION) throw SafAccessException()
        validateTreeUri(treeUri)
        validateRelativePath(relativePath)
        val cacheThumbnails = maxDimension == MAX_GALLERY_THUMBNAIL_DIMENSION
        val memoryCacheKey = thumbnailCacheKey("image", treeUri, relativePath, maxDimension)
        val memoryCache = imageMemoryCache(maxDimension)
        memoryCache.get(memoryCacheKey)?.let { return@withContext it }
        if (cacheThumbnails) {
            val cachedThumbnail = try {
                mediaUriCache.getThumbnail(treeUri.toString(), relativePath)
            } catch (_: SQLiteException) {
                throw SafAccessException()
            }
            if (cachedThumbnail != null) {
                val cachedBitmap = BitmapFactory.decodeByteArray(
                    cachedThumbnail,
                    0,
                    cachedThumbnail.size,
                )
                if (cachedBitmap != null) {
                    memoryCache.put(memoryCacheKey, cachedBitmap)
                    return@withContext cachedBitmap
                }
                try {
                    mediaUriCache.removeThumbnail(treeUri.toString(), relativePath)
                } catch (_: SQLiteException) {
                    throw SafAccessException()
                }
            }
        }
        val uri = resolveMedia(treeUri, relativePath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val providerThumbnail = try {
                resolver.loadThumbnail(
                    uri,
                    Size(maxDimension, maxDimension),
                    CancellationSignal(),
                )
            } catch (_: IOException) {
                null
            } catch (_: UnsupportedOperationException) {
                null
            }
            val boundedProviderThumbnail = providerThumbnail?.takeIf {
                it.width <= maxDimension &&
                    it.height <= maxDimension &&
                    it.width.toLong() * it.height <= MAX_IMAGE_PIXELS
            }
            if (boundedProviderThumbnail != null) {
                memoryCache.put(memoryCacheKey, boundedProviderThumbnail)
                if (cacheThumbnails) {
                    cacheThumbnail(treeUri, relativePath, boundedProviderThumbnail)
                }
                return@withContext boundedProviderThumbnail
            }
        }
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
        val bitmap = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
        if (cacheThumbnails && bitmap != null) cacheThumbnail(treeUri, relativePath, bitmap)
        bitmap?.let { memoryCache.put(memoryCacheKey, it) }
        bitmap
    }

    suspend fun decodeVideoFrame(
        treeUri: Uri,
        relativePath: String,
        maxDimension: Int,
    ): Bitmap? {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 ||
            maxDimension !in 1..MAX_IMAGE_DIMENSION
        ) {
            return null
        }
        val key = thumbnailCacheKey("video", treeUri, relativePath, maxDimension)
        return videoFrameRequests.run(key) {
            decodeVideoFrameUncached(treeUri, relativePath, maxDimension)
        }
    }

    private suspend fun decodeVideoFrameUncached(
        treeUri: Uri,
        relativePath: String,
        maxDimension: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        validateTreeUri(treeUri)
        validateRelativePath(relativePath)
        val cacheThumbnail = maxDimension == MAX_GALLERY_THUMBNAIL_DIMENSION
        val memoryCacheKey = thumbnailCacheKey("video", treeUri, relativePath, maxDimension)
        decodedThumbnails.get(memoryCacheKey)?.let { return@withContext it }
        if (cacheThumbnail) {
            val cachedThumbnail = try {
                mediaUriCache.getThumbnail(treeUri.toString(), relativePath)
            } catch (_: SQLiteException) {
                throw SafAccessException()
            }
            if (cachedThumbnail != null) {
                val cachedBitmap = BitmapFactory.decodeByteArray(
                    cachedThumbnail,
                    0,
                    cachedThumbnail.size,
                )
                if (cachedBitmap != null) {
                    decodedThumbnails.put(memoryCacheKey, cachedBitmap)
                    return@withContext cachedBitmap
                }
                try {
                    mediaUriCache.removeThumbnail(treeUri.toString(), relativePath)
                } catch (_: SQLiteException) {
                    throw SafAccessException()
                }
            }
        }
        val uri = resolveMedia(treeUri, relativePath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val providerThumbnail = try {
                resolver.loadThumbnail(
                    uri,
                    Size(maxDimension, maxDimension),
                    CancellationSignal(),
                )
            } catch (_: IOException) {
                null
            } catch (_: UnsupportedOperationException) {
                null
            }
            val boundedProviderThumbnail = providerThumbnail?.takeIf {
                it.width <= maxDimension &&
                    it.height <= maxDimension &&
                    it.width.toLong() * it.height <= MAX_IMAGE_PIXELS
            }
            if (boundedProviderThumbnail != null) {
                decodedThumbnails.put(memoryCacheKey, boundedProviderThumbnail)
                if (cacheThumbnail) {
                    cacheThumbnail(treeUri, relativePath, boundedProviderThumbnail)
                }
                return@withContext boundedProviderThumbnail
            }
        }
        val descriptor = try {
            resolver.openAssetFileDescriptor(uri, "r")
        } catch (_: SecurityException) {
            throw SafAccessException()
        } catch (_: IOException) {
            null
        }
        val bitmap = descriptor?.use { file ->
            decodeVideoFrame(maxDimension) { retriever ->
                if (file.length != AssetFileDescriptor.UNKNOWN_LENGTH) {
                    retriever.setDataSource(file.fileDescriptor, file.startOffset, file.length)
                } else if (file.startOffset == 0L) {
                    retriever.setDataSource(file.fileDescriptor)
                } else {
                    retriever.setDataSource(appContext, uri)
                }
            }
        } ?: decodeVideoFrame(maxDimension) { retriever ->
            retriever.setDataSource(appContext, uri)
        }
        if (cacheThumbnail && bitmap != null) cacheThumbnail(treeUri, relativePath, bitmap)
        bitmap?.let { decodedThumbnails.put(memoryCacheKey, it) }
        bitmap
    }

    private fun decodeVideoFrame(
        maxDimension: Int,
        setDataSource: (MediaMetadataRetriever) -> Unit,
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            setDataSource(retriever)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION,
            )?.toLongOrNull()?.coerceIn(0L, MAX_VIDEO_DURATION_MILLIS)
            val timestamps = buildList {
                add(0L)
                add(1_000_000L)
                add(-1L)
                durationMs?.let { duration ->
                    add(duration * 100L)
                    add(duration * 500L)
                }
            }.distinct()
            timestamps.firstNotNullOfOrNull { timestamp ->
                listOf(
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                ).firstNotNullOfOrNull { option ->
                    try {
                        retriever.getScaledFrameAtTime(
                            timestamp,
                            option,
                            maxDimension,
                            maxDimension,
                        )
                    } catch (_: IllegalArgumentException) {
                        null
                    } catch (_: IllegalStateException) {
                        null
                    } catch (_: UnsupportedOperationException) {
                        null
                    }
                }
            }
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IllegalStateException) {
            null
        } catch (_: UnsupportedOperationException) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun cacheThumbnail(treeUri: Uri, relativePath: String, bitmap: Bitmap) {
        val encoded = ByteArrayOutputStream().use { output ->
            val format = if (bitmap.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val quality = if (format == Bitmap.CompressFormat.PNG) 100 else 88
            if (bitmap.compress(format, quality, output)) output.toByteArray() else null
        }
        if (encoded != null) {
            try {
                mediaUriCache.putThumbnail(treeUri.toString(), relativePath, encoded)
            } catch (_: SQLiteException) {
                throw SafAccessException()
            }
        }
    }

    private fun thumbnailCacheKey(
        kind: String,
        treeUri: Uri,
        relativePath: String,
        maxDimension: Int,
    ): String = "$kind:${treeUri}:$relativePath:$maxDimension"

    private fun readBatches(
        documents: List<NoteDocument>,
    ): Flow<List<com.hanaretamae.kaede.core.repository.SafScanNote>> = flow {
        val startedAt = SystemClock.elapsedRealtime()
        var scannedContentBytes = 0L
        try {
            for (batch in documents.chunked(MAX_SCAN_BATCH_NOTES)) {
                val result = ArrayList<com.hanaretamae.kaede.core.repository.SafScanNote>(
                    batch.size,
                )
                var reservedBatchBytes = 0L
                val reads = coroutineScope {
                    val readLimit = Semaphore(AndroidSafLimits.MAX_CONCURRENT_NOTE_READS)
                    batch.map { document ->
                        val remaining = minOf(
                            MAX_BATCH_BYTES - reservedBatchBytes,
                            AndroidSafLimits.MAX_SCAN_NOTE_BYTES - scannedContentBytes,
                        ).coerceAtLeast(0)
                        val allocation = minOf(
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
                            size = content?.size?.toLong() ?: read.document.size,
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
            if (byteBudget <= 0) {
                return@withContext NoteRead(document, null)
            }
            val limit = minOf(MAX_NOTE_BYTES.toLong(), byteBudget).toInt()
            try {
                val input = resolver.openInputStream(document.uri) ?: return@withContext NoteRead(
                    document,
                    null,
                )
                val content = input.use { it.readBounded(limit) }
                if (content.size > limit) {
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
        const val MAX_BATCH_BYTES = 16L * 1024 * 1024
        // Reserve the full per-note bound while keeping each batch within its byte limit.
        const val MAX_SCAN_BATCH_NOTES = 8
        const val MAX_MEDIA_BYTES = 64 * 1024 * 1024
        const val MAX_VIDEO_DURATION_MILLIS = 24L * 60 * 60 * 1_000
        const val MAX_CONCURRENT_THUMBNAIL_DECODES = 4
        private const val MAX_THUMBNAIL_MEMORY_BYTES = 32 * 1024 * 1024
        private const val MAX_VIEWER_IMAGE_MEMORY_BYTES = 96 * 1024 * 1024
        private const val MIN_VIEWER_IMAGE_MEMORY_BYTES = 24 * 1024 * 1024

        // Room for the current viewer image plus a few prefetched neighbours, bounded by heap size.
        private fun viewerImageMemoryBytes(): Int =
            (Runtime.getRuntime().maxMemory() / 6)
                .coerceIn(MIN_VIEWER_IMAGE_MEMORY_BYTES.toLong(), MAX_VIEWER_IMAGE_MEMORY_BYTES.toLong())
                .toInt()
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
