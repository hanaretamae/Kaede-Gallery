package com.hanaretamae.kaede.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.repository.GallerySessionHandle
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.rust.RustGallerySessionRepository
import com.hanaretamae.kaede.core.rust.RustVaultSelectionRepository
import com.hanaretamae.kaede.core.settings.LanguagePreference
import com.hanaretamae.kaede.core.settings.GalleryTagPrefixesCodec
import com.hanaretamae.kaede.core.settings.GalleryTagCategoryCodec
import com.hanaretamae.kaede.core.settings.SettingsRepository
import com.hanaretamae.kaede.ui.gallery.GalleryViewerMediaState
import com.hanaretamae.kaede.ui.gallery.KaedeGalleryApp
import java.awt.Desktop
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.UUID
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileSystemView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

private const val DESKTOP_VIEWER_THUMBNAIL_SIZE = 1_024
private const val DESKTOP_GALLERY_THUMBNAIL_SIZE = 512

fun main() = application {
    val windowState = rememberWindowState(
        width = 1_200.dp,
        height = 820.dp,
        position = WindowPosition(Alignment.Center),
        placement = WindowPlacement.Floating,
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kaede Gallery",
        state = windowState,
    ) {
        DesktopApplication()
    }
}

@Composable
private fun DesktopApplication() {
    val scope = rememberCoroutineScope()
    val sessionRepository = remember { RustGallerySessionRepository() }
    val vaultRepository = remember { RustVaultSelectionRepository() }
    val privateData = remember { privateDataDirectory().toString() }
    val settingsRepository: SettingsRepository = remember(privateData) {
        DesktopSettingsRepository(Path.of(privateData, "settings.properties"))
    }
    var session by remember { mutableStateOf<GallerySessionHandle?>(null) }
    var vaultPath by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun openVault(path: String, performScan: Boolean = true) {
        if (path == vaultPath && session != null) return
        loading = true
        error = null
        val previousPath = when (val previous = vaultRepository.loadSelected(privateData)) {
            is RepositoryResult.Failure -> {
                loading = false
                error = errorText(previous.error)
                return
            }
            is RepositoryResult.Success -> previous.value
        }
        if (previousPath != null && previousPath != path) {
            session?.close()
            session = null
            vaultPath = null
            when (val forgotten = vaultRepository.clearSelectedData(privateData, previousPath)) {
                is RepositoryResult.Failure -> {
                    loading = false
                    error = errorText(forgotten.error)
                    return
                }
                is RepositoryResult.Success -> Unit
            }
        }
        val gallerySettings = when (val loaded = settingsRepository.load()) {
            is RepositoryResult.Failure -> {
                loading = false
                error = errorText(loaded.error)
                return
            }
            is RepositoryResult.Success -> loaded.value
        }
        val galleryTagSettingsFile = Path.of(privateData, "gallery-tag-settings.json")
        if (
            gallerySettings.galleryTagPrefixes != GalleryTagPrefixesCodec.DEFAULT_PREFIXES ||
            gallerySettings.flutterTagSettingsJson != null ||
            Files.exists(galleryTagSettingsFile, LinkOption.NOFOLLOW_LINKS)
        ) {
            when (
                val written = persistRustTagSettings(
                    Path.of(privateData),
                    gallerySettings.galleryTagPrefixes,
                    gallerySettings.flutterTagSettingsJson,
                )
            ) {
                is RepositoryResult.Failure -> {
                    loading = false
                    error = errorText(written.error)
                    return
                }
                is RepositoryResult.Success -> Unit
            }
        }
        when (
            val opened = sessionRepository.openFilesystem(
                privateDataDirectory = privateData,
                vaultPath = path,
                indexPath = Path.of(privateData, "gallery.sqlite").toString(),
                thumbnailCachePath = Path.of(privateData, "thumbnails").toString(),
                performScan = performScan,
            )
        ) {
            is RepositoryResult.Failure -> {
                loading = false
                error = errorText(opened.error)
            }
            is RepositoryResult.Success -> when (
                val saved = vaultRepository.saveSelected(privateData, path)
            ) {
                is RepositoryResult.Failure -> {
                    opened.value.close()
                    loading = false
                    error = errorText(saved.error)
                }
                is RepositoryResult.Success -> {
                    session = DesktopSessionHandle(opened.value)
                    vaultPath = path
                    loading = false
                }
            }
        }
    }

    LaunchedEffect(vaultRepository, privateData) {
        when (val selected = vaultRepository.loadSelected(privateData)) {
            is RepositoryResult.Failure -> {
                error = errorText(selected.error)
                loading = false
            }
            is RepositoryResult.Success -> {
                val selectedPath = selected.value
                if (selectedPath == null) {
                    loading = false
                } else {
                    val indexExists = Path.of(privateData, "gallery.sqlite").toFile().isFile
                    openVault(selectedPath, performScan = !indexExists)
                }
            }
        }
    }

    val activeSession = session
    DisposableEffect(activeSession) {
        onDispose { activeSession?.close() }
    }

    if (activeSession == null || vaultPath == null) {
        DesktopWelcome(
            loading = loading,
            error = error,
            onChooseVault = {
                val chosen = chooseVaultDirectory()
                if (chosen != null) scope.launch { openVault(chosen) }
            },
        )
    } else {
        KaedeGalleryApp(
            repository = activeSession.gallery,
            settingsRepository = settingsRepository,
            galleryThumbnail = { mediaId, isVideo ->
                DesktopGalleryThumbnail(activeSession.gallery, mediaId, isVideo)
            },
            mediaContent = { mediaState ->
                DesktopMediaContent(
                    repository = activeSession.gallery,
                    mediaState = mediaState,
                )
            },
            onExternalLink = { target ->
                val uri = try {
                    URI(target)
                } catch (_: URISyntaxException) {
                    null
                }
                val opened = uri != null &&
                    uri.scheme?.lowercase(java.util.Locale.ROOT) in setOf("http", "https") &&
                    Desktop.isDesktopSupported() &&
                    Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) &&
                    try {
                        Desktop.getDesktop().browse(uri)
                        true
                    } catch (_: IOException) {
                        false
                    } catch (_: SecurityException) {
                        false
                    } catch (_: UnsupportedOperationException) {
                        false
                    }
                if (!opened) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Unable to open the selected link.",
                        "Kaede Gallery",
                        JOptionPane.ERROR_MESSAGE,
                    )
                }
            },
            onImportSettings = { onImported ->
                importSettingsFile(onImported)
            },
            onExportSettings = { content ->
                exportSettingsFile(content, vaultPath)
            },
            onRescan = activeSession::rescan,
            onRustTagSettingsChanged = { settings ->
                when (
                    val written = persistRustTagSettings(
                        Path.of(privateData),
                        settings.galleryTagPrefixes,
                        settings.flutterTagSettingsJson,
                    )
                ) {
                    is RepositoryResult.Failure -> written
                    is RepositoryResult.Success -> activeSession.rescan()
                }
            },
            onChangeVault = {
                val chosen = chooseVaultDirectory()
                if (chosen != null) scope.launch { openVault(chosen) }
            },
            onForgetVault = {
                val selectedPath = vaultPath
                session?.close()
                session = null
                if (selectedPath == null) {
                    RepositoryResult.Failure(RepositoryError.VAULT_UNAVAILABLE)
                } else {
                    when (
                        val forgotten = vaultRepository.clearSelectedData(
                            privateData,
                            selectedPath,
                        )
                    ) {
                        is RepositoryResult.Failure -> {
                            openVault(
                                selectedPath,
                                performScan = !Path.of(privateData, "gallery.sqlite").toFile().isFile,
                            )
                            RepositoryResult.Failure(forgotten.error)
                        }
                        is RepositoryResult.Success -> {
                            vaultPath = null
                            error = null
                            RepositoryResult.Success(Unit)
                        }
                    }
                }
            },
            systemLanguage = if (
                java.util.Locale.getDefault().language == "ja"
            ) {
                LanguagePreference.JAPANESE
            } else {
                LanguagePreference.ENGLISH
            },
        )
    }
}

@Composable
private fun DesktopWelcome(
    loading: Boolean,
    error: String?,
    onChooseVault: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Kaede Gallery", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(16.dp))
                Text("Choose an existing, read-only Vault folder to begin.")
                Spacer(Modifier.height(24.dp))
                Button(onClick = onChooseVault, enabled = !loading) {
                    Text("Choose Vault")
                }
                if (loading) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DesktopMediaContent(
    repository: GalleryRepository,
    mediaState: GalleryViewerMediaState,
) {
    if (mediaState.locationLoading) {
        CircularProgressIndicator()
        return
    }
    val videoPanel = remember { AtomicReference<MpvVideoPanel?>() }
    DisposableEffect(videoPanel) {
        onDispose { videoPanel.getAndSet(null)?.close() }
    }
    if (mediaState.media.isVideo) {
        SwingPanel(
            factory = {
                MpvVideoPanel().also(videoPanel::set)
            },
            update = { panel -> panel.play(mediaState.location) },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    val imageState by produceState(
        initialValue = DesktopImageState(),
        repository,
        mediaState.media.id,
    ) {
        value = withContext(Dispatchers.IO) {
            when (
                val result = repository.thumbnail(
                    mediaState.media.id,
                    DESKTOP_VIEWER_THUMBNAIL_SIZE,
                )
            ) {
                is RepositoryResult.Failure ->
                    DesktopImageState(error = errorText(result.error))
                is RepositoryResult.Success -> {
                    val bytes = result.value
                    if (bytes == null) {
                        DesktopImageState()
                    } else {
                        try {
                            val decoded = ByteArrayInputStream(bytes).use(ImageIO::read)
                            DesktopImageState(bitmap = decoded?.toComposeImageBitmap())
                        } catch (_: IOException) {
                            DesktopImageState(error = "Image thumbnail could not be decoded.")
                        }
                    }
                }
            }
        }
    }

    when {
        imageState.bitmap != null -> Image(
            bitmap = requireNotNull(imageState.bitmap),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        imageState.error != null -> Text(
            "Image unavailable: ${imageState.error}",
            color = androidx.compose.ui.graphics.Color.White,
        )
        else -> Text(
            "Image unavailable or unsupported.",
            color = androidx.compose.ui.graphics.Color.White,
        )
    }
}

@Composable
private fun DesktopGalleryThumbnail(
    repository: GalleryRepository,
    mediaId: MediaId,
    isVideo: Boolean,
) {
    val imageState by produceState(
        initialValue = DesktopImageState(),
        repository,
        mediaId,
        isVideo,
    ) {
        value = withContext(Dispatchers.IO) {
            val bytes = if (isVideo) {
                when (val result = repository.mediaLocation(mediaId)) {
                    is RepositoryResult.Failure -> null
                    is RepositoryResult.Success ->
                        result.value?.let(DesktopVideoThumbnail::extract)
                }
            } else {
                when (val result = repository.thumbnail(mediaId, DESKTOP_GALLERY_THUMBNAIL_SIZE)) {
                    is RepositoryResult.Failure -> null
                    is RepositoryResult.Success -> result.value
                }
            }
            if (bytes == null) {
                DesktopImageState(
                    error = if (isVideo) "Video thumbnail unavailable." else null,
                )
            } else {
                try {
                    val decoded = ByteArrayInputStream(bytes).use(ImageIO::read)
                    DesktopImageState(
                        bitmap = decoded?.toComposeImageBitmap(),
                        error = if (decoded == null && isVideo) {
                            "Video thumbnail unavailable."
                        } else {
                            null
                        },
                    )
                } catch (_: IOException) {
                    DesktopImageState(
                        error = if (isVideo) "Video thumbnail unavailable." else null,
                    )
                }
            }
        }
    }
    when {
        imageState.bitmap != null -> Image(
            bitmap = requireNotNull(imageState.bitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
        imageState.error != null -> Text(
            requireNotNull(imageState.error),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private data class DesktopImageState(
    val bitmap: ImageBitmap? = null,
    val error: String? = null,
)

private fun chooseVaultDirectory(): String? {
    val chooser = JFileChooser(FileSystemView.getFileSystemView().homeDirectory).apply {
        dialogTitle = "Select an existing Vault"
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.path
    } else {
        null
    }
}

private fun importSettingsFile(onImported: (String) -> Unit) {
    val chooser = settingsFileChooser()
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return
    try {
        onImported(DesktopSettingsTransfer.read(chooser.selectedFile.toPath()))
    } catch (_: IOException) {
        showSettingsTransferError()
    } catch (_: SecurityException) {
        showSettingsTransferError()
    } catch (_: IllegalArgumentException) {
        showSettingsTransferError()
    }
}

private fun exportSettingsFile(content: String, vaultPath: String?) {
    val chooser = settingsFileChooser().apply {
        selectedFile = java.io.File("kaede-gallery-settings.kgsettings")
    }
    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return
    try {
        val vault = vaultPath?.let(Path::of)
            ?: throw IOException("No Vault is selected.")
        DesktopSettingsTransfer.write(chooser.selectedFile.toPath(), content, vault)
    } catch (_: IOException) {
        showSettingsTransferError()
    } catch (_: SecurityException) {
        showSettingsTransferError()
    } catch (_: IllegalArgumentException) {
        showSettingsTransferError()
    }
}

private fun settingsFileChooser(): JFileChooser =
    JFileChooser(FileSystemView.getFileSystemView().homeDirectory).apply {
        dialogTitle = "Kaede Gallery settings"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isAcceptAllFileFilterUsed = false
        addChoosableFileFilter(javax.swing.filechooser.FileNameExtensionFilter(
            "Kaede Gallery settings (*.kgsettings)",
            "kgsettings",
        ))
    }

private fun showSettingsTransferError() {
    JOptionPane.showMessageDialog(
        null,
        "The settings file could not be read or written. Vault files are never changed.",
        "Kaede Gallery",
        JOptionPane.ERROR_MESSAGE,
    )
}

private fun privateDataDirectory(): Path {
    val os = System.getProperty("os.name").lowercase()
    val base = if (os.contains("win")) {
        System.getenv("LOCALAPPDATA")
            ?.takeIf(String::isNotBlank)
            ?.let(Path::of)
            ?.takeIf(Path::isAbsolute)
            ?: Path.of(System.getProperty("user.home"), "AppData", "Local")
    } else {
        System.getenv("XDG_DATA_HOME")
            ?.takeIf(String::isNotBlank)
            ?.let(Path::of)
            ?.takeIf(Path::isAbsolute)
            ?: Path.of(System.getProperty("user.home"), ".local", "share")
    }
    return base.resolve("KaedeGallery").toAbsolutePath().normalize()
}

private suspend fun persistRustTagSettings(
    privateDataDirectory: Path,
    prefixes: List<String>,
    flutterTagSettingsJson: String?,
): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
    var temporary: Path? = null
    try {
        val directory = privateDataDirectory.toAbsolutePath().normalize()
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            if (
                Files.isSymbolicLink(directory) ||
                !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
            ) {
                return@withContext RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
            }
        } else {
            Files.createDirectories(directory)
        }
        if (
            Files.isSymbolicLink(directory) ||
            !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
        ) {
            return@withContext RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
        }
        val target = directory.resolve("gallery-tag-settings.json")
        if (
            Files.exists(target, LinkOption.NOFOLLOW_LINKS) &&
            (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))
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
        val temp = Files.createTempFile(directory, ".tag-settings-", ".tmp")
        temporary = temp
        try {
            Files.setPosixFilePermissions(
                temp,
                PosixFilePermissions.fromString("rw-------"),
            )
            Files.setPosixFilePermissions(
                directory,
                PosixFilePermissions.fromString("rwx------"),
            )
        } catch (_: UnsupportedOperationException) {
        }
        Files.write(temp, content, StandardOpenOption.WRITE)
        Files.move(
            temp,
            target,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
        temporary = null
        RepositoryResult.Success(Unit)
    } catch (_: IOException) {
        temporary?.let(Files::deleteIfExists)
        RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
    } catch (_: SecurityException) {
        temporary?.let(Files::deleteIfExists)
        RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE)
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
