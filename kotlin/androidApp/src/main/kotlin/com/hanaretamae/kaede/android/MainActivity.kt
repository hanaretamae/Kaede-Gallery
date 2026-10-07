package com.hanaretamae.kaede.android

import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.provider.DocumentsContract
import android.net.Uri
import android.os.Bundle
import android.system.Os
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.hanaretamae.kaede.core.model.GalleryCategory
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.GalleryPage
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.model.MediaSummary
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.model.NoteSummary
import com.hanaretamae.kaede.core.repository.GallerySessionHandle
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.GalleryScanSummary
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.repository.SafScanNote
import com.hanaretamae.kaede.core.rust.AndroidSafVaultScanner
import com.hanaretamae.kaede.core.rust.RustGallerySessionRepository
import com.hanaretamae.kaede.core.rust.RustVaultSelectionRepository
import com.hanaretamae.kaede.core.settings.AndroidSettingsRepository
import com.hanaretamae.kaede.core.settings.LanguagePreference
import com.hanaretamae.kaede.core.settings.GalleryTagCategoryCodec
import com.hanaretamae.kaede.core.settings.GalleryTagPrefixesCodec
import com.hanaretamae.kaede.core.settings.SettingsRepository
import com.hanaretamae.kaede.ui.gallery.KaedeGalleryApp
import com.hanaretamae.kaede.ui.gallery.androidDynamicColorScheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AndroidGalleryRoot() }
    }
}

@Composable
private fun AndroidGalleryRoot() {
    val context = LocalContext.current
    val activity = context.findActivity()
        ?: error("The Compose host must be an Activity.")
    val scope = rememberCoroutineScope()
    val scanner = remember(context) { AndroidSafVaultScanner(context) }
    val sessionRepository = remember { RustGallerySessionRepository() }
    val vaultRepository = remember { RustVaultSelectionRepository() }
    val settingsRepository: SettingsRepository = remember(context) {
        AndroidSettingsRepository(context)
    }
    val privateDataDirectory = remember(context) {
        context.filesDir.resolve("kaede-gallery").absolutePath
    }
    var selectedVault by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<GallerySessionHandle?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingVaultUri by remember { mutableStateOf<Uri?>(null) }
    var pendingSettingsImport by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var pendingSettingsExport by remember { mutableStateOf<String?>(null) }
    var pendingSettingsExportVault by remember { mutableStateOf<Uri?>(null) }

    val settingsImporter = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val accept = pendingSettingsImport
        pendingSettingsImport = null
        if (uri != null && accept != null) {
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        activity.contentResolver.openInputStream(uri)?.use { input ->
                            val bytes = ByteArrayOutputStream().use { output ->
                                val buffer = ByteArray(1024)
                                var total = 0
                                while (true) {
                                    val count = input.read(
                                        buffer,
                                        0,
                                        minOf(buffer.size, 16 * 1024 + 1 - total),
                                    )
                                    if (count < 0) break
                                    if (count == 0) {
                                        throw IOException("Settings file could not be read.")
                                    }
                                    total += count
                                    if (total > 16 * 1024) {
                                        throw IOException("Settings file exceeds size limit.")
                                    }
                                    output.write(buffer, 0, count)
                                }
                                output.toByteArray()
                            }
                            Charsets.UTF_8.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT)
                                .decode(ByteBuffer.wrap(bytes))
                                .toString()
                        } ?: throw IOException("Settings file could not be opened.")
                    }
                    accept(content)
                } catch (_: IOException) {
                    Toast.makeText(context, "The settings file could not be read.", Toast.LENGTH_LONG)
                        .show()
                } catch (_: SecurityException) {
                    Toast.makeText(context, "The settings file could not be read.", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    val settingsExportFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { folderUri ->
        if (folderUri != null) {
            val activeVaultUri = pendingSettingsExportVault
            val content = pendingSettingsExport
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val vaultTree = activeVaultUri
                            ?: throw IOException("No Vault is selected.")
                        val settingsContent = content
                            ?: throw IOException("Settings export is no longer available.")
                        val vaultRoot = DocumentsContract.buildDocumentUriUsingTree(
                            vaultTree,
                            DocumentsContract.getTreeDocumentId(vaultTree),
                        )
                        val folderRoot = DocumentsContract.buildDocumentUriUsingTree(
                            folderUri,
                            DocumentsContract.getTreeDocumentId(folderUri),
                        )
                        val sameVaultRoot = vaultRoot.authority == folderRoot.authority &&
                            DocumentsContract.getDocumentId(vaultRoot) ==
                            DocumentsContract.getDocumentId(folderRoot)
                        if (
                            sameVaultRoot ||
                            DocumentsContract.isChildDocument(
                                activity.contentResolver,
                                vaultRoot,
                                folderRoot,
                            )
                        ) {
                            throw IllegalArgumentException("Cannot export settings inside the Vault.")
                        }
                        val target = DocumentsContract.createDocument(
                            activity.contentResolver,
                            folderRoot,
                            "text/plain",
                            "kaede-gallery-settings.kgsettings",
                        ) ?: throw IOException("Settings file could not be created.")
                        activity.contentResolver.openOutputStream(target, "wt")?.use { output ->
                            output.write(settingsContent.toByteArray(Charsets.UTF_8))
                        } ?: throw IOException("Settings file could not be opened.")
                    }
                    pendingSettingsExport = null
                    pendingSettingsExportVault = null
                } catch (_: IllegalArgumentException) {
                    pendingSettingsExport = null
                    pendingSettingsExportVault = null
                    Toast.makeText(
                        context,
                        "Settings cannot be exported inside the selected Vault.",
                        Toast.LENGTH_LONG,
                    ).show()
                } catch (_: IOException) {
                    pendingSettingsExport = null
                    pendingSettingsExportVault = null
                    Toast.makeText(context, "The settings file could not be written.", Toast.LENGTH_LONG)
                        .show()
                } catch (_: SecurityException) {
                    pendingSettingsExport = null
                    pendingSettingsExportVault = null
                    Toast.makeText(context, "The settings file could not be written.", Toast.LENGTH_LONG)
                        .show()
                }
            }
        } else {
            pendingSettingsExport = null
            pendingSettingsExportVault = null
        }
    }

    suspend fun openVault(
        uri: Uri,
        performScan: Boolean = true,
        allowReplacingSelection: Boolean = false,
    ) {
        val vaultUri = uri.toString()
        if (vaultUri == selectedVault && session != null) {
            loading = false
            return
        }
        loading = true
        error = null
        val previous = when (val selected = vaultRepository.loadSelected(privateDataDirectory)) {
            is RepositoryResult.Failure -> {
                error = errorText(selected.error)
                loading = false
                return
            }
            is RepositoryResult.Success -> selected.value
        }
        if (previous != null && previous != vaultUri && !allowReplacingSelection) {
            pendingVaultUri = uri
            loading = false
            return
        }
        val snapshot = if (performScan) {
            try {
                scanner.snapshot(uri)
            } catch (_: AndroidSafVaultScanner.SafAccessException) {
                if (previous != vaultUri) {
                    try {
                        activity.contentResolver.releasePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    } catch (_: SecurityException) {
                        error = "The selected folder could not be read, and its access grant could not be released."
                        loading = false
                        return
                    }
                }
                error = "The selected folder could not be read. Select it again."
                loading = false
                return
            } catch (_: SecurityException) {
                if (previous != vaultUri) {
                    try {
                        activity.contentResolver.releasePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    } catch (_: SecurityException) {
                        error = "The selected folder permission is unavailable, and its access grant could not be released."
                        loading = false
                        return
                    }
                }
                error = "The selected folder permission is unavailable. Select it again."
                loading = false
                return
            }
        } else {
            null
        }
        if (previous != null && previous != vaultUri) {
            session?.close()
            session = null
            when (
                val forgotten = vaultRepository.clearSelectedData(
                    privateDataDirectory,
                    previous,
                )
            ) {
                is RepositoryResult.Failure -> {
                    error = errorText(forgotten.error)
                    loading = false
                    return
                }
                is RepositoryResult.Success -> Unit
            }
            try {
                activity.contentResolver.releasePersistableUriPermission(
                    Uri.parse(previous),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                error = "The previous folder access could not be released."
                loading = false
                return
            }
        }
        val gallerySettings = when (val loaded = settingsRepository.load()) {
            is RepositoryResult.Failure -> {
                error = errorText(loaded.error)
                loading = false
                return
            }
            is RepositoryResult.Success -> loaded.value
        }
        val galleryTagSettingsFile = File(privateDataDirectory, "gallery-tag-settings.json")
        if (
            gallerySettings.galleryTagPrefixes != GalleryTagPrefixesCodec.DEFAULT_PREFIXES ||
            gallerySettings.flutterTagSettingsJson != null ||
            galleryTagSettingsFile.exists()
        ) {
            when (
                val written = persistRustTagSettings(
                    context,
                    privateDataDirectory,
                    gallerySettings.galleryTagPrefixes,
                    gallerySettings.flutterTagSettingsJson,
                )
            ) {
                is RepositoryResult.Failure -> {
                    error = errorText(written.error)
                    loading = false
                    return
                }
                is RepositoryResult.Success -> Unit
            }
        }
        val opened = sessionRepository.openSaf(
            privateDataDirectory = privateDataDirectory,
            vaultUri = vaultUri,
            indexPath = "$privateDataDirectory/gallery.sqlite",
            thumbnailCachePath = "$privateDataDirectory/thumbnails",
            filePaths = snapshot?.filePaths.orEmpty(),
            batches = snapshot?.batches ?: emptyFlow(),
            performScan = performScan,
        )
        when (opened) {
            is RepositoryResult.Failure -> {
                error = errorText(opened.error)
                loading = false
            }
            is RepositoryResult.Success -> {
                when (val saved = vaultRepository.saveSelected(privateDataDirectory, vaultUri)) {
                    is RepositoryResult.Failure -> {
                        opened.value.close()
                        error = errorText(saved.error)
                        loading = false
                    }
                    is RepositoryResult.Success -> {
                        session?.close()
                        session = AndroidSessionHandle(opened.value, scanner, Uri.parse(vaultUri))
                        selectedVault = vaultUri
                        loading = false
                    }
                }
            }
        }
    }

    suspend fun cancelVaultSwitch(uri: Uri) {
        pendingVaultUri = null
        val selected = vaultRepository.loadSelected(privateDataDirectory)
        if (selected is RepositoryResult.Success && selected.value != uri.toString()) {
            try {
                activity.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                Toast.makeText(
                    context,
                    "The temporary folder access could not be released.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    val vaultPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            try {
                activity.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                scope.launch {
                    val previous = vaultRepository.loadSelected(privateDataDirectory)
                    if (previous is RepositoryResult.Failure) {
                        error = errorText(previous.error)
                    } else {
                        val isSameVault =
                            (previous as RepositoryResult.Success).value == uri.toString()
                        val indexExists =
                            context.filesDir.resolve("kaede-gallery/gallery.sqlite").isFile
                        openVault(uri, performScan = !isSameVault || !indexExists)
                    }
                }
            } catch (_: SecurityException) {
                error = "Read access to the selected folder could not be retained."
            }
        }
    }

    LaunchedEffect(vaultRepository, privateDataDirectory) {
        when (val selected = vaultRepository.loadSelected(privateDataDirectory)) {
            is RepositoryResult.Failure -> {
                error = errorText(selected.error)
                loading = false
            }
            is RepositoryResult.Success -> {
                selectedVault = selected.value
                if (selected.value == null) {
                    loading = false
                } else if (!scanner.hasPersistedReadPermission(Uri.parse(selected.value))) {
                    error = "The selected folder permission is unavailable. Select it again."
                    loading = false
                } else {
                    val indexExists =
                        context.filesDir.resolve("kaede-gallery/gallery.sqlite").isFile
                    openVault(Uri.parse(selected.value), performScan = !indexExists)
                }
            }
        }
    }

    pendingVaultUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { scope.launch { cancelVaultSwitch(uri) } },
            title = { Text("Replace selected Vault?") },
            text = {
                Text(
                    "This removes the previous Vault's local index and thumbnails. " +
                        "Files in either Vault are never changed.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingVaultUri = null
                        scope.launch {
                            openVault(uri, allowReplacingSelection = true)
                        }
                    },
                ) {
                    Text("Replace")
                }
            },
            dismissButton = {
                TextButton(onClick = { scope.launch { cancelVaultSwitch(uri) } }) {
                    Text("Cancel")
                }
            },
        )
    }

    DisposableEffect(session) {
        val activeSession = session
        onDispose { activeSession?.close() }
    }

    val activeSession = session
    val activeVault = selectedVault
    if (activeSession == null || activeVault == null) {
        WelcomeScreen(
            loading = loading,
            error = error,
            onChooseVault = { vaultPicker.launch(null) },
        )
    } else {
        val vaultUri = Uri.parse(activeVault)
        KaedeGalleryApp(
            repository = activeSession.gallery,
            settingsRepository = settingsRepository,
            galleryThumbnail = { mediaId, isVideo ->
                AndroidGalleryThumbnail(
                    scanner = scanner,
                    treeUri = vaultUri,
                    repository = activeSession.gallery,
                    mediaId = mediaId,
                    isVideo = isVideo,
                )
            },
            mediaContent = { state ->
                AndroidMediaContent(
                    scanner = scanner,
                    treeUri = vaultUri,
                    path = state.location,
                    isVideo = state.media.isVideo,
                )
            },
            onExternalLink = { target ->
                val uri = Uri.parse(target)
                if (uri.scheme in setOf("http", "https", "obsidian")) {
                    try {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: android.content.ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            "No application can open the selected link.",
                            Toast.LENGTH_LONG,
                        ).show()
                    } catch (_: SecurityException) {
                        Toast.makeText(
                            context,
                            "The selected link could not be opened.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            },
            onImportSettings = { accept ->
                pendingSettingsImport = accept
                settingsImporter.launch(arrayOf("text/plain", "application/octet-stream"))
            },
            onExportSettings = { content ->
                pendingSettingsExport = content
                pendingSettingsExportVault = vaultUri
                settingsExportFolderPicker.launch(null)
            },
            dynamicColorSchemeProvider = { theme ->
                androidDynamicColorScheme(context, theme)
            },
            onRustTagSettingsChanged = { settings ->
                when (
                    val written = persistRustTagSettings(
                        context,
                        privateDataDirectory,
                        settings.galleryTagPrefixes,
                        settings.flutterTagSettingsJson,
                    )
                ) {
                    is RepositoryResult.Failure -> written
                    is RepositoryResult.Success -> try {
                        val snapshot = scanner.snapshot(vaultUri)
                        activeSession.rescanSaf(snapshot.filePaths, snapshot.batches)
                    } catch (_: IOException) {
                        RepositoryResult.Failure(RepositoryError.VAULT_UNAVAILABLE)
                    } catch (_: SecurityException) {
                        RepositoryResult.Failure(RepositoryError.VAULT_UNAVAILABLE)
                    }
                }
            },
            onRescan = {
                try {
                    val snapshot = scanner.snapshot(vaultUri)
                    activeSession.rescanSaf(snapshot.filePaths, snapshot.batches)
                } catch (_: IOException) {
                    RepositoryResult.Failure(RepositoryError.VAULT_UNAVAILABLE)
                } catch (_: SecurityException) {
                    RepositoryResult.Failure(RepositoryError.VAULT_UNAVAILABLE)
                }
            },
            onChangeVault = { vaultPicker.launch(null) },
            onForgetVault = {
                val selectedUri = activeVault
                session?.close()
                session = null
                when (
                    val forgotten = vaultRepository.clearSelectedData(
                        privateDataDirectory,
                        selectedUri,
                    )
                ) {
                    is RepositoryResult.Failure -> {
                        openVault(
                            vaultUri,
                            performScan = !context.filesDir
                                .resolve("kaede-gallery/gallery.sqlite").isFile,
                        )
                        RepositoryResult.Failure(forgotten.error)
                    }
                    is RepositoryResult.Success -> {
                        selectedVault = null
                        try {
                            activity.contentResolver.releasePersistableUriPermission(
                                vaultUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                            error = null
                            RepositoryResult.Success(Unit)
                        } catch (_: SecurityException) {
                            error = "The folder access grant could not be released."
                            RepositoryResult.Failure(RepositoryError.VAULT_UNAVAILABLE)
                        }
                    }
                }
            },
            systemLanguage = if (
                java.util.Locale.getDefault().language.equals("ja", ignoreCase = true)
            ) {
                LanguagePreference.JAPANESE
            } else {
                LanguagePreference.ENGLISH
            },
        )
    }
}

@Composable
private fun WelcomeScreen(
    loading: Boolean,
    error: String?,
    onChooseVault: () -> Unit,
) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Kaede Gallery", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(16.dp))
                Text("Select an existing Vault folder. Access is read-only.")
                Spacer(Modifier.height(24.dp))
                Button(onClick = onChooseVault, enabled = !loading) {
                    Text("Choose Vault")
                }
                if (loading) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                error?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AndroidMediaContent(
    scanner: AndroidSafVaultScanner,
    treeUri: Uri,
    path: String?,
    isVideo: Boolean,
) {
    val resolvedUri by produceState<Uri?>(null, scanner, treeUri, path) {
        value = if (path == null) null else try {
            scanner.resolveMedia(treeUri, path)
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
    when {
        path == null || resolvedUri == null -> Text(
            "Media is unavailable.",
            color = androidx.compose.ui.graphics.Color.White,
        )
        isVideo -> AndroidVideoPlayer(resolvedUri!!)
        else -> {
            val bitmap by produceState<android.graphics.Bitmap?>(null, scanner, treeUri, path) {
                value = try {
                    scanner.decodeImage(treeUri, path, MAX_VIEWER_IMAGE_SIZE)
                } catch (_: IOException) {
                    null
                } catch (_: SecurityException) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    "Image is unavailable or unsupported.",
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

@Composable
private fun AndroidGalleryThumbnail(
    scanner: AndroidSafVaultScanner,
    treeUri: Uri,
    repository: GalleryRepository,
    mediaId: MediaId,
    isVideo: Boolean,
) {
    val bitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        scanner,
        treeUri,
        repository,
        mediaId,
    ) {
        value = try {
            when (val location = repository.mediaLocation(mediaId)) {
                is RepositoryResult.Failure -> null
                is RepositoryResult.Success -> location.value?.let {
                    if (isVideo) {
                        scanner.decodeVideoFrame(treeUri, it, MAX_GALLERY_THUMBNAIL_SIZE)
                    } else {
                        scanner.decodeImage(treeUri, it, MAX_GALLERY_THUMBNAIL_SIZE)
                    }
                }
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

@Composable
private fun AndroidVideoPlayer(uri: Uri) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { viewContext -> PlayerView(viewContext).apply { this.player = player } },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun errorText(error: RepositoryError): String = when (error) {
    RepositoryError.VAULT_UNAVAILABLE -> "The selected Vault is unavailable."
    RepositoryError.INVALID_VAULT -> "The selected folder is not a valid Vault."
    RepositoryError.STORAGE_UNAVAILABLE -> "Private application storage is unavailable."
    RepositoryError.INDEX_UNAVAILABLE -> "The local gallery index is unavailable."
    RepositoryError.INVALID_REQUEST -> "The Vault request was invalid."
    RepositoryError.OPERATION_FAILED -> "The gallery operation failed."
}

private const val MAX_VIEWER_IMAGE_SIZE = 2_048
private const val MAX_GALLERY_THUMBNAIL_SIZE = 512

private class AndroidSessionHandle(
    private val delegate: GallerySessionHandle,
    scanner: AndroidSafVaultScanner,
    treeUri: Uri,
) : GallerySessionHandle {
    private val closed = AtomicBoolean(false)
    private val notePathsById = ConcurrentHashMap<NoteId, String>()

    override val gallery: GalleryRepository = object : GalleryRepository {
        override suspend fun queryPage(query: GalleryQuery): RepositoryResult<GalleryPage> {
            val result = delegate.gallery.queryPage(query)
            if (result is RepositoryResult.Success) {
                result.value.entries.forEach { entry ->
                    when (entry) {
                        is NoteSummary -> notePathsById[entry.id] = entry.path
                        is MediaSummary -> notePathsById[entry.noteId] = entry.notePath
                    }
                }
            }
            return result
        }

        override suspend fun categories(
            query: GalleryQuery,
        ): RepositoryResult<List<GalleryCategory>> = delegate.gallery.categories(query)

        override suspend fun noteDetail(
            noteId: NoteId,
            safContent: ByteArray?,
        ): RepositoryResult<GalleryNoteDetail?> {
            if (safContent != null) return delegate.gallery.noteDetail(noteId, safContent)
            val path = notePathsById[noteId] ?: when (val resolved = delegate.gallery.notePath(noteId)) {
                is RepositoryResult.Failure -> return resolved
                is RepositoryResult.Success -> resolved.value
                    ?: return RepositoryResult.Failure(RepositoryError.OPERATION_FAILED)
            }.also { notePathsById[noteId] = it }
            val content = try {
                scanner.readNoteContent(treeUri, path)
            } catch (_: AndroidSafVaultScanner.SafAccessException) {
                return RepositoryResult.Failure(RepositoryError.VAULT_UNAVAILABLE)
            }
            return delegate.gallery.noteDetail(noteId, content)
        }

        override suspend fun notePath(noteId: NoteId): RepositoryResult<String?> =
            delegate.gallery.notePath(noteId)

        override suspend fun mediaLocation(mediaId: MediaId): RepositoryResult<String?> =
            delegate.gallery.mediaLocation(mediaId)

        override suspend fun thumbnail(
            mediaId: MediaId,
            size: Int,
        ): RepositoryResult<ByteArray?> = delegate.gallery.thumbnail(mediaId, size)
    }

    override val initialScan: GalleryScanSummary
        get() = delegate.initialScan

    override suspend fun rescan(): RepositoryResult<GalleryScanSummary> {
        notePathsById.clear()
        return delegate.rescan()
    }

    override suspend fun rescanSaf(
        filePaths: List<String>,
        batches: Flow<List<SafScanNote>>,
    ): RepositoryResult<GalleryScanSummary> {
        notePathsById.clear()
        return delegate.rescanSaf(filePaths, batches)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) delegate.close()
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private suspend fun persistRustTagSettings(
    context: Context,
    privateDataDirectory: String,
    prefixes: List<String>,
    flutterTagSettingsJson: String?,
): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
    var temporary: File? = null
    try {
        val filesRoot = context.filesDir.canonicalFile
        val expectedDirectory = File(filesRoot, "kaede-gallery")
        val directory = File(privateDataDirectory).canonicalFile
        if (directory != expectedDirectory) {
            return@withContext RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
        }
        if (!directory.exists() && !directory.mkdir()) {
            return@withContext RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
        }
        if (
            !directory.isDirectory ||
            directory.canonicalFile != expectedDirectory ||
            directory.canonicalFile.parentFile != filesRoot
        ) {
            return@withContext RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
        }
        val target = File(directory, "gallery-tag-settings.json")
        if (
            target.exists() &&
            (!target.isFile || target.canonicalFile != target.absoluteFile)
        ) {
            return@withContext RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
        }
        val content = try {
            GalleryTagCategoryCodec.encodeRustSettings(prefixes, flutterTagSettingsJson)
                .toByteArray(Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            return@withContext RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        }
        if (content.size > GalleryTagPrefixesCodec.MAX_ENCODED_LENGTH) {
            return@withContext RepositoryResult.Failure(RepositoryError.INVALID_REQUEST)
        }
        temporary = File(directory, ".tag-settings-${UUID.randomUUID()}.tmp")
        if (!temporary.createNewFile()) {
            return@withContext RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
        }
        FileOutputStream(temporary).use { output ->
            output.write(content)
            output.fd.sync()
        }
        Os.rename(temporary.absolutePath, target.absolutePath)
        temporary = null
        RepositoryResult.Success(Unit)
    } catch (_: IOException) {
        RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
    } catch (_: SecurityException) {
        RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
    } catch (_: android.system.ErrnoException) {
        RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
    } finally {
        temporary?.delete()
    }
}
