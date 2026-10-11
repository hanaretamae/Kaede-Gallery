@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.hanaretamae.kaede.android

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Repeat
import androidx.media3.common.VideoSize
import android.view.TextureView
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import android.app.WallpaperManager
import android.content.ClipData
import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.provider.DocumentsContract
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.system.Os
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
import com.hanaretamae.kaede.core.rust.MAX_GALLERY_THUMBNAIL_DIMENSION
import com.hanaretamae.kaede.core.rust.ThumbnailLoadPriority
import com.hanaretamae.kaede.core.settings.AndroidSettingsRepository
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.core.settings.LanguagePreference
import com.hanaretamae.kaede.core.settings.GalleryTagCategoryCodec
import com.hanaretamae.kaede.core.settings.GalleryTagPrefixesCodec
import com.hanaretamae.kaede.core.settings.SettingsRepository
import com.hanaretamae.kaede.core.settings.SettingsStateHolder
import com.hanaretamae.kaede.core.settings.ThemePreference
import com.hanaretamae.kaede.ui.gallery.GalleryViewerMediaState
import com.hanaretamae.kaede.ui.gallery.KaedeTooltip
import com.hanaretamae.kaede.ui.gallery.KaedeGalleryTheme
import com.hanaretamae.kaede.ui.gallery.KaedeGalleryApp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.hanaretamae.kaede.ui.gallery.GalleryPlaybackActions
import com.hanaretamae.kaede.ui.gallery.GalleryPlaybackStrings
import com.hanaretamae.kaede.ui.gallery.EnglishGalleryPlaybackStrings
import com.hanaretamae.kaede.ui.gallery.GalleryPlaybackSpeedChoices
import com.hanaretamae.kaede.ui.gallery.GallerySettingsScreen
import com.hanaretamae.kaede.ui.gallery.EnglishGallerySettingsStrings
import com.hanaretamae.kaede.ui.gallery.JapaneseGallerySettingsStrings
import com.hanaretamae.kaede.ui.gallery.androidDynamicColorScheme
import com.hanaretamae.kaede.core.settings.platformSystemLanguagePreference
import com.hanaretamae.kaede.core.settings.resolve
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
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
    @Volatile
    private var startupReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holdFirstFrameUntilReady()
        setContent { AndroidGalleryRoot(onStartupReady = { startupReady = true }) }
    }

    /**
     * Keeps the launch window (system splash on Android 12+) until the saved Vault session and
     * settings are ready, so a restored Vault opens straight into the gallery. A short timeout
     * still reveals the restoring indicator when the first scan is slow.
     */
    private fun holdFirstFrameUntilReady() {
        val content = findViewById<android.view.View>(android.R.id.content)
        val startedAt = android.os.SystemClock.uptimeMillis()
        content.viewTreeObserver.addOnPreDrawListener(
            object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val timedOut = android.os.SystemClock.uptimeMillis() - startedAt > STARTUP_HOLD_MILLIS
                    if (!startupReady && !timedOut) {
                        content.postDelayed({ content.invalidate() }, 16)
                        return false
                    }
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }

    private companion object {
        const val STARTUP_HOLD_MILLIS = 2_500L
    }
}

@Composable
private fun AndroidGalleryRoot(onStartupReady: () -> Unit = {}) {
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
    val rootSettingsStateHolder = remember(settingsRepository, scope) {
        SettingsStateHolder(settingsRepository, scope)
    }
    val rootSettingsState by rootSettingsStateHolder.state.collectAsState()
    LaunchedEffect(rootSettingsStateHolder) { rootSettingsStateHolder.load() }
    DisposableEffect(rootSettingsStateHolder) {
        onDispose(rootSettingsStateHolder::dispose)
    }
    val privateDataDirectory = remember(context) {
        context.filesDir.resolve("kaede-gallery").absolutePath
    }
    var selectedVault by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<GallerySessionHandle?>(null) }
    var loading by remember { mutableStateOf(true) }
    var startupAppearance by remember { mutableStateOf(AppearanceSettings()) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingVaultUri by remember { mutableStateOf<Uri?>(null) }
    var pendingSettingsImport by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var pendingSettingsExport by remember { mutableStateOf<String?>(null) }
    var pendingSettingsExportVault by remember { mutableStateOf<Uri?>(null) }
    var showWelcomeExample by remember { mutableStateOf(false) }
    var systemBackHandler by remember { mutableStateOf<(() -> Unit)?>(null) }
    var backGestureProgress by remember { mutableStateOf(0f) }
    val viewerFullscreen = remember { mutableStateOf(false) }
    val viewerWindowFullscreen = remember { mutableStateOf(false) }
    val viewerMediaOnly = remember { mutableStateOf(false) }

    fun updateViewerSystemBars() {
        val insetsController = WindowInsetsControllerCompat(
            activity.window,
            activity.window.decorView,
        )
        if (viewerWindowFullscreen.value || viewerFullscreen.value || viewerMediaOnly.value) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val appearance = rootSettingsState.settings?.appearance ?: startupAppearance
    val useDarkSystemBars = when (appearance.theme) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    SideEffect {
        WindowInsetsControllerCompat(
            activity.window,
            activity.window.decorView,
        ).isAppearanceLightStatusBars = !useDarkSystemBars
    }

    PredictiveBackHandler(enabled = systemBackHandler != null) { progress ->
        try {
            progress.collect { event ->
                backGestureProgress = if (event.swipeEdge == BackEventCompat.EDGE_RIGHT) {
                    -event.progress
                } else {
                    event.progress
                }
            }
            systemBackHandler?.invoke()
        } finally {
            backGestureProgress = 0f
        }
    }

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
            try {
                scanner.clearMediaUriCache(Uri.parse(previous))
            } catch (_: AndroidSafVaultScanner.SafAccessException) {
                error = "The previous Vault's media index could not be cleared."
                loading = false
                return
            }
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
        startupAppearance = gallerySettings.appearance
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
        when (val loaded = settingsRepository.load()) {
            is RepositoryResult.Failure -> {
                error = errorText(loaded.error)
                loading = false
                return@LaunchedEffect
            }
            is RepositoryResult.Success -> startupAppearance = loaded.value.appearance
        }
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
                    shapes = ButtonDefaults.shapes(),
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
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { scope.launch { cancelVaultSwitch(uri) } }) {
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
    if ((loading && error == null) ||
        (rootSettingsState.loading && rootSettingsState.settings == null)
    ) {
        VaultRestoringScreen(startupAppearance)
    } else if (activeSession == null || activeVault == null) {
        SideEffect(onStartupReady)
        SideEffect {
            systemBackHandler = if (showWelcomeExample) {
                { showWelcomeExample = false }
            } else {
                null
            }
        }
        if (showWelcomeExample) {
            val appearance = rootSettingsState.settings?.appearance ?: startupAppearance
            val activeSettingsLanguage = (
                rootSettingsState.settings?.appearance?.language ?: LanguagePreference.SYSTEM
                )
                .resolve(platformSystemLanguagePreference())
            KaedeGalleryTheme(
                appearance = appearance,
                dynamicColorScheme = androidDynamicColorScheme(context, appearance.theme),
            ) {
                GallerySettingsScreen(
                    stateHolder = rootSettingsStateHolder,
                    onClose = { showWelcomeExample = false },
                    strings = if (activeSettingsLanguage == LanguagePreference.JAPANESE) {
                        JapaneseGallerySettingsStrings
                    } else {
                        EnglishGallerySettingsStrings
                    },
                    loadOnEnter = false,
                    disposeStateHolderOnExit = false,
                    openExampleOnStart = showWelcomeExample,
                    exampleOnly = showWelcomeExample,
                    loadLicenseText = { assetPath ->
                        try {
                            context.assets.open(assetPath).bufferedReader().use { it.readText() }
                        } catch (_: IOException) {
                            null
                        }
                    },
                )
            }
        } else {
            WelcomeScreen(
                loading = loading,
                error = error,
                appearance = rootSettingsState.settings?.appearance ?: startupAppearance,
                onChooseVault = { vaultPicker.launch(null) },
                onOpenExample = { showWelcomeExample = true },
            )
        }
    } else {
        val vaultUri = Uri.parse(activeVault)
        suspend fun launchMediaAction(
            media: GalleryViewerMediaState,
            reveal: Boolean = false,
            wallpaper: Boolean = false,
        ) {
            val relativePath = media.location ?: return
            try {
                val uri = scanner.resolveMedia(vaultUri, relativePath)
                val intent = createAndroidMediaIntent(
                    context = context,
                    uri = uri,
                    relativePath = relativePath,
                    reveal = reveal,
                    wallpaper = wallpaper,
                )
                if (wallpaper) {
                    activity.startActivity(intent)
                } else {
                    activity.startActivity(
                        Intent.createChooser(
                            intent,
                            if (reveal) "Show selected media" else "Open selected media",
                        ),
                    )
                }
            } catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "No application can handle this media action.",
                    Toast.LENGTH_LONG,
                ).show()
            } catch (_: IllegalArgumentException) {
                Toast.makeText(
                    context,
                    "The selected media cannot be used for this action.",
                    Toast.LENGTH_LONG,
                ).show()
            } catch (_: IOException) {
                Toast.makeText(
                    context,
                    "The selected media could not be opened.",
                    Toast.LENGTH_LONG,
                ).show()
            } catch (_: SecurityException) {
                Toast.makeText(
                    context,
                    "The selected media could not be opened.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
        KaedeGalleryApp(
            repository = activeSession.gallery,
            onReady = onStartupReady,
            settingsRepository = settingsRepository,
            settingsStateHolder = rootSettingsStateHolder,
            backGestureProgress = backGestureProgress,
            vaultName = DocumentsContract.getTreeDocumentId(vaultUri)
                .substringAfter(':', vaultUri.toString())
                .substringAfterLast('/'),
            scanWarningCount = activeSession.initialScan.warnings.coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt(),
            galleryThumbnail = { mediaId, isVideo, isVisible ->
                AndroidGalleryThumbnail(
                    scanner = scanner,
                    treeUri = vaultUri,
                    repository = activeSession.gallery,
                    mediaId = mediaId,
                    isVideo = isVideo,
                    isVisible = isVisible,
                )
            },
            mediaContent = { state ->
                AndroidMediaContent(
                    scanner = scanner,
                    treeUri = vaultUri,
                    path = state.location,
                    locationLoading = state.locationLoading,
                    isVideo = state.media.isVideo,
                    unavailableLabel = state.unavailableLabel,
                    fullscreen = state.fullscreen,
                    onToggleFullscreen = state.onToggleFullscreen,
                    playbackStrings = state.playbackStrings,
                    onPlaybackOverlayVisibilityChanged =
                        state.onPlaybackOverlayVisibilityChanged,
                    prefetchLocations = state.prefetchLocations,
                )
            },
            onViewerFullscreenChanged = { fullscreen ->
                viewerFullscreen.value = fullscreen
                updateViewerSystemBars()
            },
            viewerWindowFullscreen = viewerWindowFullscreen.value,
            onViewerWindowFullscreenChanged = { fullscreen ->
                viewerWindowFullscreen.value = fullscreen
                updateViewerSystemBars()
            },
            onViewerMediaOnlyChanged = { mediaOnly ->
                viewerMediaOnly.value = mediaOnly
                updateViewerSystemBars()
            },
            onOpenMedia = { media -> scope.launch { launchMediaAction(media) } },
            onRevealMedia = { media ->
                scope.launch { launchMediaAction(media, reveal = true) }
            },
            onSetWallpaperMedia = { media ->
                scope.launch { launchMediaAction(media, wallpaper = true) }
            },
            onOpenVaultNote = { notePath ->
                val uri = androidObsidianOpenUri(vaultUri, notePath)
                if (uri == null) {
                    Toast.makeText(
                        context,
                        "The selected note could not be opened.",
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    try {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: android.content.ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            "No application can open this note.",
                            Toast.LENGTH_LONG,
                        ).show()
                    } catch (_: SecurityException) {
                        Toast.makeText(
                            context,
                            "The selected note could not be opened.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            },
            loadLicenseText = { assetPath ->
                try {
                    context.assets.open(assetPath).bufferedReader().use { it.readText() }
                } catch (_: IOException) {
                    null
                }
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
            onBackHandlerChanged = { systemBackHandler = it },
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
                val startedAt = SystemClock.elapsedRealtime()
                try {
                    val snapshot = scanner.snapshot(vaultUri)
                    activeSession.rescanSaf(snapshot.filePaths, snapshot.batches)
                } catch (_: IOException) {
                    RepositoryResult.Failure(RepositoryError.VAULT_UNAVAILABLE)
                } catch (_: SecurityException) {
                    RepositoryResult.Failure(RepositoryError.VAULT_UNAVAILABLE)
                } finally {
                    Log.d(
                        "KaedeGallerySAF",
                        "SAF rescan completed: " +
                            "durationMs=${SystemClock.elapsedRealtime() - startedAt}",
                    )
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
                        var cleanupError: String? = null
                        try {
                            scanner.clearMediaUriCache(vaultUri)
                        } catch (_: AndroidSafVaultScanner.SafAccessException) {
                            cleanupError = "The local media index could not be cleared."
                        }
                        try {
                            activity.contentResolver.releasePersistableUriPermission(
                                vaultUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        } catch (_: SecurityException) {
                            cleanupError = "The folder access grant could not be released."
                        }
                        selectedVault = null
                        error = cleanupError
                        if (cleanupError == null) {
                            RepositoryResult.Success(Unit)
                        } else {
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

internal fun androidObsidianOpenUri(vaultTreeUri: Uri, notePath: String): Uri? {
    if (!isSafeRelativeNotePath(notePath)) return null
    val vaultDocumentId = try {
        DocumentsContract.getTreeDocumentId(vaultTreeUri)
    } catch (_: IllegalArgumentException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    val vaultName = vaultDocumentId.substringAfter(':', vaultDocumentId).substringAfterLast('/')
    if (vaultName.isBlank()) return null
    return Uri.Builder()
        .scheme("obsidian")
        .authority("open")
        .appendQueryParameter("vault", vaultName)
        .appendQueryParameter("file", notePath)
        .build()
}

private fun isSafeRelativeNotePath(notePath: String): Boolean {
    if (notePath.isBlank() || notePath.startsWith('/') || '\\' in notePath) return false
    return notePath.split('/').none { it.isEmpty() || it == "." || it == ".." }
}

internal fun createAndroidMediaIntent(
    context: Context,
    uri: Uri,
    relativePath: String,
    reveal: Boolean,
    wallpaper: Boolean,
): Intent {
    val extension = relativePath.substringAfterLast('.', "").lowercase()
    val mimeType = android.webkit.MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension)
    if (wallpaper) {
        require(mimeType?.startsWith("image/") == true) { "Wallpaper action requires an image" }
        val clip = ClipData.newRawUri("", uri)
        val cropIntent = if (Build.VERSION.SDK_INT >= 24) {
            try {
                WallpaperManager.getInstance(context).getCropAndSetWallpaperIntent(uri)
            } catch (_: IllegalArgumentException) {
                null
            }
        } else {
            null
        }
        val intent = cropIntent ?: Intent(Intent.ACTION_ATTACH_DATA).apply {
            setDataAndType(uri, mimeType)
            putExtra("mimeType", mimeType)
        }
        return intent.apply {
            clipData = clip
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, if (reveal) "*/*" else mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

@Composable
private fun WelcomeScreen(
    loading: Boolean,
    error: String?,
    appearance: AppearanceSettings,
    onChooseVault: () -> Unit,
    onOpenExample: () -> Unit,
) {
    val context = LocalContext.current
    val japanese = appearance.language.resolve(platformSystemLanguagePreference()) ==
        LanguagePreference.JAPANESE
    KaedeGalleryTheme(
        appearance = appearance,
        dynamicColorScheme = androidDynamicColorScheme(context, appearance.theme),
    ) {
        Box(Modifier.fillMaxSize()) {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Kaede Gallery", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (japanese) {
                                "既存のVaultフォルダーを選択してください。アクセスは読み取り専用です。"
                            } else {
                                "Select an existing Vault folder. Access is read-only."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            shapes = ButtonDefaults.shapes(),
                            onClick = onChooseVault,
                            enabled = !loading,
                        ) {
                            Text(if (japanese) "Vaultを選択" else "Choose Vault")
                        }
                        Button(
                            shapes = ButtonDefaults.shapes(),
                            onClick = onOpenExample,
                            enabled = !loading,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null)
                                Text(if (japanese) "ノートの例を見る" else "View example note")
                            }
                        }
                        error?.let {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (japanese) {
                                    "Vaultを読み込めませんでした。アクセス権限を確認して、フォルダーを選択し直してください。"
                                } else {
                                    it
                                },
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultRestoringScreen(appearance: AppearanceSettings) {
    KaedeGalleryTheme(
        appearance = appearance,
        dynamicColorScheme = androidDynamicColorScheme(
            LocalContext.current,
            appearance.theme,
        ),
    ) {
        com.hanaretamae.kaede.ui.gallery.GalleryLoadingScreen()
    }
}

private sealed interface AndroidImageLoadState {
    data object Loading : AndroidImageLoadState
    data class Ready(
        val bitmap: android.graphics.Bitmap,
        val preview: Boolean = false,
    ) : AndroidImageLoadState
    data object Unavailable : AndroidImageLoadState
}

private sealed interface AndroidResolvedMediaState {
    data object Loading : AndroidResolvedMediaState
    data class Ready(val uri: Uri) : AndroidResolvedMediaState
    data object Unavailable : AndroidResolvedMediaState
}

@Composable
private fun AndroidMediaContent(
    scanner: AndroidSafVaultScanner,
    treeUri: Uri,
    path: String?,
    locationLoading: Boolean,
    isVideo: Boolean,
    unavailableLabel: String = "Media unavailable.",
    fullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    playbackStrings: GalleryPlaybackStrings = EnglishGalleryPlaybackStrings,
    onPlaybackOverlayVisibilityChanged: ((Boolean) -> Unit)? = null,
    prefetchLocations: List<String> = emptyList(),
) {
    if (isVideo) {
        val mediaState by produceState<AndroidResolvedMediaState>(
            initialValue = AndroidResolvedMediaState.Loading,
            scanner,
            treeUri,
            path,
            locationLoading,
        ) {
            value = when {
                locationLoading -> AndroidResolvedMediaState.Loading
                path == null -> AndroidResolvedMediaState.Unavailable
                else -> try {
                    AndroidResolvedMediaState.Ready(scanner.resolveMedia(treeUri, path))
                } catch (_: IOException) {
                    AndroidResolvedMediaState.Unavailable
                } catch (_: SecurityException) {
                    AndroidResolvedMediaState.Unavailable
                }
            }
        }
        when (val loaded = mediaState) {
            AndroidResolvedMediaState.Loading -> LoadingIndicator()
            is AndroidResolvedMediaState.Ready -> AndroidVideoPlayer(
                loaded.uri,
                fullscreen,
                onToggleFullscreen,
                playbackStrings,
                onPlaybackOverlayVisibilityChanged,
            )
            AndroidResolvedMediaState.Unavailable -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(unavailableLabel, color = Color.White)
            }
        }
    } else if (locationLoading) {
        LoadingIndicator()
    } else if (path != null) {
        val imageState = remember(scanner, treeUri, path) {
            mutableStateOf(
                scanner.peekDecodedImage(treeUri, path, MAX_VIEWER_IMAGE_SIZE)
                    ?.let { AndroidImageLoadState.Ready(it) }
                    ?: scanner.peekDecodedImage(treeUri, path, MAX_GALLERY_THUMBNAIL_SIZE)
                        ?.let { AndroidImageLoadState.Ready(it, preview = true) }
                    ?: AndroidImageLoadState.Loading,
            )
        }
        LaunchedEffect(scanner, treeUri, path, prefetchLocations) {
            suspend fun decode(maxDimension: Int): android.graphics.Bitmap? = try {
                scanner.decodeImage(treeUri, path, maxDimension)
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }

            val current = imageState.value
            if (current !is AndroidImageLoadState.Ready || current.preview) {
                val decoded = try {
                    scanner.withThumbnailPermit(ThumbnailLoadPriority.INTERACTIVE) {
                        decode(MAX_VIEWER_IMAGE_SIZE)
                    }
                } catch (_: IOException) {
                    null
                } catch (_: SecurityException) {
                    null
                }
                imageState.value = when {
                    decoded != null -> AndroidImageLoadState.Ready(decoded)
                    imageState.value is AndroidImageLoadState.Ready -> imageState.value
                    else -> AndroidImageLoadState.Unavailable
                }
            }

            coroutineScope {
                prefetchLocations.asSequence().filter { it != path }.map { location ->
                    async {
                        try {
                            scanner.withThumbnailPermit(ThumbnailLoadPriority.PREFETCH) {
                                scanner.decodeImage(treeUri, location, MAX_VIEWER_IMAGE_SIZE)
                            }
                        } catch (_: IOException) {
                        } catch (_: SecurityException) {
                        }
                    }
                }.toList().awaitAll()
            }
        }
        when (val loaded = imageState.value) {
            AndroidImageLoadState.Loading -> LoadingIndicator()
            is AndroidImageLoadState.Ready -> Image(
                bitmap = loaded.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier.fillMaxSize(),
            )
            AndroidImageLoadState.Unavailable -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(unavailableLabel, color = Color.White)
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(unavailableLabel, color = Color.White)
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
    isVisible: Boolean,
) {
    val bitmapState = remember(scanner, treeUri, repository, mediaId) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(scanner, treeUri, repository, mediaId, isVisible) {
        if (!isVisible || bitmapState.value != null) return@LaunchedEffect
        bitmapState.value = scanner.withThumbnailPermit(ThumbnailLoadPriority.VISIBLE) {
            try {
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
    }
    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        LoadingIndicator()
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AndroidVideoPlayer(
    uri: Uri,
    fullscreen: Boolean,
    onToggleFullscreen: (() -> Unit)?,
    playbackStrings: GalleryPlaybackStrings,
    onPlaybackOverlayVisibilityChanged: ((Boolean) -> Unit)?,
) {
    val context = LocalContext.current
    var looping by remember(uri) { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    var videoAspectRatio by remember(player) { mutableFloatStateOf(0f) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio =
                        videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }
        }
        player.addListener(listener)
        onDispose {
            onPlaybackOverlayVisibilityChanged?.invoke(false)
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player, looping) {
        player.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }
    LaunchedEffect(player, muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(player, speed) { player.setPlaybackSpeed(speed) }

    var playbackPositionMs by remember(player) { mutableLongStateOf(0L) }
    var playbackDurationMs by remember(player) { mutableLongStateOf(0L) }
    var isPlaying by remember(player) { mutableStateOf(false) }
    var controlsVisible by remember(player) { mutableStateOf(true) }
    var scrubFraction by remember(player) { mutableStateOf<Float?>(null) }
    var showSpeedMenu by remember(player) { mutableStateOf(false) }
    var lastInteractionAtMs by remember(player) { mutableLongStateOf(0L) }

    fun revealControls() {
        controlsVisible = true
        lastInteractionAtMs = SystemClock.elapsedRealtime()
    }

    fun seekTo(positionMs: Long) {
        val durationMs = playbackDurationMs
        val target = positionMs.coerceAtLeast(0L).let {
            if (durationMs > 0L) it.coerceAtMost(durationMs) else it
        }
        // ExoPlayer stays in STATE_ENDED after a seek unless playback is requested again.
        val ended = player.playbackState == Player.STATE_ENDED
        player.seekTo(target)
        if (ended && target < durationMs) player.play()
        playbackPositionMs = target
    }

    LaunchedEffect(fullscreen) { revealControls() }
    LaunchedEffect(player) {
        while (true) {
            val durationMs = player.duration.takeIf { it > 0L } ?: 0L
            playbackDurationMs = durationMs
            playbackPositionMs = if (
                player.playbackState == Player.STATE_ENDED && durationMs > 0L
            ) {
                durationMs
            } else {
                player.currentPosition.coerceAtLeast(0L)
            }
            isPlaying = player.isPlaying
            delay(200)
        }
    }
    LaunchedEffect(controlsVisible, isPlaying, lastInteractionAtMs, scrubFraction, showSpeedMenu) {
        if (controlsVisible && isPlaying && scrubFraction == null && !showSpeedMenu) {
            delay(PLAYER_CONTROLS_TIMEOUT_MILLIS)
            if (SystemClock.elapsedRealtime() - lastInteractionAtMs >= PLAYER_CONTROLS_TIMEOUT_MILLIS) {
                controlsVisible = false
            }
        }
    }

    val durationMs = playbackDurationMs
    val sliderValue = scrubFraction ?: if (durationMs > 0L) {
        (playbackPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val positionMs = scrubFraction?.let { (it * durationMs).toLong() } ?: playbackPositionMs
    val sliderState = androidx.compose.material3.rememberSliderState(value = 0f)
    if (scrubFraction == null) sliderState.value = sliderValue
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(player) {
            detectTapGestures {
                controlsVisible = !controlsVisible
                lastInteractionAtMs = SystemClock.elapsedRealtime()
            }
        },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val fittedWidth = if (videoAspectRatio > 0f) {
                    minOf(maxWidth, maxHeight * videoAspectRatio)
                } else {
                    maxWidth
                }
                val fittedHeight = if (videoAspectRatio > 0f) {
                    minOf(maxHeight, maxWidth / videoAspectRatio)
                } else {
                    maxHeight
                }
                // TextureView avoids the black SurfaceView hole during viewer transitions.
                AndroidView(
                    factory = { viewContext ->
                        TextureView(viewContext).also(player::setVideoTextureView)
                    },
                    update = player::setVideoTextureView,
                    onRelease = player::clearVideoTextureView,
                    modifier = Modifier.size(width = fittedWidth, height = fittedHeight),
                )
            }
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AnimatedContent(
                        targetState = showSpeedMenu,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 5 }) togetherWith
                                (fadeOut() + slideOutVertically { -it / 5 })
                        },
                        label = "androidPlaybackSpeedChoices",
                    ) { choosingSpeed ->
                        if (choosingSpeed) {
                            GalleryPlaybackSpeedChoices(
                                selected = speed,
                                onSelect = {
                                    speed = it
                                    showSpeedMenu = false
                                    onPlaybackOverlayVisibilityChanged?.invoke(false)
                                    revealControls()
                                },
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Slider(
                                    state = sliderState,
                                    onValueChange = {
                                        if (durationMs > 0L) {
                                            sliderState.value = it
                                            scrubFraction = it
                                            seekTo((it * durationMs).toLong())
                                            revealControls()
                                        }
                                    },
                                    onValueChangeFinished = {
                                        scrubFraction?.let { seekTo((it * durationMs).toLong()) }
                                        scrubFraction = null
                                        revealControls()
                                    },
                                    enabled = durationMs > 0L,
                                    modifier = Modifier.fillMaxWidth().height(64.dp),
                                )
                                GalleryPlaybackActions(
                                    leading = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            KaedeTooltip(
                                                if (isPlaying) playbackStrings.pauseVideo
                                                else playbackStrings.playVideo,
                                            ) {
                                                FilledIconButton(
                                                    shapes = IconButtonDefaults.shapes(),
                                                    onClick = {
                                                        when {
                                                            isPlaying -> player.pause()
                                                            player.playbackState == Player.STATE_ENDED -> {
                                                                player.seekTo(0L)
                                                                player.play()
                                                            }
                                                            else -> player.play()
                                                        }
                                                        isPlaying = !isPlaying
                                                        revealControls()
                                                    },
                                                ) {
                                                    Icon(
                                                        if (isPlaying) {
                                                            Icons.Filled.Pause
                                                        } else {
                                                            Icons.Filled.PlayArrow
                                                        },
                                                        if (isPlaying) playbackStrings.pauseVideo
                                                        else playbackStrings.playVideo,
                                                    )
                                                }
                                            }
                                            PlaybackPill(
                                                "${playbackTimeLabel(positionMs)} / ${playbackTimeLabel(durationMs)}",
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    },
                                    trailing = {
                                        PlaybackPill(
                                            label = "${speed.toPlaybackSpeedLabel()}x",
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            onClick = {
                                                showSpeedMenu = true
                                                onPlaybackOverlayVisibilityChanged?.invoke(true)
                                                revealControls()
                                            },
                                        )
                                        KaedeTooltip(
                                            if (looping) playbackStrings.turnLoopingOff
                                            else playbackStrings.loopVideo,
                                        ) {
                                            FilledTonalIconToggleButton(
                                                checked = looping,
                                                onCheckedChange = {
                                                    looping = it
                                                    revealControls()
                                                },
                                                shapes = IconButtonDefaults.toggleableShapes(),
                                                colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                    checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                                ),
                                            ) {
                                                Icon(
                                                    Icons.Filled.Repeat,
                                                    if (looping) playbackStrings.loopOn
                                                    else playbackStrings.loopOff,
                                                )
                                            }
                                        }
                                        KaedeTooltip(
                                            if (muted) playbackStrings.unmuteVideo
                                            else playbackStrings.muteVideo,
                                        ) {
                                            FilledTonalIconToggleButton(
                                                checked = !muted,
                                                onCheckedChange = {
                                                    muted = !it
                                                    revealControls()
                                                },
                                                shapes = IconButtonDefaults.toggleableShapes(),
                                                colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                ),
                                            ) {
                                                Icon(
                                                    if (muted) {
                                                        Icons.AutoMirrored.Filled.VolumeOff
                                                    } else {
                                                        Icons.AutoMirrored.Filled.VolumeUp
                                                    },
                                                    if (muted) playbackStrings.unmuteVideo
                                                    else playbackStrings.muteVideo,
                                                )
                                            }
                                        }
                                        onToggleFullscreen?.let { toggle ->
                                            KaedeTooltip(
                                                if (fullscreen) playbackStrings.exitFullScreen
                                                else playbackStrings.fullScreen,
                                            ) {
                                                FilledTonalIconButton(
                                                    shapes = IconButtonDefaults.shapes(),
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    ),
                                                    onClick = {
                                                        toggle()
                                                        revealControls()
                                                    },
                                                ) {
                                                    Icon(
                                                        if (fullscreen) {
                                                            Icons.Filled.FullscreenExit
                                                        } else {
                                                            Icons.Filled.Fullscreen
                                                        },
                                                        if (fullscreen) playbackStrings.exitFullScreen
                                                        else playbackStrings.fullScreen,
                                                    )
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (!controlsVisible) {
            onToggleFullscreen?.let { toggle ->
                KaedeTooltip(
                    if (fullscreen) playbackStrings.exitFullScreen
                    else playbackStrings.fullScreen,
                ) {
                    FilledTonalIconButton(
                        shapes = IconButtonDefaults.shapes(),
                        onClick = {
                            toggle()
                            revealControls()
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    ) {
                        Icon(
                            if (fullscreen) Icons.Filled.FullscreenExit
                            else Icons.Filled.Fullscreen,
                            if (fullscreen) playbackStrings.exitFullScreen
                            else playbackStrings.fullScreen,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        )
    }
    if (onClick == null) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            content = content,
        )
    } else {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            content = content,
        )
    }
}

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private const val PLAYER_CONTROLS_TIMEOUT_MILLIS = 3_500L

private fun Float.toPlaybackSpeedLabel(): String =
    if (this * 10f % 1f == 0f) "%.1f".format(this) else "%.2f".format(this)

private fun playbackTimeLabel(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1_000L).toInt()
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60
    val hours = totalSeconds / 3_600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun errorText(error: RepositoryError): String = when (error) {
    RepositoryError.VAULT_UNAVAILABLE -> "The selected Vault is unavailable."
    RepositoryError.INVALID_VAULT -> "The selected folder is not a valid Vault."
    RepositoryError.STORAGE_UNAVAILABLE -> "Private application storage is unavailable."
    RepositoryError.INDEX_UNAVAILABLE -> "The local gallery index is unavailable."
    RepositoryError.INVALID_REQUEST -> "The Vault request was invalid."
    RepositoryError.OPERATION_FAILED -> "The gallery operation failed."
}

private const val MAX_VIEWER_IMAGE_SIZE = 8_192
private const val MAX_GALLERY_THUMBNAIL_SIZE =
    MAX_GALLERY_THUMBNAIL_DIMENSION

private class AndroidSessionHandle(
    private val delegate: GallerySessionHandle,
    scanner: AndroidSafVaultScanner,
    treeUri: Uri,
) : GallerySessionHandle {
    private val closed = AtomicBoolean(false)
    private val notePathsById = ConcurrentHashMap<NoteId, String>()
    private val mediaLocationsById = ConcurrentHashMap<MediaId, CachedMediaLocation>()
    private val mediaLocationCacheLock = Any()
    private var mediaLocationCacheRevision = 0L

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

        override suspend fun mediaLocation(mediaId: MediaId): RepositoryResult<String?> {
            val revision = synchronized(mediaLocationCacheLock) {
                mediaLocationsById[mediaId]?.let {
                    return RepositoryResult.Success(it.path)
                }
                mediaLocationCacheRevision
            }
            return when (val result = delegate.gallery.mediaLocation(mediaId)) {
                is RepositoryResult.Failure -> result
                is RepositoryResult.Success -> {
                    synchronized(mediaLocationCacheLock) {
                        if (revision == mediaLocationCacheRevision) {
                            mediaLocationsById[mediaId] = CachedMediaLocation(result.value)
                        }
                    }
                    result
                }
            }
        }

        override suspend fun thumbnail(
            mediaId: MediaId,
            size: Int,
        ): RepositoryResult<ByteArray?> = delegate.gallery.thumbnail(mediaId, size)
    }

    override val initialScan: GalleryScanSummary
        get() = delegate.initialScan

    override suspend fun rescan(): RepositoryResult<GalleryScanSummary> {
        notePathsById.clear()
        clearMediaLocationCache()
        return try {
            delegate.rescan()
        } finally {
            clearMediaLocationCache()
        }
    }

    override suspend fun rescanSaf(
        filePaths: List<String>,
        batches: Flow<List<SafScanNote>>,
    ): RepositoryResult<GalleryScanSummary> {
        notePathsById.clear()
        clearMediaLocationCache()
        return try {
            delegate.rescanSaf(filePaths, batches)
        } finally {
            clearMediaLocationCache()
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) delegate.close()
    }

    private fun clearMediaLocationCache() {
        synchronized(mediaLocationCacheLock) {
            mediaLocationCacheRevision++
            mediaLocationsById.clear()
        }
    }
}

private data class CachedMediaLocation(val path: String?)

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
