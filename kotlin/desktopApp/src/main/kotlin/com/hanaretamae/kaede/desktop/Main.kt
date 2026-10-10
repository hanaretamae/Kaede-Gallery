package com.hanaretamae.kaede.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState

import com.hanaretamae.kaede.core.model.MediaId
import com.hanaretamae.kaede.core.repository.GallerySessionHandle
import com.hanaretamae.kaede.core.repository.GalleryRepository
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.rust.RustGallerySessionRepository
import com.hanaretamae.kaede.core.rust.RustVaultSelectionRepository
import com.hanaretamae.kaede.core.settings.LanguagePreference
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.core.settings.GalleryTagPrefixesCodec
import com.hanaretamae.kaede.core.settings.GalleryTagCategoryCodec
import com.hanaretamae.kaede.core.settings.SettingsRepository
import com.hanaretamae.kaede.ui.gallery.GalleryViewerMediaState
import com.hanaretamae.kaede.ui.gallery.KaedeGalleryApp
import com.hanaretamae.kaede.ui.gallery.KaedeGalleryTheme

import java.awt.Desktop
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.InvalidPathException
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.UUID
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.Timer
import javax.swing.filechooser.FileSystemView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

private const val DESKTOP_VIEWER_THUMBNAIL_SIZE = 1_024
private const val DESKTOP_GALLERY_THUMBNAIL_SIZE = 512
internal val DESKTOP_MINIMUM_WINDOW_SIZE = DpSize(540.dp, 720.dp)
private const val DESKTOP_LAYOUT_DEBUG_PROPERTY = "kaede.debugLayout"
private const val PORTAL_REFRESH_INTERVAL_MILLIS = 15_000L

fun main() {
    System.setProperty("awtAppClassName", "com.hanaretamae.kaede")
    application {
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
            resizable = true,
        ) {
            val density = LocalDensity.current
            val debugLayout = java.lang.Boolean.getBoolean(DESKTOP_LAYOUT_DEBUG_PROPERTY)
            var composeRootSize by remember { mutableStateOf(IntSize.Zero) }
            LaunchedEffect(composeRootSize, windowState.size, density) {
                if (debugLayout && composeRootSize != IntSize.Zero) {
                    System.err.println(
                        "Kaede Gallery layout: compose=${composeRootSize.width}x${composeRootSize.height}px, " +
                            "density=${density.density}, windowState=${windowState.size}",
                    )
                }

            }
            Box(
                modifier = Modifier.fillMaxSize().onSizeChanged { size ->
                    if (composeRootSize != size) composeRootSize = size
                },
            ) {
                DesktopApplication(windowState) { fullscreen ->
                    windowState.placement = if (fullscreen) {
                        WindowPlacement.Fullscreen
                    } else {
                        WindowPlacement.Floating
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DesktopApplication(
    windowState: WindowState,
    onFullscreenChanged: (Boolean) -> Unit,
) {
    val portalAppearance by produceState(DesktopSystemAppearance()) {
        while (true) {
            value = withContext(Dispatchers.IO) { readDesktopSystemAppearance() }
            delay(PORTAL_REFRESH_INTERVAL_MILLIS)
        }
    }
    val awtWindow = LocalAwtWindow.current
    val density = LocalDensity.current
    val awtFrame = awtWindow as? java.awt.Frame
    if (awtFrame != null) {
        DisposableEffect(awtFrame) {
            val previousIcon = awtFrame.iconImage
            val iconUrl = requireNotNull(
                Thread.currentThread().contextClassLoader
                    .getResource("branding/kaede-gallery-icon.png"),
            ) { "The bundled application icon is missing." }
            val icon = requireNotNull(ImageIO.read(iconUrl)) {
                "The bundled application icon is invalid."
            }
            awtFrame.iconImage = icon
            onDispose { awtFrame.iconImage = previousIcon }
        }
    }
    val minimumWidthPx = with(density) { DESKTOP_MINIMUM_WINDOW_SIZE.width.roundToPx() }
    val minimumHeightPx = with(density) { DESKTOP_MINIMUM_WINDOW_SIZE.height.roundToPx() }
    if (awtWindow != null) {
        DisposableEffect(awtWindow, density, minimumWidthPx, minimumHeightPx) {
            val previousMinimumSize = Dimension(awtWindow.minimumSize)
            awtWindow.minimumSize = Dimension(minimumWidthPx, minimumHeightPx)
            val debugLayout = java.lang.Boolean.getBoolean(DESKTOP_LAYOUT_DEBUG_PROPERTY)
            val nativeGeometry = (awtWindow as? ComposeWindow)?.let { window ->
                LinuxX11WindowGeometry.open(window.windowHandle)
            }
            var initialBoundsApplied = false
            var lastObservedSize = Dimension(awtWindow.size)
            fun logWindowSize(force: Boolean = false) {
                if (!debugLayout) return
                val currentSize = Dimension(awtWindow.size)
                if (!force && currentSize == lastObservedSize) return
                lastObservedSize = currentSize
                val insets = awtWindow.insets
                val minimumSize = awtWindow.minimumSize
                val maximumSize = awtWindow.maximumSize
                val graphicsConfiguration = awtWindow.graphicsConfiguration
                val transform = graphicsConfiguration.defaultTransform
                val resizable = (awtWindow as? java.awt.Frame)?.isResizable
                val nativeHandle = (awtWindow as? ComposeWindow)?.windowHandle
                    ?.toString(16)
                val contentPane = (awtWindow as? javax.swing.JFrame)?.contentPane
                val children = contentPane?.components?.joinToString { child ->
                    "${child.javaClass.simpleName}=${child.width}x${child.height}"
                }
                System.err.println(
                    "Kaede Gallery layout: awt=${currentSize.width}x${currentSize.height}px, " +
                        "insets=${insets.left},${insets.top},${insets.right},${insets.bottom}, " +
                        "min=${minimumSize.width}x${minimumSize.height}, " +
                        "max=${maximumSize.width}x${maximumSize.height}, " +
                        "resizable=$resizable, " +
                        "scale=${transform.scaleX}x${transform.scaleY}, " +
                        "nativeHandle=0x$nativeHandle, " +
                        "contentPane=${contentPane?.width}x${contentPane?.height}, children=$children",
                )
            }
            fun synchronizeNativeSize() {
                if (!awtWindow.isShowing) return
                if (!initialBoundsApplied) {
                    val requestedSize = with(density) {
                        Dimension(windowState.size.width.roundToPx(), windowState.size.height.roundToPx())
                    }
                    if (awtWindow.size != requestedSize) return
                    initialBoundsApplied = true
                }
                val nativeSize = nativeGeometry?.dimensions() ?: return
                val transform = awtWindow.graphicsConfiguration.defaultTransform
                val targetSize = awtWindowSizeFromX11(
                    dimensions = nativeSize,
                    scaleX = transform.scaleX,
                    scaleY = transform.scaleY,
                    insets = awtWindow.insets,
                )
                if (targetSize.width <= 0 || targetSize.height <= 0) return
                if (awtWindow.size != targetSize) {
                    if (debugLayout) {
                        System.err.println(
                            "Kaede Gallery layout: syncing AWT to X11 " +
                                "${targetSize.width}x${targetSize.height}px",
                        )
                    }
                    awtWindow.size = targetSize
                }
                val targetStateSize = with(density) {
                    DpSize(targetSize.width.toDp(), targetSize.height.toDp())
                }
                if (windowState.size != targetStateSize) {
                    windowState.size = targetStateSize
                }
            }
            val resizeListener = object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) = logWindowSize()
            }
            val sizeProbe = Timer(250) {
                synchronizeNativeSize()
                logWindowSize()
            }
            if (debugLayout) {
                awtWindow.addComponentListener(resizeListener)
                logWindowSize(force = true)
            }
            if (nativeGeometry != null || debugLayout) sizeProbe.start()
            onDispose {
                sizeProbe.stop()
                if (debugLayout) awtWindow.removeComponentListener(resizeListener)
                nativeGeometry?.close()
                awtWindow.minimumSize = previousMinimumSize
            }
        }
    }

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
    var loadingMessage by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun openVault(path: String, performScan: Boolean = true) {
        if (path == vaultPath && session != null) return
        loading = true
        loadingMessage = "Loading Vault..."
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
            loadingMessage = loadingMessage,
            error = error,
            systemAppearance = portalAppearance,
            onChooseVault = {
                scope.launch {
                    loading = true
                    loadingMessage = "Waiting for folder selection..."
                    error = null
                    try {
                        val selectedPath = chooseVaultDirectory()
                        if (selectedPath == null) {
                            loading = false
                            loadingMessage = null
                            error = "No folder was selected. Use Open or Select to confirm the folder."
                        } else {
                            openVault(selectedPath)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        loading = false
                        loadingMessage = null
                        error = failure.message
                            ?.takeIf { failure is PortalSelectionException }
                            ?: "The selected folder could not be opened."
                    }
                }
            },
        )
    } else {
        val selectedVaultPath = vaultPath
        KaedeGalleryApp(
            repository = activeSession.gallery,
            settingsRepository = settingsRepository,
            dynamicColorSchemeProvider = { themePreference ->
                val darkTheme = when (themePreference) {
                    com.hanaretamae.kaede.core.settings.ThemePreference.SYSTEM ->
                        portalAppearance.darkTheme ?: false
                    com.hanaretamae.kaede.core.settings.ThemePreference.LIGHT -> false
                    com.hanaretamae.kaede.core.settings.ThemePreference.DARK -> true
                }
                desktopSystemColorScheme(portalAppearance.accentColor, darkTheme)
            },
            systemDarkTheme = portalAppearance.darkTheme,
            vaultName = Path.of(vaultPath).fileName?.toString(),
            scanWarningCount = activeSession.initialScan.warnings.coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt(),
            galleryThumbnail = { mediaId, isVideo ->
                DesktopGalleryThumbnail(activeSession.gallery, mediaId, isVideo)
            },
            mediaContent = { mediaState ->
                DesktopMediaContent(
                    repository = activeSession.gallery,
                    mediaState = mediaState,
                )
            },
            onViewerFullscreenChanged = onFullscreenChanged,
            onOpenMedia = { media ->
                scope.launch {
                    val opened = withContext(Dispatchers.IO) {
                        launchDesktopMedia(media.location, selectedVaultPath, reveal = false)
                    }
                    if (!opened) showDesktopMediaError(reveal = false)
                }
            },
            onRevealMedia = { media ->
                scope.launch {
                    val opened = withContext(Dispatchers.IO) {
                        launchDesktopMedia(media.location, selectedVaultPath, reveal = true)
                    }
                    if (!opened) showDesktopMediaError(reveal = true)
                }
            },
            onSetWallpaperMedia = if (isGnomeDesktop()) {
                { media ->
                    scope.launch {
                        val succeeded = withContext(Dispatchers.IO) {
                            setLinuxWallpaper(media.location, selectedVaultPath)
                        }
                        if (!succeeded) {
                            JOptionPane.showMessageDialog(
                                null,
                                "Unable to set the selected image as wallpaper.",
                                "Kaede Gallery",
                                JOptionPane.ERROR_MESSAGE,
                            )
                        }
                    }
                }
            } else {
                null
            },
            onOpenVaultNote = { notePath ->
                scope.launch {
                    val opened = withContext(Dispatchers.IO) {
                        launchObsidianNote(notePath, selectedVaultPath)
                    }
                    if (!opened) {
                        JOptionPane.showMessageDialog(
                            null,
                            "Unable to open the selected note in Obsidian.",
                            "Kaede Gallery",
                            JOptionPane.ERROR_MESSAGE,
                        )
                    }
                }
            },
            loadLicenseText = ::loadDesktopLicenseText,
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
                scope.launch {
                    chooseVaultDirectory()?.let { openVault(it) }
                }
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

private fun launchDesktopMedia(location: String?, vaultPath: String?, reveal: Boolean): Boolean {
    val path = resolveVaultMediaPath(location, vaultPath)
    return when {
        path == null -> false
        reveal && isLinuxDesktop() -> showInLinuxFileManager(path)
        reveal && isWindowsDesktop() -> showInWindowsFileManager(path)
        else -> Desktop.isDesktopSupported() &&
            try {
                val desktop = Desktop.getDesktop()
                if (!desktop.isSupported(Desktop.Action.OPEN)) {
                    false
                } else {
                    desktop.open(if (reveal) path.parent.toFile() else path.toFile())
                    true
                }
            } catch (_: IOException) {
                false
            } catch (_: SecurityException) {
                false
            } catch (_: UnsupportedOperationException) {
                false
            }
    }
}

private fun showDesktopMediaError(reveal: Boolean) {
    JOptionPane.showMessageDialog(
        null,
        if (reveal) "Unable to show the selected media in the file manager."
        else "Unable to open the selected media.",
        "Kaede Gallery",
        JOptionPane.ERROR_MESSAGE,
    )
}

private fun loadDesktopLicenseText(assetPath: String): String? = try {
    Thread.currentThread().contextClassLoader
        .getResourceAsStream(assetPath)
        ?.bufferedReader()
        ?.use { it.readText() }
} catch (_: IOException) {
    null
}

private fun launchObsidianNote(notePath: String, vaultPath: String?): Boolean {
    val note = resolveVaultNotePath(notePath, vaultPath)
    val uri = note?.let(::obsidianOpenUri)
    return uri != null &&
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
}

private fun setLinuxWallpaper(location: String?, vaultPath: String?): Boolean {
    if (!isGnomeDesktop()) return false
    val image = resolveVaultMediaPath(location, vaultPath) ?: return false
    if (image.fileName.toString().substringAfterLast('.', "").lowercase() !in
        setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
    ) {
        return false
    }
    val uri = image.toUri().toASCIIString()
    return runGSettings("picture-uri", uri) && runGSettings("picture-uri-dark", uri)
}

private fun runGSettings(key: String, value: String): Boolean {
    val process = try {
        ProcessBuilder(
            "gsettings",
            "set",
            "org.gnome.desktop.background",
            key,
            value,
        ).redirectInput(ProcessBuilder.Redirect.from(Path.of("/dev/null").toFile()))
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (_: IOException) {
        return false
    } catch (_: SecurityException) {
        return false
    }
    return try {
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            false
        } else {
            process.exitValue() == 0
        }
    } catch (_: InterruptedException) {
        process.destroyForcibly()
        Thread.currentThread().interrupt()
        false
    }
}

internal fun obsidianOpenUri(note: Path): URI? {
    val encodedPath = URLEncoder.encode(note.toString(), StandardCharsets.UTF_8)
    return try {
        URI("obsidian://open?path=$encodedPath")
    } catch (_: URISyntaxException) {
        null
    }
}

internal fun isLinuxDesktop(): Boolean =
    System.getProperty("os.name").lowercase(java.util.Locale.ROOT).contains("linux")

private fun isWindowsDesktop(): Boolean =
    System.getProperty("os.name").lowercase(java.util.Locale.ROOT).contains("windows")

private fun isGnomeDesktop(): Boolean =
    isLinuxDesktop() &&
        (System.getenv("XDG_CURRENT_DESKTOP") ?: "")
            .uppercase(java.util.Locale.ROOT).contains("GNOME")

private fun showInWindowsFileManager(media: Path): Boolean {
    val process = try {
        ProcessBuilder(windowsFileManagerShowItemCommand(media))
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (_: IOException) {
        return false
    } catch (_: SecurityException) {
        return false
    }
    return process.isAlive || process.exitValue() == 0
}

private fun showInLinuxFileManager(media: Path): Boolean {
    val process = try {
        ProcessBuilder(linuxFileManagerShowItemsCommand(media))
            .redirectInput(ProcessBuilder.Redirect.from(Path.of("/dev/null").toFile()))
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (_: IOException) {
        return false
    } catch (_: SecurityException) {
        return false
    }
    return try {
        if (!process.waitFor(FILE_MANAGER_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            false
        } else {
            process.exitValue() == 0
        }
    } catch (_: InterruptedException) {
        process.destroyForcibly()
        Thread.currentThread().interrupt()
        false
    }
}

internal fun windowsFileManagerShowItemCommand(media: Path): List<String> =
    listOf("explorer.exe", "/select,", media.toString())

internal fun linuxFileManagerShowItemsCommand(media: Path): List<String> {
    val uri = media.toUri().toASCIIString()
    val quotedUri = uri.replace("\\", "\\\\").replace("'", "\\'")
    return listOf(
        "gdbus",
        "call",
        "--session",
        "--dest=org.freedesktop.FileManager1",
        "--object-path=/org/freedesktop/FileManager1",
        "--method=org.freedesktop.FileManager1.ShowItems",
        "['$quotedUri']",
        "",
    )
}

internal fun resolveVaultMediaPath(location: String?, vaultPath: String?): Path? {
    if (location == null || vaultPath == null) return null
    return try {
        val root = Path.of(vaultPath).toRealPath()
        val media = Path.of(location).toRealPath()
        media.takeIf {
            it.startsWith(root) && Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS)
        }
    } catch (_: IOException) {
        null
    } catch (_: InvalidPathException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

internal fun resolveVaultNotePath(notePath: String?, vaultPath: String?): Path? {
    if (notePath.isNullOrBlank() || vaultPath.isNullOrBlank()) return null
    return try {
        val root = Path.of(vaultPath).toRealPath()
        val relative = Path.of(notePath)
        if (relative.isAbsolute) return null
        val candidate = root.resolve(relative).normalize()
        if (!candidate.startsWith(root)) return null
        val note = candidate.toRealPath()
        note.takeIf {
            it.startsWith(root) && Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS)
        }
    } catch (_: IOException) {
        null
    } catch (_: InvalidPathException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

private const val FILE_MANAGER_TIMEOUT_SECONDS = 10L

@Composable
private fun DesktopWelcome(
    loading: Boolean,
    loadingMessage: String?,
    error: String?,
    systemAppearance: DesktopSystemAppearance,
    onChooseVault: () -> Unit,
) {
    KaedeGalleryTheme(
        appearance = AppearanceSettings(),
        dynamicColorScheme = desktopSystemColorScheme(
            systemAppearance.accentColor,
            systemAppearance.darkTheme ?: isSystemInDarkTheme(),
        ),
        systemDarkTheme = systemAppearance.darkTheme,
    ) {
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
                    Spacer(Modifier.height(8.dp))
                    Text(loadingMessage ?: "Loading...")
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
    var activeVideoPanel by remember { mutableStateOf<MpvVideoPanel?>(null) }
    DisposableEffect(videoPanel, mediaState.media.id) {
        onDispose {
            videoPanel.getAndSet(null)?.close()
            activeVideoPanel = null
        }
    }
    if (mediaState.media.isVideo) {
        Column(modifier = Modifier.fillMaxSize()) {
            SwingPanel(
                factory = {
                    MpvVideoPanel().also {
                        videoPanel.set(it)
                        activeVideoPanel = it
                    }
                },
                update = { panel -> panel.play(mediaState.location) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            val panel = activeVideoPanel
            if (panel == null) {
                Text(
                    "Starting video player…",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                val playbackState by panel.playbackState.collectAsState()
                if (MpvVideoCommand.supportsInAppControls()) {
                    DesktopVideoControls(
                        state = playbackState,
                        onPlayPause = panel::togglePlayback,
                        onSeek = panel::seekTo,
                        onSpeed = panel::setPlaybackSpeed,
                        onLoop = panel::setLooping,
                        onMute = panel::setMuted,
                    )
                }
            }
        }
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
            contentScale = ContentScale.Inside,
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
private fun DesktopVideoControls(
    state: DesktopVideoPlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeed: (Float) -> Unit,
    onLoop: (Boolean) -> Unit,
    onMute: (Boolean) -> Unit,
) {
    var seekPreview by remember { mutableStateOf<Float?>(null) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    val duration = state.durationSeconds
    val position = seekPreview ?: state.positionSeconds
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Slider(
                value = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f,
                onValueChange = { fraction ->
                    seekPreview = fraction * duration
                },
                onValueChangeFinished = {
                    seekPreview?.let(onSeek)
                    seekPreview = null
                },
                enabled = state.connected && duration > 0f,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = onPlayPause,
                    enabled = state.connected,
                ) {
                    Icon(
                        imageVector = if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.playing) "Pause video" else "Play video",
                    )
                }
                Text(
                    "${formatDesktopVideoDuration(position)} / ${formatDesktopVideoDuration(duration)}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(
                        onClick = { speedMenuExpanded = true },
                        enabled = state.connected,
                    ) {
                        Text("${state.playbackSpeed}×")
                    }
                    DropdownMenu(
                        expanded = speedMenuExpanded,
                        onDismissRequest = { speedMenuExpanded = false },
                    ) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}×") },
                                onClick = {
                                    speedMenuExpanded = false
                                    onSpeed(speed)
                                },
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { onLoop(!state.looping) },
                    enabled = state.connected,
                ) {
                    Icon(
                        Icons.Filled.RepeatOne,
                        contentDescription = if (state.looping) "Turn looping off" else "Loop video",
                        tint = if (state.looping) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(
                    onClick = { onMute(!state.muted) },
                    enabled = state.connected,
                ) {
                    Icon(
                        imageVector = if (state.muted) {
                            Icons.Filled.VolumeOff
                        } else {
                            Icons.Filled.VolumeUp
                        },
                        contentDescription = if (state.muted) "Unmute video" else "Mute video",
                    )
                }
            }
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
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
            modifier = Modifier.fillMaxSize(),
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

private suspend fun chooseVaultDirectory(): String? {
    if (isLinuxDesktop()) {
        try {
            return withContext(Dispatchers.IO) {
                LinuxPortalDirectoryPicker.chooseDirectory("Select an existing Vault")
            }?.toString()
        } catch (failure: IOException) {
            if (!shouldUseSwingFallback(failure)) throw failure
            val reason = failure.message ?: "System folder picker failed."
            return chooseVaultDirectoryWithSwing("$reason Select an existing Vault")
        }
    }
    return chooseVaultDirectoryWithSwing()
}

private fun chooseVaultDirectoryWithSwing(
    dialogTitle: String = "Select an existing Vault",
): String? {
    val chooser = JFileChooser(FileSystemView.getFileSystemView().homeDirectory).apply {
        this.dialogTitle = dialogTitle
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
