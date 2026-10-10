package com.hanaretamae.kaede.desktop

import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.core.settings.GallerySettings
import com.hanaretamae.kaede.core.settings.LanguagePreference
import com.hanaretamae.kaede.core.settings.SettingsTransferCodec
import com.hanaretamae.kaede.core.settings.ThemePreference
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class DesktopSettingsRepositoryTest {
    @Test
    fun missingSettingsFileUsesDefaults() = runTest {
        val directory = Files.createTempDirectory("kaede-settings-defaults-test")
        try {
            val repository = DesktopSettingsRepository(directory.resolve("settings.properties"))

            assertEquals(
                RepositoryResult.Success(GallerySettings()),
                repository.load(),
            )
        } finally {
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun missingSettingsParentUsesDefaultsWithoutCreatingIt() = runTest {
        val directory = Files.createTempDirectory("kaede-settings-missing-parent-test")
        val parent = directory.resolve("private-data")
        try {
            val repository = DesktopSettingsRepository(parent.resolve("settings.properties"))

            assertEquals(
                RepositoryResult.Success(GallerySettings()),
                repository.load(),
            )
            assertFalse(Files.exists(parent))
        } finally {
            Files.deleteIfExists(parent)
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun persistsSettingsAtomicallyInPrivateDataDirectory() = runTest {
        val directory = Files.createTempDirectory("kaede-settings-test")
        val settingsFile = directory.resolve("settings.properties")
        try {
            val repository = DesktopSettingsRepository(settingsFile)
            val expected = GallerySettings(
                appearance = AppearanceSettings(
                    theme = ThemePreference.DARK,
                    language = LanguagePreference.JAPANESE,
                    useSystemColor = false,
                    pureBlack = true,
                ),
                pageSize = 72,
                fixedColumnCount = 4,
                galleryTagPrefixes = listOf("collection/", "portfolio"),
                includedTagPrefixes = listOf("source"),
                hiddenTagPrefixes = listOf("private"),
                showMissingMediaIcon = true,
                showLoadedRange = false,
                showTilePosition = true,
                showCounts = false,
                flutterTagSettingsJson =
                    """{"includedPrefixes":["source"],"hiddenPrefixes":["private"],"colors":[]}""",
            )

            assertIs<RepositoryResult.Success<Unit>>(repository.save(expected))
            assertEquals(expected, assertIs<RepositoryResult.Success<GallerySettings>>(
                repository.load(),
            ).value)
            assertEquals(listOf("settings.properties"), Files.list(directory).use { paths ->
                paths.map { it.fileName.toString() }.sorted().toList()
            })
        } finally {
            Files.deleteIfExists(settingsFile)
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun rejectsMalformedSettingsInsteadOfSilentlyUsingDefaults() = runTest {
        val directory = Files.createTempDirectory("kaede-settings-invalid-test")
        val settingsFile = directory.resolve("settings.properties")
        try {
            val repository = DesktopSettingsRepository(settingsFile)

            for (invalidProperties in listOf(
                "page_size=not-a-number\n",
                "page_size=0\n",
                "show_counts=maybe\n",
            )) {
                Files.writeString(settingsFile, invalidProperties)
                assertEquals(
                    RepositoryResult.Failure(RepositoryError.OPERATION_FAILED),
                    repository.load(),
                )
            }
        } finally {
            Files.deleteIfExists(settingsFile)
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun rejectsSymlinkedSettingsFileWithoutReadingOrReplacingItsTarget() = runTest {
        val directory = Files.createTempDirectory("kaede-settings-link-test")
        val outside = Files.createTempFile("kaede-settings-target", ".properties")
        val original = "private-target=unchanged\n"
        val settingsFile = directory.resolve("settings.properties")
        try {
            Files.writeString(outside, original)
            Files.createSymbolicLink(settingsFile, outside)
            val repository = DesktopSettingsRepository(settingsFile)

            assertEquals(
                RepositoryResult.Failure(RepositoryError.OPERATION_FAILED),
                repository.load(),
            )
            assertEquals(
                RepositoryResult.Failure(RepositoryError.OPERATION_FAILED),
                repository.save(GallerySettings()),
            )
            assertEquals(original, Files.readString(outside))
        } finally {
            Files.deleteIfExists(settingsFile)
            Files.deleteIfExists(directory)
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun reportsUnavailableStorageWhenSettingsParentIsNotDirectory() = runTest {
        val parentFile = Files.createTempFile("kaede-settings-parent-test", ".tmp")
        try {
            val repository = DesktopSettingsRepository(parentFile.resolve("settings.properties"))

            assertEquals(
                RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE),
                repository.load(),
            )
            assertEquals(
                RepositoryResult.Failure(RepositoryError.STORAGE_UNAVAILABLE),
                repository.save(GallerySettings()),
            )
        } finally {
            Files.deleteIfExists(parentFile)
        }
    }

    @Test
    fun transfersSettingsOnlyOutsideTheSelectedVault() {
        val directory = Files.createTempDirectory("kaede-settings-transfer-test")
        val vault = Files.createDirectory(directory.resolve("vault"))
        val outside = directory.resolve("backup.kgsettings")
        val insideVault = vault.resolve("backup.kgsettings")
        val content = SettingsTransferCodec.encode(GallerySettings())
        try {
            DesktopSettingsTransfer.write(outside, content, vault)
            assertEquals(content, DesktopSettingsTransfer.read(outside))
            assertEquals(
                listOf("backup.kgsettings"),
                Files.list(directory).use { paths ->
                    paths.filter { it.fileName.toString().endsWith(".kgsettings") }
                        .map { it.fileName.toString() }
                        .toList()
                },
            )

            assertFailsWith<IllegalArgumentException> {
                DesktopSettingsTransfer.write(insideVault, content, vault)
            }
            assertTrue(!Files.exists(insideVault))
        } finally {
            Files.deleteIfExists(outside)
            Files.deleteIfExists(vault)
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun boundedSettingsImportRejectsOversizedFiles() {
        val file = Files.createTempFile("kaede-settings-overflow", ".kgsettings")
        try {
            Files.writeString(file, "x".repeat(16 * 1024 + 1))
            assertFailsWith<IllegalArgumentException> {
                DesktopSettingsTransfer.read(file)
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
