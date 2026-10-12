package com.hanaretamae.kaede.desktop

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopPathsTest {
    @Test
    fun stripsWindowsVerbatimPrefixes() {
        assertEquals("C:\\Vault\\a.png", stripWindowsVerbatimPrefix("\\\\?\\C:\\Vault\\a.png"))
        assertEquals("\\\\host\\share\\a.png", stripWindowsVerbatimPrefix("\\\\?\\UNC\\host\\share\\a.png"))
        assertEquals("/home/a.png", stripWindowsVerbatimPrefix("/home/a.png"))
    }

    @Test
    fun vaultNamesAcceptCanonicalWindowsPathsWithoutOpeningTheVault() {
        if (isWindowsDesktop()) {
            val drivePath = "\\\\?\\C:\\Fictional\\Fictional Gallery"
            assertTrue(desktopPathOf(drivePath).isAbsolute)
            assertEquals("Fictional Gallery", desktopVaultName(drivePath))
            assertEquals("Fictional Gallery", desktopVaultName("\\\\?\\UNC\\fictional-host\\share\\Fictional Gallery"))
        } else {
            assertEquals("Fictional Gallery", desktopVaultName("/fictional/Fictional Gallery"))
        }
    }

    @Test
    fun fallsBackToPathLookupWithoutBundledTool() {
        assertEquals("kaede-nonexistent-tool", desktopToolCommand("kaede-nonexistent-tool", "Linux"))
    }

    @Test
    fun windowsReleaseFindsToolsNextToLauncherEvenWithAnExternalJavaHome() {
        val directory = Files.createTempDirectory(Files.createDirectories(Path.of("build")), "fictional-video-")
        val bundle = Files.createDirectory(directory.resolve("Kaede Gallery 絵"))
        val tools = Files.createDirectory(bundle.resolve("tools"))
        val launcher = Files.createFile(bundle.resolve("Kaede-Gallery.exe"))
        val mpv = Files.createFile(tools.resolve("mpv.exe"))
        val ffmpeg = Files.createFile(tools.resolve("ffmpeg.exe"))
        try {
            assertEquals(
                mpv.toAbsolutePath().toString(),
                desktopToolCommand("mpv", "Windows 11", directory.toString(), launcher.toString()),
            )
            assertEquals(
                ffmpeg.toAbsolutePath().toString(),
                desktopToolCommand("ffmpeg", "Windows 11", directory.toString(), launcher.toString()),
            )
            assertEquals(
                "ffprobe",
                desktopToolCommand("ffprobe", "Windows 11", directory.toString(), launcher.toString()),
            )
        } finally {
            Files.deleteIfExists(ffmpeg)
            Files.deleteIfExists(mpv)
            Files.deleteIfExists(launcher)
            Files.deleteIfExists(tools)
            Files.deleteIfExists(bundle)
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun windowsReleaseFindsToolsBesidePackagedRuntimeWithoutLauncherProperty() {
        val bundle = Files.createTempDirectory(Files.createDirectories(Path.of("build")), "fictional-video-")
        val runtime = Files.createDirectory(bundle.resolve("runtime"))
        val tools = Files.createDirectory(bundle.resolve("tools"))
        val ffmpeg = Files.createFile(tools.resolve("ffmpeg.exe"))
        try {
            assertEquals(
                ffmpeg.toAbsolutePath().toString(),
                desktopToolCommand("ffmpeg", "Windows 11", runtime.toString(), null),
            )
            assertEquals("mpv", desktopToolCommand("mpv", "Windows 11", runtime.toString(), null))
        } finally {
            Files.deleteIfExists(ffmpeg)
            Files.deleteIfExists(tools)
            Files.deleteIfExists(runtime)
            Files.deleteIfExists(bundle)
        }
    }
}

class WindowsIntegrationTest {
    @Test
    fun parsesRegistryDwordAndAccent() {
        val output = "\r\nHKEY_CURRENT_USER\\x\r\n    AccentColor    REG_DWORD    0xffd77800\r\n"
        val value = parseWindowsRegistryDword(output)
        assertEquals(0xffd77800L, value)
        val accent = parseWindowsAccentColor(value)!!
        assertEquals(0x00, (accent.red * 255).toInt())
        assertEquals(0x78, (accent.green * 255).toInt())
        assertEquals(0xd7, (accent.blue * 255).toInt())
        assertEquals(null, parseWindowsRegistryDword("ERROR"))
    }

    @Test
    fun folderPickerUsesEncodedCommand() {
        val command = windowsFolderPickerCommand()
        assertEquals("powershell.exe", command.first())
        assertEquals("-EncodedCommand", command[command.size - 2])
    }
}
