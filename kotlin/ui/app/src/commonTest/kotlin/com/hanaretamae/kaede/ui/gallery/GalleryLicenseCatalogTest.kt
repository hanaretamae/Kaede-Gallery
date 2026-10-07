package com.hanaretamae.kaede.ui.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GalleryLicenseCatalogTest {
    @Test
    fun licenseCatalogUsesUniqueTitlesAndBundledAssets() {
        val documents = galleryLicenseDocuments(EnglishGallerySettingsStrings)

        assertEquals(3, documents.size)
        assertEquals(documents.size, documents.map { it.title }.toSet().size)
        assertEquals(
            setOf(
                "licenses/Apache-2.0.txt",
                "licenses/LGPL-2.1.txt",
                "licenses/RUST-DEPENDENCY-LICENSES.txt",
            ),
            documents.map { it.assetPath }.toSet(),
        )
        assertTrue(documents.all { it.title.isNotBlank() })
    }

    @Test
    fun licenseCatalogLabelsAreLocalized() {
        val japanese = galleryLicenseDocuments(JapaneseGallerySettingsStrings)

        assertEquals(3, japanese.size)
        assertTrue(japanese.all { it.title.isNotBlank() })
        assertTrue(japanese.any { it.title.contains("UniFFI") })
    }
}
