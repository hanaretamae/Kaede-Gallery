@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.hanaretamae.kaede.desktop

import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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
import com.hanaretamae.kaede.core.rust.ThumbnailLoadPriority
import com.hanaretamae.kaede.core.settings.LanguagePreference
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.core.settings.GalleryTagPrefixesCodec
import com.hanaretamae.kaede.core.settings.GalleryTagCategoryCodec
import com.hanaretamae.kaede.core.settings.SettingsRepository
import com.hanaretamae.kaede.core.settings.SettingsStateHolder
import com.hanaretamae.kaede.core.settings.platformSystemLanguagePreference
import com.hanaretamae.kaede.core.settings.resolve
import com.hanaretamae.kaede.ui.gallery.GalleryViewerMediaState
import com.hanaretamae.kaede.ui.gallery.KaedeTooltip
import com.hanaretamae.kaede.ui.gallery.KaedeGalleryApp
import com.hanaretamae.kaede.ui.gallery.KaedeGalleryTheme
import com.hanaretamae.kaede.ui.gallery.GallerySettingsScreen
import com.hanaretamae.kaede.ui.gallery.EnglishGallerySettingsStrings
import com.hanaretamae.kaede.ui.gallery.JapaneseGallerySettingsStrings
import com.hanaretamae.kaede.ui.gallery.GalleryPlaybackActions
import com.hanaretamae.kaede.ui.gallery.GalleryPlaybackStrings
import com.hanaretamae.kaede.ui.gallery.EnglishGalleryPlaybackStrings
import com.hanaretamae.kaede.ui.gallery.GalleryPlaybackSpeedChoices
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.hanaretamae.kaede.ui.gallery.GalleryLoadingScreen

import java.awt.Desktop
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

private const val DESKTOP_VIEWER_IMAGE_SIZE = 4_096
private const val DESKTOP_GALLERY_THUMBNAIL_SIZE = 512
internal val DESKTOP_MINIMUM_WINDOW_SIZE = DpSize(400.dp, 500.dp)
private const val DESKTOP_LAYOUT_DEBUG_PROPERTY = "kaede.debugLayout"
private const val PORTAL_REFRESH_INTERVAL_MILLIS = 15_000L

fun main() {
    System.setProperty("awtAppClassName", "com.hanaretamae.kaede")
    if (System.getProperty("os.name").startsWith("Linux", ignoreCase = true)) {
        System.setProperty("java.awt.im.style", "on-the-spot")
    }
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
            var wasFullscreen by remember { mutableStateOf(false) }
            LaunchedEffect(windowState.placement) {
                val fullscreen = windowState.placement == WindowPlacement.Fullscreen
                if (wasFullscreen && !fullscreen && isWindowsDesktop()) {
                    // Windows leaves a stale layout after leaving fullscreen; a 1px resize forces a relayout.
                    kotlinx.coroutines.delay(200)
                    val size = window.size
                    window.setSize(size.width + 1, size.height)
                    kotlinx.coroutines.delay(50)
                    window.setSize(size.width, size.height)
                    window.revalidate()
                    window.repaint()
                }
                wasFullscreen = fullscreen
            }
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
                DesktopApplication(windowState)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DesktopApplication(
    windowState: WindowState,
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
    val rootSettingsStateHolder = remember(settingsRepository, scope) {
        SettingsStateHolder(settingsRepository, scope)
    }
    val rootSettingsState by rootSettingsStateHolder.state.collectAsState()
    LaunchedEffect(rootSettingsStateHolder) { rootSettingsStateHolder.load() }
    DisposableEffect(rootSettingsStateHolder) {
        onDispose(rootSettingsStateHolder::dispose)
    }
    var session by remember { mutableStateOf<GallerySessionHandle?>(null) }
    var vaultPath by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var vaultSelectionResolved by remember { mutableStateOf(false) }
    var initialScanRequired by remember { mutableStateOf(false) }
    var openingVaultName by remember { mutableStateOf<String?>(null) }
    var startupAppearance by remember { mutableStateOf(AppearanceSettings()) }
    var showWelcomeExample by remember { mutableStateOf(false) }

    suspend fun openVault(path: String, performScan: Boolean = true, force: Boolean = false) {
        if (!force && path == vaultPath && session != null) return
        loading = true
        openingVaultName = try {
            desktopVaultName(path)
        } catch (_: InvalidPathException) {
            loading = false
            error = errorText(RepositoryError.INVALID_VAULT)
            vaultSelectionResolved = true
            return
        }
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
        startupAppearance = gallerySettings.appearance
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
                performScan = false,
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
                    initialScanRequired = performScan
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
                vaultSelectionResolved = true
            }
            is RepositoryResult.Success -> {
                val selectedPath = selected.value
                if (selectedPath == null) {
                    loading = false
                    vaultSelectionResolved = true
                } else {
                    val indexExists = Path.of(privateData, "gallery.sqlite").toFile().isFile
                    openVault(selectedPath, performScan = !indexExists)
                    vaultSelectionResolved = true
                }
            }
        }
    }

    val activeSession = session
    DisposableEffect(activeSession) {
        onDispose { activeSession?.close() }
    }

    if (activeSession == null || vaultPath == null) {
        if (!vaultSelectionResolved || loading ||
            (rootSettingsState.loading && rootSettingsState.settings == null)
        ) {
            DesktopStartupLoading(
                openingVaultName,
                rootSettingsState.settings?.appearance ?: startupAppearance,
                portalAppearance,
            )
        } else if (showWelcomeExample) {
            val appearance = rootSettingsState.settings?.appearance ?: startupAppearance
            val darkTheme = when (appearance.theme) {
                com.hanaretamae.kaede.core.settings.ThemePreference.SYSTEM ->
                    portalAppearance.darkTheme ?: isSystemInDarkTheme()
                com.hanaretamae.kaede.core.settings.ThemePreference.LIGHT -> false
                com.hanaretamae.kaede.core.settings.ThemePreference.DARK -> true
            }
            val activeSettingsLanguage = (
                rootSettingsState.settings?.appearance?.language ?: LanguagePreference.SYSTEM
                )
                .resolve(platformSystemLanguagePreference())
            KaedeGalleryTheme(
                appearance = appearance,
                dynamicColorScheme = desktopSystemColorScheme(
                    portalAppearance.accentColor,
                    darkTheme,
                ),
                systemDarkTheme = portalAppearance.darkTheme,
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
                    loadLicenseText = ::loadDesktopLicenseText,
                    openExampleOnStart = true,
                    exampleOnly = true,
                )
            }
        } else {
            DesktopWelcome(
                loading = loading,
                error = error,
                systemAppearance = portalAppearance,
                appearance = rootSettingsState.settings?.appearance ?: startupAppearance,
                onOpenExample = { showWelcomeExample = true },
                onChooseVault = {
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            val selectedPath = chooseVaultDirectory()
                            if (selectedPath == null) {
                                loading = false
                                error = "No folder was selected. Use Open or Select to confirm the folder."
                            } else {
                                openVault(selectedPath)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            loading = false
                            error = failure.message
                                ?.takeIf { failure is PortalSelectionException }
                                ?: "The selected folder could not be opened."
                        }
                    }
                },
            )
        }
    } else {
        val selectedVaultPath = requireNotNull(vaultPath)
        KaedeGalleryApp(
            repository = activeSession.gallery,
            settingsRepository = settingsRepository,
            settingsStateHolder = rootSettingsStateHolder,
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
            vaultName = desktopVaultName(selectedVaultPath),
            scanWarningCount = activeSession.initialScan.warnings.coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt(),
            rescanOnStart = initialScanRequired,
            galleryThumbnail = { mediaId, isVideo, isVisible ->
                DesktopGalleryThumbnail(activeSession.gallery, mediaId, isVideo, isVisible)
            },
            mediaContent = { mediaState ->
                DesktopMediaContent(
                    repository = activeSession.gallery,
                    mediaState = mediaState,
                )
            },
            onViewerFullscreenChanged = {},
            viewerWindowFullscreen = windowState.placement == WindowPlacement.Fullscreen,
            onViewerWindowFullscreenChanged = { fullscreen ->
                windowState.placement = if (fullscreen) {
                    WindowPlacement.Fullscreen
                } else {
                    WindowPlacement.Floating
                }
            },
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
                scope.launch { importSettingsFile(onImported) }
            },
            onExportSettings = { content ->
                scope.launch { exportSettingsFile(content, vaultPath) }
            },
            onRescan = {
                activeSession.rescan().also { initialScanRequired = false }
            },
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
                    try {
                        chooseVaultDirectory()?.let { openVault(it) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        showVaultDirectoryPickerError()
                    }
                }
            },
            onForgetVault = {
                val selectedPath = vaultPath
                session?.close()
                // Clearing the session here would cancel the settings coroutine during cleanup.
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
                                force = true,
                            )
                            RepositoryResult.Failure(forgotten.error)
                        }
                        is RepositoryResult.Success -> {
                            session = null
                            vaultPath = null
                            error = null
                            loading = false
                            vaultSelectionResolved = true
                            initialScanRequired = false
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
    if (uri != null && isWindowsDesktop()) return launchWindowsProtocol(uri)
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

// Hands the URI straight to the registered protocol handler instead of the browser.
private fun launchWindowsProtocol(uri: URI): Boolean = try {
    ProcessBuilder("rundll32.exe", "url.dll,FileProtocolHandler", uri.toASCIIString())
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    true
} catch (_: IOException) {
    false
} catch (_: SecurityException) {
    false
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

internal fun isWindowsDesktop(): Boolean =
    System.getProperty("os.name").lowercase(java.util.Locale.ROOT).contains("windows")

private fun isGnomeDesktop(): Boolean =
    isLinuxDesktop() &&
        (System.getenv("XDG_CURRENT_DESKTOP") ?: "")
            .uppercase(java.util.Locale.ROOT).contains("GNOME")

private fun showInWindowsFileManager(media: Path): Boolean {
    return try {
        selectInWindowsExplorer(media)
    } catch (_: LinkageError) {
        false
    } catch (_: SecurityException) {
        false
    }
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
        val root = desktopPathOf(vaultPath).toRealPath()
        val media = desktopPathOf(location).toRealPath()
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
        val root = desktopPathOf(vaultPath).toRealPath()
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
    error: String?,
    systemAppearance: DesktopSystemAppearance,
    appearance: AppearanceSettings,
    onChooseVault: () -> Unit,
    onOpenExample: () -> Unit,
) {
    val japanese = appearance.language.resolve(platformSystemLanguagePreference()) ==
        LanguagePreference.JAPANESE
    val darkTheme = when (appearance.theme) {
        com.hanaretamae.kaede.core.settings.ThemePreference.SYSTEM ->
            systemAppearance.darkTheme ?: isSystemInDarkTheme()
        com.hanaretamae.kaede.core.settings.ThemePreference.LIGHT -> false
        com.hanaretamae.kaede.core.settings.ThemePreference.DARK -> true
    }
    KaedeGalleryTheme(
        appearance = appearance,
        dynamicColorScheme = desktopSystemColorScheme(
            systemAppearance.accentColor,
            darkTheme,
        ),
        systemDarkTheme = systemAppearance.darkTheme,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 2.dp,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
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
                                    "Choose an existing, read-only Vault folder to begin."
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
                            if (error != null) {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    if (japanese) {
                                        "Vaultを読み込めませんでした。フォルダーへのアクセス権限を確認してください。"
                                    } else {
                                        error
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
}

@Composable
private fun DesktopStartupLoading(
    vaultName: String?,
    appearance: AppearanceSettings,
    systemAppearance: DesktopSystemAppearance,
) {
    val darkTheme = when (appearance.theme) {
        com.hanaretamae.kaede.core.settings.ThemePreference.SYSTEM ->
            systemAppearance.darkTheme ?: isSystemInDarkTheme()
        com.hanaretamae.kaede.core.settings.ThemePreference.LIGHT -> false
        com.hanaretamae.kaede.core.settings.ThemePreference.DARK -> true
    }
    KaedeGalleryTheme(
        appearance = appearance,
        dynamicColorScheme = desktopSystemColorScheme(systemAppearance.accentColor, darkTheme),
        systemDarkTheme = systemAppearance.darkTheme,
    ) {
        GalleryLoadingScreen(vaultName)
    }
}

@Composable
internal fun DesktopMediaContent(
    repository: GalleryRepository,
    mediaState: GalleryViewerMediaState,
) {
    if (mediaState.media.isVideo && mediaState.locationLoading) {
        LoadingIndicator()
        return
    }
    if (mediaState.media.isVideo && mediaState.location == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(mediaState.unavailableLabel, color = Color.White)
        }
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
            if (panel != null) {
                DisposableEffect(panel, mediaState.fullscreen) {
                    val dispatcher = java.awt.KeyEventDispatcher { event ->
                        if (
                            mediaState.fullscreen &&
                            event.id == java.awt.event.KeyEvent.KEY_PRESSED &&
                            event.keyCode == java.awt.event.KeyEvent.VK_ESCAPE &&
                            javax.swing.SwingUtilities.getWindowAncestor(panel)?.isActive == true
                        ) {
                            mediaState.onToggleFullscreen?.invoke()
                            true
                        } else {
                            false
                        }
                    }
                    val keyboard = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    keyboard.addKeyEventDispatcher(dispatcher)
                    onDispose { keyboard.removeKeyEventDispatcher(dispatcher) }
                }
                val playbackState by panel.playbackState.collectAsState()
                if (MpvVideoCommand.supportsInAppControls()) {
                    var controlsVisible by remember(mediaState.fullscreen) { mutableStateOf(true) }
                    var controlsInteracting by remember { mutableStateOf(false) }
                    LaunchedEffect(
                        mediaState.fullscreen, playbackState.playing,
                        controlsVisible, controlsInteracting,
                    ) {
                        if (
                            mediaState.fullscreen && playbackState.playing &&
                            controlsVisible && !controlsInteracting
                        ) {
                            delay(3_000)
                            controlsVisible = false
                        }
                    }
                    AnimatedVisibility(
                        visible = controlsVisible || !mediaState.fullscreen,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        DesktopVideoControls(
                            state = playbackState,
                            playbackStrings = mediaState.playbackStrings,
                            fullscreen = mediaState.fullscreen,
                            onToggleFullscreen = mediaState.onToggleFullscreen,
                            onPlayPause = panel::togglePlayback,
                            onSeek = panel::seekTo,
                            onSpeed = panel::setPlaybackSpeed,
                            onLoop = panel::setLooping,
                            onMute = panel::setMuted,
                            onInteractionChanged = { controlsInteracting = it },
                            onPlaybackOverlayVisibilityChanged =
                                mediaState.onPlaybackOverlayVisibilityChanged,
                        )
                    }
                    if (mediaState.fullscreen && !controlsVisible) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            KaedeTooltip(mediaState.playbackStrings.showControls) {
                                FilledTonalIconButton(onClick = { controlsVisible = true }) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        mediaState.playbackStrings.showControls,
                                    )
                                }
                            }
                            mediaState.onToggleFullscreen?.let { toggle ->
                                DesktopFullscreenButton(
                                    fullscreen = true,
                                    onClick = toggle,
                                    fullScreenLabel = mediaState.playbackStrings.fullScreen,
                                    exitFullScreenLabel = mediaState.playbackStrings.exitFullScreen,
                                )
                            }
                        }
                    }
                } else {
                    mediaState.onToggleFullscreen?.let { toggle ->
                        DesktopFullscreenButton(
                            fullscreen = mediaState.fullscreen,
                            onClick = toggle,
                            modifier = Modifier.align(Alignment.End).padding(16.dp),
                            fullScreenLabel = mediaState.playbackStrings.fullScreen,
                            exitFullScreenLabel = mediaState.playbackStrings.exitFullScreen,
                        )
                    }
                }
            }
        }
        return
    }
    if (mediaState.locationLoading) {
        LoadingIndicator()
        return
    }
    val viewerImage = remember(repository, mediaState.media.id) {
        mutableStateOf(
            DesktopImageState(bitmap = DesktopImageMemoryCache.preview(mediaState.media.id)),
        )
    }
    val imageState by viewerImage
    LaunchedEffect(repository, mediaState.media.id, mediaState.location) {
        if (mediaState.location == null) {
            viewerImage.value = viewerImage.value.copy(error = mediaState.unavailableLabel)
            return@LaunchedEffect
        }
        val cachedImage = DesktopImageMemoryCache.get(
            mediaState.media.id,
            DESKTOP_VIEWER_IMAGE_SIZE,
        )
        if (cachedImage != null) {
            viewerImage.value = DesktopImageState(bitmap = cachedImage)
            return@LaunchedEffect
        }
        val preview = viewerImage.value.bitmap
        val loaded = DesktopImageMemoryCache.load(
            mediaState.media.id,
            DESKTOP_VIEWER_IMAGE_SIZE,
            ThumbnailLoadPriority.INTERACTIVE,
        ) {
            withContext(Dispatchers.IO) {
                val bitmap = decodeDesktopViewerImage(
                    mediaState.location,
                    DESKTOP_VIEWER_IMAGE_SIZE,
                )
                DesktopImageState(
                    bitmap = bitmap,
                    error = if (bitmap == null) mediaState.unavailableLabel else null,
                )
            }
        }
        viewerImage.value = if (loaded.bitmap != null) {
            loaded
        } else {
            loaded.copy(bitmap = preview)
        }
    }
    LaunchedEffect(repository, mediaState.media.id, mediaState.prefetchMediaIds) {
        delay(100)
        coroutineScope {
            mediaState.prefetchMediaIds
                .asSequence()
                .filter { it != mediaState.media.id }
                .map { mediaId ->
                    async {
                        DesktopImageMemoryCache.load(
                            mediaId,
                            DESKTOP_VIEWER_IMAGE_SIZE,
                            ThumbnailLoadPriority.PREFETCH,
                        ) {
                            withContext(Dispatchers.IO) {
                                when (val result = repository.mediaLocation(mediaId)) {
                                    is RepositoryResult.Failure ->
                                        DesktopImageState(error = errorText(result.error))
                                    is RepositoryResult.Success -> {
                                        val bitmap = decodeDesktopViewerImage(
                                            result.value,
                                            DESKTOP_VIEWER_IMAGE_SIZE,
                                        )
                                        DesktopImageState(
                                            bitmap = bitmap,
                                            error = if (bitmap == null) {
                                                "Image unavailable."
                                            } else {
                                                null
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                .toList()
                .awaitAll()
        }
    }

    when {
        imageState.bitmap != null -> Image(
            bitmap = requireNotNull(imageState.bitmap),
            contentDescription = null,
            contentScale = ContentScale.Inside,
            modifier = Modifier.fillMaxSize(),
        )
        imageState.error != null -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(mediaState.unavailableLabel, color = Color.White)
        }
        else -> LoadingIndicator()
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun DesktopVideoControls(
    state: DesktopVideoPlaybackState,
    playbackStrings: GalleryPlaybackStrings = EnglishGalleryPlaybackStrings,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeed: (Float) -> Unit,
    onLoop: (Boolean) -> Unit,
    onMute: (Boolean) -> Unit,
    fullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    onInteractionChanged: (Boolean) -> Unit = {},
    onPlaybackOverlayVisibilityChanged: ((Boolean) -> Unit)? = null,
) {
    var seekPreview by remember { mutableStateOf<Float?>(null) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    val controlsFocus = remember { FocusRequester() }
    val duration = state.durationSeconds
    val position = seekPreview ?: state.positionSeconds
    val sliderState = rememberSliderState(value = 0f)
    if (seekPreview == null) {
        sliderState.value = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
    }
    LaunchedEffect(speedMenuExpanded, seekPreview != null) {
        onInteractionChanged(speedMenuExpanded || seekPreview != null)
    }
    LaunchedEffect(speedMenuExpanded) {
        if (!speedMenuExpanded) controlsFocus.requestFocus()
    }
    DisposableEffect(Unit) {
        onDispose {
            onInteractionChanged(false)
            onPlaybackOverlayVisibilityChanged?.invoke(false)
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().focusRequester(controlsFocus).focusable()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AnimatedVisibility(
            visible = !speedMenuExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Slider(
                state = sliderState,
                onValueChange = { fraction ->
                    sliderState.value = fraction
                    val seconds = fraction * duration
                    seekPreview = seconds
                    onSeek(seconds)
                },
                onValueChangeFinished = {
                    seekPreview?.let(onSeek)
                    seekPreview = null
                },
                enabled = duration > 0f && state.connected,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            )
        }
        GalleryPlaybackActions(leading = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            KaedeTooltip(
                if (state.playing) playbackStrings.pauseVideo else playbackStrings.playVideo,
            ) {
                FilledIconButton(
                    shapes = IconButtonDefaults.shapes(),
                    onClick = onPlayPause,
                    enabled = state.connected,
                ) {
                    Icon(
                        imageVector = if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.playing) {
                            playbackStrings.pauseVideo
                        } else {
                            playbackStrings.playVideo
                        },
                    )
                }
            }
            DesktopPlaybackPill(
                "${formatDesktopVideoDuration(position)} / ${formatDesktopVideoDuration(duration)}",
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
            }
        }, trailing = {
            DesktopPlaybackPill(
                "${state.playbackSpeed}x",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = {
                    val expanded = !speedMenuExpanded
                    speedMenuExpanded = expanded
                    onPlaybackOverlayVisibilityChanged?.invoke(expanded)
                    Unit
                }.takeIf { state.connected },
                contentDescription = playbackStrings.playbackSpeed,
            )
            KaedeTooltip(
                if (state.looping) playbackStrings.turnLoopingOff
                else playbackStrings.loopVideo,
            ) {
                FilledTonalIconToggleButton(
                    checked = state.looping,
                    onCheckedChange = onLoop,
                    shapes = IconButtonDefaults.toggleableShapes(),
                    enabled = state.connected,
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Repeat,
                        contentDescription = if (state.looping) {
                            playbackStrings.turnLoopingOff
                        } else {
                            playbackStrings.loopVideo
                        },
                    )
                }
            }
            KaedeTooltip(
                if (state.muted) playbackStrings.unmuteVideo else playbackStrings.muteVideo,
            ) {
                FilledTonalIconToggleButton(
                    checked = !state.muted,
                    onCheckedChange = { onMute(!it) },
                    shapes = IconButtonDefaults.toggleableShapes(),
                    enabled = state.connected,
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = if (state.muted) {
                            Icons.Filled.VolumeOff
                        } else {
                            Icons.Filled.VolumeUp
                        },
                        contentDescription = if (state.muted) {
                            playbackStrings.unmuteVideo
                        } else {
                            playbackStrings.muteVideo
                        },
                    )
                }
            }
            onToggleFullscreen?.let { toggle ->
                DesktopFullscreenButton(
                    fullscreen = fullscreen,
                    onClick = toggle,
                    fullScreenLabel = playbackStrings.fullScreen,
                    exitFullScreenLabel = playbackStrings.exitFullScreen,
                )
            }
        })
        AnimatedVisibility(
            visible = speedMenuExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            GalleryPlaybackSpeedChoices(
                selected = state.playbackSpeed,
                onSelect = {
                    onSpeed(it)
                    speedMenuExpanded = false
                    onPlaybackOverlayVisibilityChanged?.invoke(false)
                },
            )
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

@Composable
private fun DesktopGalleryThumbnail(
    repository: GalleryRepository,
    mediaId: MediaId,
    isVideo: Boolean,
    isVisible: Boolean,
) {
    val thumbnailState = remember(repository, mediaId, isVideo) {
        mutableStateOf(
            DesktopImageState(bitmap = DesktopImageMemoryCache.get(mediaId, DESKTOP_GALLERY_THUMBNAIL_SIZE)),
        )
    }
    val imageState by thumbnailState
    LaunchedEffect(repository, mediaId, isVideo, isVisible) {
        if (!isVisible) return@LaunchedEffect
        if (thumbnailState.value.bitmap != null) return@LaunchedEffect
        thumbnailState.value = DesktopImageMemoryCache.load(
            mediaId,
            DESKTOP_GALLERY_THUMBNAIL_SIZE,
            ThumbnailLoadPriority.VISIBLE,
        ) {
            withContext(Dispatchers.IO) {
                val bytes = if (isVideo) {
                    when (val result = repository.mediaLocation(mediaId)) {
                        is RepositoryResult.Failure -> null
                        is RepositoryResult.Success ->
                            result.value?.let(DesktopVideoThumbnail::extract)
                    }
                } else {
                    when (
                        val result = repository.thumbnail(
                            mediaId,
                            DESKTOP_GALLERY_THUMBNAIL_SIZE,
                        )
                    ) {
                        is RepositoryResult.Failure -> null
                        is RepositoryResult.Success -> result.value
                    }
                }
                val bitmap = bytes?.let(::decodeDesktopImage)
                DesktopImageState(
                    bitmap = bitmap,
                    error = if (bitmap == null && isVideo) {
                        "Video thumbnail unavailable."
                    } else {
                        null
                    },
                )
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
    if (isWindowsDesktop()) {
        try {
            return withContext(Dispatchers.IO) { chooseWindowsDirectory("Select an existing Vault") }
        } catch (_: IOException) {
            return chooseVaultDirectoryWithSwing()
        }
    }
    return chooseVaultDirectoryWithSwing()
}

private suspend fun chooseVaultDirectoryWithSwing(
    dialogTitle: String = "Select an existing Vault",
): String? = withContext(Dispatchers.IO) {
    runOnAwtEventThread {
        val chooser = JFileChooser(FileSystemView.getFileSystemView().homeDirectory).apply {
            this.dialogTitle = dialogTitle
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.path
        } else {
            null
        }
    }
}

internal fun <T> runOnAwtEventThread(action: () -> T): T {
    if (EventQueue.isDispatchThread()) return action()
    val result = AtomicReference<Result<T>?>(null)
    EventQueue.invokeAndWait { result.set(runCatching(action)) }
    return requireNotNull(result.get()).getOrThrow()
}

private suspend fun showVaultDirectoryPickerError() {
    withContext(Dispatchers.IO) {
        runOnAwtEventThread {
            JOptionPane.showMessageDialog(
                null,
                "The folder picker could not be opened.",
                "Kaede Gallery",
                JOptionPane.ERROR_MESSAGE,
            )
        }
    }
}

private suspend fun importSettingsFile(onImported: (String) -> Unit) {
    try {
        val selected = chooseSettingsPath(save = false) ?: return
        onImported(withContext(Dispatchers.IO) { DesktopSettingsTransfer.read(selected) })
    } catch (_: IOException) {
        showSettingsTransferError()
    } catch (_: SecurityException) {
        showSettingsTransferError()
    } catch (_: IllegalArgumentException) {
        showSettingsTransferError()
    }
}

private suspend fun exportSettingsFile(content: String, vaultPath: String?) {
    try {
        val selected = chooseSettingsPath(
            save = true,
            fileName = "kaede-gallery-settings.kgsettings",
        ) ?: return
        val vault = vaultPath?.let(::desktopPathOf)
            ?: throw IOException("No Vault is selected.")
        withContext(Dispatchers.IO) {
            DesktopSettingsTransfer.write(selected, content, vault)
        }
    } catch (_: IOException) {
        showSettingsTransferError()
    } catch (_: SecurityException) {
        showSettingsTransferError()
    } catch (_: IllegalArgumentException) {
        showSettingsTransferError()
    }
}

private suspend fun chooseSettingsPath(save: Boolean, fileName: String? = null): Path? {
    if (isLinuxDesktop()) {
        try {
            return withContext(Dispatchers.IO) {
                if (save) {
                    LinuxPortalDirectoryPicker.saveFile(
                        "Export Kaede Gallery settings",
                        fileName ?: "kaede-gallery-settings.kgsettings",
                    )
                } else {
                    LinuxPortalDirectoryPicker.chooseFile("Import Kaede Gallery settings")
                }
            }
        } catch (failure: IOException) {
            if (!shouldUseSwingFallback(failure)) throw failure
        }
    }
    val chooser = FileDialog(
        null as java.awt.Frame?,
        "Kaede Gallery settings",
        if (save) FileDialog.SAVE else FileDialog.LOAD,
    ).apply {
        if (save) file = fileName ?: "kaede-gallery-settings.kgsettings"
    }
    chooser.isVisible = true
    val selectedFile = chooser.file ?: return null
    val selectedDirectory = chooser.directory ?: return null
    return Path.of(selectedDirectory, selectedFile)
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

@Composable
private fun DesktopFullscreenButton(
    fullscreen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullScreenLabel: String = "Maximize application window",
    exitFullScreenLabel: String = "Restore application window",
) {
    val label = if (fullscreen) exitFullScreenLabel else fullScreenLabel
    KaedeTooltip(label) {
        FilledTonalIconButton(
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            onClick = onClick,
            modifier = modifier,
        ) {
            Icon(
                if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = label,
            )
        }
    }
}

@Composable
private fun DesktopPlaybackPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
) {
    val semanticsModifier = contentDescription?.let {
        Modifier.semantics { this.contentDescription = it }
    } ?: Modifier
    val content: @Composable () -> Unit = {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
    if (onClick == null) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = semanticsModifier,
            content = content,
        )
    } else {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = semanticsModifier,
            content = content,
        )
    }
}
