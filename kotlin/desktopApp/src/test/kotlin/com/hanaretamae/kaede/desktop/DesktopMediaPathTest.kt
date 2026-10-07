package com.hanaretamae.kaede.desktop

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopMediaPathTest {
    @Test
    fun onlyResolvesExistingMediaInsideTheSelectedVault() {
        val vault = Files.createTempDirectory("kaede-media-vault")
        val outside = Files.createTempDirectory("kaede-media-outside")
        val media = Files.write(vault.resolve("fictional.png"), byteArrayOf(1))
        val externalMedia = Files.write(outside.resolve("fictional.png"), byteArrayOf(2))
        val externalLink = Files.createSymbolicLink(vault.resolve("external.png"), externalMedia)
        try {
            assertEquals(
                media.toRealPath(),
                resolveVaultMediaPath(media.toString(), vault.toString()),
            )
            assertNull(resolveVaultMediaPath(externalMedia.toString(), vault.toString()))
            assertNull(resolveVaultMediaPath(externalLink.toString(), vault.toString()))
            assertNull(resolveVaultMediaPath(vault.toString(), vault.toString()))
            assertNull(resolveVaultMediaPath(null, vault.toString()))
        } finally {
            Files.deleteIfExists(media)
            Files.deleteIfExists(externalMedia)
            Files.deleteIfExists(externalLink)
            Files.deleteIfExists(vault)
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun linuxFileManagerSelectsTheMediaUriWithoutShellInterpolation() {
        val vault = Files.createTempDirectory("kaede-media-vault")
        val media = Files.write(vault.resolve("image with 'quote'.png"), byteArrayOf(1))
        try {
            val command = linuxFileManagerShowItemsCommand(media)

            assertEquals("gdbus", command.first())
            assertEquals("--session", command[2])
            assertEquals("--dest=org.freedesktop.FileManager1", command[3])
            assertEquals(
                "--object-path=/org/freedesktop/FileManager1",
                command[4],
            )
            assertEquals(
                "--method=org.freedesktop.FileManager1.ShowItems",
                command[5],
            )
            val uri = media.toUri().toASCIIString()
                .replace("\\", "\\\\")
                .replace("'", "\\'")
            assertEquals("['$uri']", command[6])
            assertEquals("", command[7])
            assertEquals(8, command.size)
        } finally {
            Files.deleteIfExists(media)
            Files.deleteIfExists(vault)
        }
    }


    @Test
    fun windowsFileManagerSelectsTheExactMediaPathAsAnArgument() {
        val media = Path.of("C:\\fictional vault\\image with spaces.png")

        assertEquals(
            listOf("explorer.exe", "/select,", media.toString()),
            windowsFileManagerShowItemCommand(media),
        )
    }

    @Test
    fun obsidianNoteOpeningIsLimitedToExistingFilesInsideTheVault() {
        val vault = Files.createTempDirectory("kaede-note-vault")
        val outside = Files.createTempDirectory("kaede-note-outside")
        val note = Files.createDirectories(vault.resolve("notes"))
            .resolve("fictional note.md")
        Files.write(note, byteArrayOf(1))
        val externalNote = Files.write(outside.resolve("outside.md"), byteArrayOf(2))
        Files.createSymbolicLink(vault.resolve("linked.md"), externalNote)
        try {
            assertEquals(
                note.toRealPath(),
                resolveVaultNotePath("notes/fictional note.md", vault.toString()),
            )
            assertNull(
                resolveVaultNotePath("../${outside.fileName}/outside.md", vault.toString()),
            )
            assertNull(resolveVaultNotePath("linked.md", vault.toString()))
            assertNull(resolveVaultNotePath(externalNote.toString(), vault.toString()))
            assertNull(resolveVaultNotePath("missing.md", vault.toString()))
        } finally {
            Files.deleteIfExists(note)
            Files.deleteIfExists(vault.resolve("notes"))
            Files.deleteIfExists(vault.resolve("linked.md"))
            Files.deleteIfExists(externalNote)
            Files.deleteIfExists(vault)
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun obsidianUriEncodesTheCanonicalNotePath() {
        val note = Path.of("/fictional vault/notes/note & tag.md")
        val uri = requireNotNull(obsidianOpenUri(note))

        assertEquals("obsidian", uri.scheme)
        assertEquals("open", uri.host)
        assertEquals(
            note.toString(),
            URLDecoder.decode(uri.rawQuery.substringAfter("path="), StandardCharsets.UTF_8),
        )
    }

}
