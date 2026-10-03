package com.hanaretamae.vault_gallery

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class MainActivity : FlutterActivity() {
    private data class PendingExport(
        val result: MethodChannel.Result,
        val content: ByteArray,
    )

    private val channelName = "com.hanaretamae.vault_gallery/saf"
    private val pickerRequestCode = 7142
    private val exportRequestCode = 7143
    private val listedDocuments = ConcurrentHashMap<String, ResolvedDocument>()
    private val listedDocumentsLock = Any()
    private val safDatabaseLock = Any()
    private val openVideoDescriptors =
        ConcurrentHashMap<Int, android.os.ParcelFileDescriptor>()
    private var loadedDocumentTree: String? = null
    private val safExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "saf-io").apply { isDaemon = true }
    }
    private val safReadExecutor =
        Executors.newFixedThreadPool(SafLimits.MAX_CONCURRENT_NOTE_READS) { task ->
        Thread(task, "saf-note-read").apply { isDaemon = true }
    }
    private val thumbnailExecutor = Executors.newFixedThreadPool(3) { task ->
        Thread(task, "saf-thumbnail").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingPicker: MethodChannel.Result? = null
    private var pendingExport: PendingExport? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "chooseVault" -> chooseVault(result)
                    "saveJson" -> chooseJsonExport(call, result)
                    "loadVault", "listFiles", "readFile", "readListedFiles", "resolveFile",
                    "readMedia", "readMediaImage", "openMedia", "openMediaFd",
                    "closeMediaFd", "thumbnail", "forgetVault" ->
                        runSafCall(call, result)
                    else -> result.notImplemented()
                }
            }
    }

    override fun onDestroy() {
        openVideoDescriptors.values.forEach { descriptor ->
            try {
                descriptor.close()
            } catch (_: java.io.IOException) {
                // The descriptor is released with the activity regardless.
            }
        }
        openVideoDescriptors.clear()
        super.onDestroy()
    }

    private fun openMediaFileDescriptor(tree: Uri, mediaUri: Uri): Int? {
        val safeUri = checkedMediaUri(tree, mediaUri)
        val descriptor = contentResolver.openFileDescriptor(safeUri, "r") ?: return null
        val fd = descriptor.fd
        val previous = openVideoDescriptors.putIfAbsent(fd, descriptor)
        if (previous != null) {
            descriptor.close()
            error("Unable to reserve a media descriptor")
        }
        return fd
    }

    private fun chooseJsonExport(call: MethodCall, result: MethodChannel.Result) {
        if (pendingExport != null || pendingPicker != null) {
            result.error("SAF_BUSY", "Another document picker is already open.", null)
            return
        }
        val fileName = call.argument<String>("fileName")
        val content = call.argument<ByteArray>("content")
        if (fileName.isNullOrBlank() || fileName.contains('/') || content == null) {
            result.error("INVALID_ARGUMENT", "The export request is invalid.", null)
            return
        }
        pendingExport = PendingExport(result, content)
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        try {
            startActivityForResult(intent, exportRequestCode)
        } catch (_: Exception) {
            pendingExport = null
            result.error("SAF_IO", "The document picker could not be opened.", null)
        }
    }

    private fun runSafCall(call: MethodCall, result: MethodChannel.Result) {
        if (call.method == "openMedia") {
            launchMedia(call, result)
            return
        }
        val executor = if (call.method == "thumbnail") thumbnailExecutor else safExecutor
        executor.execute {
            try {
                val value = when (call.method) {
                    "loadVault" -> loadVault()
                    "forgetVault" -> {
                        forgetVault(
                            call.argument<String>("vaultUri") ?: error("Invalid request"),
                        )
                        null
                    }
                    "listFiles" -> listFiles(
                        requiredVault(call.argument("vaultUri"), loadDocumentCache = false),
                    )
                    "readFile" -> {
                        val vault = requiredVault(call.argument("vaultUri"))
                        val relative = call.argument<String>("path") ?: error("Invalid request")
                        readDocument(vault, relative, SafLimits.MAX_NOTE_BYTES)
                    }
                    "readListedFiles" -> {
                        val vault = requiredVault(call.argument("vaultUri"))
                        readListedFiles(
                            vault,
                            call.argument<List<Map<String, Any?>>>("documents")
                                ?: error("Invalid request"),
                        )
                    }
                    "resolveFile" -> {
                        val vault = requiredVault(call.argument("vaultUri"))
                        val relative = call.argument<String>("path") ?: error("Invalid request")
                        val video = call.argument<Boolean>("video") ?: false
                        checkedSegments(relative)
                        val document = cachedOrResolveDocument(vault, relative)
                        if (video && document.entry.size > 64L * 1024 * 1024) {
                            null
                        } else {
                            document.uri.toString()
                        }
                    }
                    "readMedia" -> {
                        val vault = requiredVault(call.argument("vaultUri"))
                        val mediaUri = call.argument<String>("mediaUri")
                            ?.let(Uri::parse) ?: error("Invalid request")
                        readResolvedMedia(vault, mediaUri)
                    }
                    "readMediaImage" -> {
                        val vault = requiredVault(call.argument("vaultUri"))
                        val mediaUri = call.argument<String>("mediaUri")
                            ?.let(Uri::parse) ?: error("Invalid request")
                        val maxDimension = call.argument<Int>("maxDimension") ?: 2048
                        readResolvedMediaImage(vault, mediaUri, maxDimension)
                    }
                    "openMediaFd" -> {
                        val vault = requiredVault(call.argument("vaultUri"))
                        val mediaUri = call.argument<String>("mediaUri")
                            ?.let(Uri::parse) ?: error("Invalid request")
                        openMediaFileDescriptor(vault, mediaUri)
                    }
                    "closeMediaFd" -> {
                        val descriptor = call.argument<Int>("descriptor")
                            ?: error("Invalid request")
                        openVideoDescriptors.remove(descriptor)?.close()
                        null
                    }
                    "thumbnail" -> {
                        val vault = requiredVault(call.argument("vaultUri"))
                        val relative = call.argument<String>("path") ?: error("Invalid request")
                        val size = call.argument<Int>("size") ?: 320
                        val video = call.argument<Boolean>("video") ?: false
                        createThumbnail(vault, relative, size, video)
                    }
                    else -> error("Unsupported request")
                }
                mainHandler.post { result.success(value) }
            } catch (_: SecurityException) {
                mainHandler.post {
                    result.error(
                        "SAF_PERMISSION",
                        "The selected folder is no longer accessible. Select it again.",
                        null,
                    )
                }
            } catch (_: Exception) {
                mainHandler.post {
                    result.error("SAF_IO", "The selected folder could not be read.", null)
                }
            }

        }
    }

    private fun launchMedia(call: MethodCall, result: MethodChannel.Result) {
        safExecutor.execute {
            try {
                val vault = requiredVault(call.argument("vaultUri"))
                val mediaUri = call.argument<String>("mediaUri")?.let(Uri::parse)
                val openFolder = call.argument<Boolean>("openFolder") ?: false
                val intent = createMediaOpenIntent(vault, mediaUri, openFolder)
                mainHandler.post {
                    try {
                        startActivity(
                            if (openFolder) {
                                Intent.createChooser(intent, "フォルダーを開く")
                            } else {
                                intent
                            },
                        )
                        result.success(true)
                    } catch (_: UnsupportedOperationException) {
                        result.error(
                            "SAF_UNSUPPORTED",
                            if (openFolder) {
                                "The selected app cannot open this folder."
                            } else {
                                "The selected app cannot open this media file."
                            },
                            null,
                        )
                    } catch (_: Exception) {
                        result.error(
                            "SAF_OPEN_FAILED",
                            if (openFolder) {
                                "The selected folder could not be opened."
                            } else {
                                "The selected media file could not be opened."
                            },
                            null,
                        )
                    }
                }
            } catch (_: SecurityException) {
                mainHandler.post {
                    result.error(
                        "SAF_PERMISSION",
                        "The selected folder is no longer accessible. Select it again.",
                        null,
                    )
                }
            } catch (_: Exception) {
                mainHandler.post {
                    result.error(
                        "SAF_OPEN_FAILED",
                        "The selected media could not be opened.",
                        null,
                    )
                }
            }
        }
    }

    private fun chooseVault(result: MethodChannel.Result) {
        if (pendingPicker != null) {
            result.error("SAF_BUSY", "A folder picker is already open.", null)
            return
        }
        pendingPicker = result
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments"),
                )
            }
        }
        startActivityForResult(intent, pickerRequestCode)
    }

    @Deprecated("Uses the Storage Access Framework activity result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == exportRequestCode) {
            val export = pendingExport ?: return
            pendingExport = null
            val uri = data?.data
            if (resultCode != Activity.RESULT_OK || uri == null) {
                export.result.success(false)
                return
            }
            if (isInsideSelectedVault(uri)) {
                export.result.error(
                    "VAULT_READ_ONLY",
                    "Settings cannot be saved inside the selected Vault.",
                    null,
                )
                return
            }
            safExecutor.execute {
                try {
                    contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(export.content)
                    } ?: error("Unable to open the selected destination")
                    mainHandler.post { export.result.success(true) }
                } catch (_: Exception) {
                    mainHandler.post {
                        export.result.error(
                            "SAF_IO",
                            "The selected export destination could not be written.",
                            null,
                        )
                    }
                }
            }
            return
        }
        if (requestCode != pickerRequestCode) return
        val reply = pendingPicker ?: return
        pendingPicker = null
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            reply.success(null)
            return
        }
        val uri = data.data!!
        try {
            val flags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            require(flags != 0)
            contentResolver.takePersistableUriPermission(uri, flags)
            val preferences = getSharedPreferences("vault_gallery", MODE_PRIVATE)
            val previous = preferences.getString("vault_uri", null)?.let(Uri::parse)
            if (previous != null && previous != uri) {
                try {
                    contentResolver.releasePersistableUriPermission(
                        previous,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (error: Exception) {
                    contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    throw error
                }
            }
            if (!preferences.edit().putString("vault_uri", uri.toString()).commit()) {
                contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                error("Unable to retain selected folder")
            }
            reply.success(uri.toString())
        } catch (_: Exception) {
            reply.error("SAF_PERMISSION", "The folder access permission could not be retained.", null)
        }
    }

    private fun loadVault(): String? {
        val uri = getSharedPreferences("vault_gallery", MODE_PRIVATE)
            .getString("vault_uri", null) ?: return null
        val parsed = Uri.parse(uri)
        val hasPermission = contentResolver.persistedUriPermissions.any {
            it.uri == parsed && it.isReadPermission
        }
        if (!hasPermission) {
            getSharedPreferences("vault_gallery", MODE_PRIVATE)
                .edit()
                .remove("vault_uri")
                .apply()
            return null
        }
        return uri
    }

    private fun forgetVault(vaultValue: String) {
        val tree = Uri.parse(vaultValue)
        require(tree.scheme == "content" && tree.toString() == vaultValue)
        val preferences = getSharedPreferences("vault_gallery", MODE_PRIVATE)
        val selectedVault = preferences.getString("vault_uri", null)
        require(selectedVault == null || selectedVault == vaultValue)

        synchronized(safDatabaseLock) {
            val database = documentDatabase()
            try {
                database.execSQL("PRAGMA secure_delete=ON")
                database.beginTransaction()
                try {
                    database.delete("documents", "tree_uri=?", arrayOf(vaultValue))
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
                database.execSQL("VACUUM")
            } finally {
                database.close()
            }
        }

        synchronized(listedDocumentsLock) {
            listedDocuments.clear()
            loadedDocumentTree = null
        }
        var descriptorFailure: java.io.IOException? = null
        for ((fd, descriptor) in openVideoDescriptors.entries) {
            if (openVideoDescriptors.remove(fd, descriptor)) {
                try {
                    descriptor.close()
                } catch (error: java.io.IOException) {
                    if (descriptorFailure == null) descriptorFailure = error
                }
            }
        }
        descriptorFailure?.let { throw it }

        if (contentResolver.persistedUriPermissions.any {
                it.uri == tree && it.isReadPermission
            }) {
            contentResolver.releasePersistableUriPermission(
                tree,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        if (contentResolver.persistedUriPermissions.any {
                it.uri == tree && it.isReadPermission
            }) {
            error("Unable to release folder permission")
        }
        if (!preferences.edit().remove("vault_uri").commit()) {
            error("Unable to clear selected folder")
        }
        synchronized(listedDocumentsLock) {
            listedDocuments.clear()
            loadedDocumentTree = null
        }
    }

    private fun requiredVault(
        value: String?,
        loadDocumentCache: Boolean = true,
    ): Uri {
        val uri = value?.let(Uri::parse) ?: error("No selected folder")
        require(uri.scheme == "content")
        if (contentResolver.persistedUriPermissions.none {
                it.uri == uri && it.isReadPermission
            }) {
            throw SecurityException("Folder permission is no longer available")
        }
        if (loadDocumentCache) loadListedDocuments(uri)
        return uri
    }

    private fun documentDatabase(): SQLiteDatabase =
        SQLiteDatabase.openOrCreateDatabase(File(filesDir, "saf-documents.sqlite"), null)
            .apply {
                execSQL(
                    "CREATE TABLE IF NOT EXISTS documents (" +
                        "tree_uri TEXT NOT NULL, path TEXT NOT NULL, document_uri TEXT NOT NULL, " +
                        "document_id TEXT NOT NULL, name TEXT NOT NULL, mime_type TEXT NOT NULL, " +
                        "modified INTEGER NOT NULL, size INTEGER NOT NULL, " +
                        "PRIMARY KEY(tree_uri, path))",
                )
            }

    private fun loadListedDocuments(tree: Uri) {
        synchronized(safDatabaseLock) {
            loadListedDocumentsSerialized(tree)
        }
    }

    private fun loadListedDocumentsSerialized(tree: Uri) {
        val treeValue = tree.toString()
        synchronized(listedDocumentsLock) {
            if (loadedDocumentTree == treeValue) return
            val database = documentDatabase()
            try {
                listedDocuments.clear()
                database.query(
                    "documents",
                    arrayOf("path", "document_uri", "document_id", "name", "mime_type", "modified", "size"),
                    "tree_uri=?",
                    arrayOf(treeValue),
                    null,
                    null,
                    null,
                    SafLimits.MAX_DOCUMENTS.toString(),
                ).use { rows ->
                    while (rows.moveToNext()) {
                        val path = rows.getString(0)
                        val uri = Uri.parse(rows.getString(1))
                        val documentId = rows.getString(2)
                        val name = rows.getString(3)
                        val mimeType = rows.getString(4)
                        val modified = rows.getLong(5)
                        val size = rows.getLong(6)
                        if (checkedSegments(path).isEmpty() ||
                            name.isEmpty() || name == "." || name == ".." ||
                            name.contains('/') || name.contains('\\') ||
                            name.contains('\u0000') || modified < 0 || size < 0 ||
                            DocumentsContract.getDocumentId(checkedMediaUri(tree, uri)) != documentId
                        ) {
                            error("Invalid cached document entry")
                        }
                        listedDocuments[path] = ResolvedDocument(
                            uri,
                            Entry(documentId, name, mimeType, modified, size),
                        )
                    }
                }
                loadedDocumentTree = treeValue
            } catch (error: IllegalArgumentException) {
                listedDocuments.clear()
                database.delete("documents", "tree_uri=?", arrayOf(treeValue))
                loadedDocumentTree = treeValue
            } finally {
                database.close()
            }
        }
    }

    private data class Entry(
        val id: String,
        val name: String,
        val mimeType: String,
        val modified: Long,
        val size: Long,
    )

    private data class ResolvedDocument(val uri: Uri, val entry: Entry)

    private fun children(tree: Uri, parentId: String): List<Entry> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        val cursor = contentResolver.query(uri, projection, null, null, null)
            ?: error("Unable to list folder")
        return cursor.use { rows ->
            val result = ArrayList<Entry>()
            while (rows.moveToNext()) {
                require(result.size < SafLimits.MAX_DOCUMENTS) {
                    "Folder contains too many entries"
                }
                result.add(rows.toEntry())
            }
            result
        }
    }

    private fun Cursor.toEntry(): Entry {
        fun value(column: String): String? {
            val index = getColumnIndex(column)
            return if (index < 0 || isNull(index)) null else getString(index)
        }
        fun longValue(column: String): Long {
            val index = getColumnIndex(column)
            return if (index < 0 || isNull(index)) 0L else getLong(index)
        }
        return Entry(
            value(DocumentsContract.Document.COLUMN_DOCUMENT_ID) ?: error("Invalid folder entry"),
            value(DocumentsContract.Document.COLUMN_DISPLAY_NAME) ?: error("Invalid folder entry"),
            value(DocumentsContract.Document.COLUMN_MIME_TYPE) ?: "application/octet-stream",
            longValue(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            longValue(DocumentsContract.Document.COLUMN_SIZE),
        )
    }

    private fun listFiles(tree: Uri): List<Map<String, Any>> =
        synchronized(safDatabaseLock) {
            listFilesSerialized(tree)
        }

    private fun listFilesSerialized(tree: Uri): List<Map<String, Any>> {
        synchronized(listedDocumentsLock) {
            listedDocuments.clear()
            loadedDocumentTree = null
        }
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val visitedFolders = HashSet<String>()
        val paths = HashSet<String>()
        val output = ArrayList<Map<String, Any>>()
        var pathBytes = 0
        var entryCount = 0
        fun walk(parentId: String, parentPath: String, depth: Int) {
            require(depth <= SafLimits.MAX_DEPTH) { "Folder nesting limit exceeded" }
            if (!visitedFolders.add(parentId)) error("Folder cycle detected")
            for (entry in children(tree, parentId)) {
                entryCount++
                require(entryCount <= SafLimits.MAX_DOCUMENTS) {
                    "Folder entry limit exceeded"
                }
                if (entry.name.isEmpty() || entry.name == "." || entry.name == ".." ||
                    entry.name.contains('/') || entry.name.contains('\\') ||
                    entry.name.contains('\u0000')
                ) error("Invalid folder entry")
                val relative = if (parentPath.isEmpty()) entry.name else "$parentPath/${entry.name}"
                val relativeBytes = relative.toByteArray(Charsets.UTF_8).size
                require(relativeBytes <= SafLimits.MAX_RELATIVE_PATH_BYTES) {
                    "Path is too long"
                }
                pathBytes += relativeBytes
                require(pathBytes <= SafLimits.MAX_AGGREGATE_PATH_BYTES) {
                    "Folder listing is too large"
                }
                if (!paths.add(relative)) error("Ambiguous folder path")
                if (ignoredName(entry.name)) continue
                if (entry.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walk(entry.id, relative, depth + 1)
                } else {
                    val document = ResolvedDocument(
                        DocumentsContract.buildDocumentUriUsingTree(tree, entry.id),
                        entry,
                    )
                    listedDocuments[relative] = document
                    output.add(
                        mapOf(
                            "path" to relative,
                            "modifiedNanos" to entry.modified.coerceAtLeast(0L)
                                .coerceAtMost(Long.MAX_VALUE / 1_000_000L) * 1_000_000L,
                            "size" to entry.size.coerceAtLeast(0L),
                            "documentUri" to document.uri.toString(),
                        ),
                    )
                }
            }
        }
        walk(rootId, "", 0)
        val database = documentDatabase()
        try {
            database.beginTransaction()
            database.delete("documents", "tree_uri=?", arrayOf(tree.toString()))
            for ((path, document) in listedDocuments) {
                val values = ContentValues().apply {
                    put("tree_uri", tree.toString())
                    put("path", path)
                    put("document_uri", document.uri.toString())
                    put("document_id", document.entry.id)
                    put("name", document.entry.name)
                    put("mime_type", document.entry.mimeType)
                    put("modified", document.entry.modified)
                    put("size", document.entry.size)
                }
                database.insertOrThrow("documents", null, values)
            }
            database.setTransactionSuccessful()
            synchronized(listedDocumentsLock) {
                loadedDocumentTree = tree.toString()
            }
        } finally {
            database.endTransaction()
            database.close()
        }
        return output
    }

    private fun ignoredName(name: String): Boolean =
        name.startsWith(".") ||
            name.contains(".sync-conflict-") ||
            (name.startsWith(".syncthing.") && name.endsWith(".tmp")) ||
            name.startsWith("~syncthing~")

    private fun checkedSegments(path: String): List<String> {
        require(
            path.isNotEmpty() &&
                path.toByteArray(Charsets.UTF_8).size <= SafLimits.MAX_RELATIVE_PATH_BYTES &&
                !path.startsWith('/') &&
                !path.contains('\\') &&
                !path.contains('\u0000'),
        )
        val segments = path.split('/')
        require(segments.size - 1 <= SafLimits.MAX_DEPTH)
        require(segments.all { it.isNotEmpty() && it != "." && it != ".." })
        return segments
    }

    private fun resolveDocument(tree: Uri, relativePath: String): ResolvedDocument {
        var parentId = DocumentsContract.getTreeDocumentId(tree)
        var result: Uri? = null
        var lastEntry: Entry? = null
        val segments = checkedSegments(relativePath)
        for ((index, segment) in segments.withIndex()) {
            val matches = children(tree, parentId).filter { it.name == segment }
            require(matches.size == 1) { "Document is missing or ambiguous" }
            val entry = matches.single()
            lastEntry = entry
            result = DocumentsContract.buildDocumentUriUsingTree(tree, entry.id)
            if (index < segments.lastIndex) {
                require(entry.mimeType == DocumentsContract.Document.MIME_TYPE_DIR)
                parentId = entry.id
            }
        }
        val uri = result ?: error("Document not found")
        return ResolvedDocument(uri, lastEntry ?: error("Document not found")).also {
            listedDocuments[relativePath] = it
        }
    }

    private fun cachedOrResolveDocument(tree: Uri, relativePath: String): ResolvedDocument {
        val cached = listedDocuments[relativePath]
        if (cached != null) {
            try {
                return ResolvedDocument(checkedMediaUri(tree, cached.uri), cached.entry)
            } catch (_: IllegalArgumentException) {
                listedDocuments.remove(relativePath, cached)
            }
        }
        return resolveDocument(tree, relativePath)
    }

    private fun readDocument(tree: Uri, relativePath: String, maxBytes: Int): ByteArray {
        checkedSegments(relativePath)
        val uri = cachedOrResolveDocument(tree, relativePath).uri
        return contentResolver.openInputStream(uri)?.use { input ->
            readBounded(input, maxBytes)
        } ?: error("Unable to open document")
    }

    private fun readResolvedMedia(tree: Uri, mediaUri: Uri): ByteArray {
        val checkedUri = checkedMediaUri(tree, mediaUri)
        return contentResolver.openInputStream(checkedUri)?.use {
            readBounded(it, 64 * 1024 * 1024)
        } ?: error("Unable to open media")
    }

    private fun readResolvedMediaImage(
        tree: Uri,
        mediaUri: Uri,
        maxDimension: Int,
    ): ByteArray {
        require(maxDimension in 320..3072)
        val checkedUri = checkedMediaUri(tree, mediaUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsInput = contentResolver.openInputStream(checkedUri)
            ?: error("Unable to open image")
        boundsInput.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth in 1..65536 && bounds.outHeight in 1..65536)
        require(bounds.outWidth.toLong() * bounds.outHeight <= 268_435_456L)
        var sample = 1
        while (bounds.outWidth / sample > maxDimension * 2 ||
            bounds.outHeight / sample > maxDimension * 2
        ) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = contentResolver.openInputStream(checkedUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Unable to decode image")
        val scale = minOf(
            maxDimension.toFloat() / decoded.width,
            maxDimension.toFloat() / decoded.height,
            1f,
        )
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            ).also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }
        return ByteArrayOutputStream().use { output ->
            val format = if (bitmap.hasAlpha()) {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }
            val compressed = bitmap.compress(format, 88, output)
            bitmap.recycle()
            require(compressed) { "Unable to encode image" }
            require(output.size() <= 32 * 1024 * 1024) { "Encoded image exceeds size limit" }
            output.toByteArray()
        }
    }

    private fun checkedMediaUri(tree: Uri, mediaUri: Uri): Uri {
        require(mediaUri.scheme == "content" && mediaUri.authority == tree.authority)
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val documentId = DocumentsContract.getDocumentId(mediaUri)
        require(documentId.startsWith("$rootId/"))
        return DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
    }

    private fun readListedFiles(
        tree: Uri,
        documents: List<Map<String, Any?>>,
    ): List<Map<String, Any?>> {
        require(documents.size <= SafLimits.MAX_READ_BATCH_NOTES) {
            "Too many notes in one read batch"
        }
        val requests = documents.map { document ->
            val path = document["path"] as? String ?: error("Invalid note path")
            checkedSegments(path)
            val uri = (document["documentUri"] as? String)
                ?.let(Uri::parse) ?: error("Invalid document URI")
            val safeUri = checkedMediaUri(tree, uri)
            val declaredSize = (document["size"] as? Number)
                ?.toLong() ?: error("Invalid note size")
            require(declaredSize >= 0)
            Triple(path, safeUri, declaredSize)
        }
        var remainingBytes = SafLimits.MAX_READ_BATCH_BYTES
        val allowed = requests.map { request ->
            val requestedBytes = when {
                request.third > SafLimits.MAX_NOTE_BYTES.toLong() -> 0
                request.third == 0L -> SafLimits.MAX_NOTE_BYTES
                else -> minOf(SafLimits.MAX_NOTE_BYTES.toLong(), request.third + 1).toInt()
            }
            val limit = requestedBytes.takeIf {
                it <= remainingBytes
            } ?: 0
            remainingBytes -= limit
            request to limit
        }
        val results = safReadExecutor.invokeAll(
            allowed.map { (request, limit) ->
                java.util.concurrent.Callable {
                    val (path, uri, declaredSize) = request
                    val content = if (limit == 0) {
                        null
                    } else {
                        try {
                            contentResolver.openInputStream(uri)?.use {
                                readBounded(it, limit)
                            } ?: error("Unable to open document")
                        } catch (error: SecurityException) {
                            throw error
                        } catch (_: java.io.IOException) {
                            null
                        } catch (_: IllegalArgumentException) {
                            null
                        }
                    }
                    mapOf(
                        "path" to path,
                        "content" to content,
                        "size" to declaredSize,
                    )
                }
            },
        )
        return results.map { future ->
            try {
                future.get()
            } catch (error: java.util.concurrent.ExecutionException) {
                when (val cause = error.cause) {
                    is SecurityException -> throw cause
                    is Exception -> throw cause
                    else -> throw error
                }
            }
        }
    }

    private fun createMediaOpenIntent(
        tree: Uri,
        mediaUri: Uri?,
        openFolder: Boolean,
    ): Intent {
        val target: Uri
        val mimeType: String
        if (openFolder) {
            val media = checkedMediaUri(tree, mediaUri ?: error("Invalid media URI"))
            val documentId = DocumentsContract.getDocumentId(media)
            val separator = documentId.lastIndexOf('/')
            val folderId = if (separator < 0) documentId else documentId.substring(0, separator)
            target = DocumentsContract.buildDocumentUriUsingTree(tree, folderId)
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            target = checkedMediaUri(tree, mediaUri ?: error("Invalid media URI"))
            mimeType = contentResolver.getType(target) ?: "*/*"
        }
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(target, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (openFolder) {
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            }
        }
    }

    private fun isInsideSelectedVault(destination: Uri): Boolean {
        val vault = getSharedPreferences("vault_gallery", MODE_PRIVATE)
            .getString("vault_uri", null)
            ?.let(Uri::parse) ?: return false
        if (destination.authority != vault.authority) return false
        val rootId = DocumentsContract.getTreeDocumentId(vault)
        val documentId = try {
            DocumentsContract.getDocumentId(destination)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return documentId == rootId || documentId.startsWith("$rootId/")
    }

    private fun readBounded(input: InputStream, maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "Document size limit exceeded" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun createThumbnail(
        tree: Uri,
        relativePath: String,
        requestedSize: Int,
        video: Boolean,
    ): ByteArray? {
        require(requestedSize in 1..1024)
        val document = cachedOrResolveDocument(tree, relativePath)
        if (!video && document.entry.size > 64L * 1024 * 1024) return null
        val uri = document.uri
        val bitmap = if (video) {
            val frame = extractVideoFrame(uri, requestedSize, useFileDescriptor = false)
                ?: extractVideoFrame(uri, requestedSize, useFileDescriptor = true)
            frame ?: if (android.os.Build.VERSION.SDK_INT >= 29) {
                try {
                    contentResolver.loadThumbnail(
                        uri,
                        android.util.Size(requestedSize, requestedSize),
                        null,
                    )
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        } else {
            val input = contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            input.use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth <= 0 || options.outHeight <= 0 ||
                options.outWidth > 16384 || options.outHeight > 16384 ||
                options.outWidth.toLong() * options.outHeight > 32L * 1024 * 1024
            ) return null
            var sample = 1
            while (options.outWidth / sample > requestedSize * 2 ||
                options.outHeight / sample > requestedSize * 2
            ) sample *= 2
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } ?: return null
        val scale = minOf(
            requestedSize.toFloat() / bitmap.width,
            requestedSize.toFloat() / bitmap.height,
            1f,
        )
        val outputBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true,
            ).also { if (it !== bitmap) bitmap.recycle() }
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { output ->
            if (!outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                outputBitmap.recycle()
                return null
            }
            outputBitmap.recycle()
            output.toByteArray()
        }
    }

    private fun extractVideoFrame(
        uri: Uri,
        requestedSize: Int,
        useFileDescriptor: Boolean,
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        var descriptor: android.os.ParcelFileDescriptor? = null
        return try {
            if (useFileDescriptor) {
                descriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
                retriever.setDataSource(descriptor.fileDescriptor)
            } else {
                retriever.setDataSource(this, uri)
            }
            val durationUs = runCatching {
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION,
                )?.toLongOrNull()
            }.getOrNull()
                ?.coerceIn(0, 24 * 60 * 60 * 1000L)
                ?.times(1000)
            val timestamps = listOfNotNull(
                -1L,
                durationUs?.div(10),
                durationUs?.div(4),
                1_000_000L,
                0L,
                durationUs?.div(2),
                durationUs?.times(3)?.div(4),
            ).distinct()
            for (timestamp in timestamps) {
                val frame = runCatching {
                    if (android.os.Build.VERSION.SDK_INT >= 27) {
                        retriever.getScaledFrameAtTime(
                            timestamp,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            requestedSize,
                            requestedSize,
                        )
                    } else {
                        null
                    }
                }.getOrNull()
                if (frame != null) return frame
            }
            val width = runCatching {
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH,
                )?.toLongOrNull()
            }.getOrNull() ?: 0
            val height = runCatching {
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT,
                )?.toLongOrNull()
            }.getOrNull() ?: 0
            if (width !in 1..16384 ||
                height !in 1..16384 ||
                width * height > 16L * 1024 * 1024
            ) {
                return null
            }
            for (timestamp in timestamps) {
                val frame = runCatching {
                    retriever.getFrameAtTime(
                        timestamp,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    )
                }.getOrNull()
                if (frame != null) return frame
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // The frame remains usable if cleanup fails.
            }
            try {
                descriptor?.close()
            } catch (_: Exception) {
                // Descriptor cleanup must not discard an extracted frame.
            }
        }
    }
}
