package com.hanaretamae.kaede.ui.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GalleryLicenseCatalogTest {
    @Test
    fun licenseCatalogUsesUniqueTitlesAndBundledAssets() {
        val documents = galleryLicenseDocuments(EnglishGallerySettingsStrings)

        assertEquals(8, documents.size)
        assertEquals(documents.size, documents.map { it.title }.toSet().size)
        assertEquals(
            setOf(
                "licenses/Kaede-Gallery-MIT.txt",
                "licenses/THIRD_PARTY_NOTICES.md",
                "licenses/Runtime-LICENSES.txt",
                "licenses/Apache-2.0.txt",
                "licenses/MaterialKolor-MIT.txt",
                "licenses/GoogleSansFlex-OFL.txt",
                "licenses/LGPL-2.1.txt",
                "licenses/RUST-DEPENDENCY-LICENSES.txt",
            ),
            documents.map { it.assetPath }.toSet(),
        )
        assertTrue(documents.all { it.title.isNotBlank() })
        assertTrue(documents.single { it.title.contains("Apache") }.title.contains("Skydoves"))
    }

    @Test
    fun licenseCatalogLabelsAreLocalized() {
        val japanese = galleryLicenseDocuments(JapaneseGallerySettingsStrings)

        assertEquals(8, japanese.size)
        assertTrue(japanese.all { it.title.isNotBlank() })
        assertTrue(japanese.any { it.title.contains("UniFFI") })
        assertTrue(japanese.any { it.title.contains("Google Sans Flex") })
        assertTrue(japanese.single { it.title.contains("Apache") }.title.contains("Skydoves"))
    }
}
