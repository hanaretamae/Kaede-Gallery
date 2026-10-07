package com.hanaretamae.kaede.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryTagCategoryCodecTest {
    @Test
    fun decodesFlutterCategoryRulesAndLocalizedDefaults() {
        val tags = """
            {"tagCategories":{"categories":[
              {"name":"Art","path":"source/art"},
              {"name":"Source","path":"source/*","splitDeep":true}
            ],"other":{"enabled":false,"name":"Other tags","splitDeep":true}}}
        """.trimIndent()

        assertEquals(
            GalleryTagCategorySettings(
                categories = listOf(
                    GalleryTagCategoryRule("Art", "source/art"),
                    GalleryTagCategoryRule("Source", "source/*", true),
                ),
                other = GalleryOtherCategorySettings(false, "Other tags", true),
            ),
            GalleryTagCategoryCodec.decodeSettings(tags, japaneseDefaults = false),
        )
        assertEquals(
            GalleryTagCategoryCodec.DEFAULT_CATEGORIES_JA,
            GalleryTagCategoryCodec.decodeSettings(null, japaneseDefaults = true).categories,
        )
        assertEquals(
            GalleryTagCategoryCodec.DEFAULT_CATEGORIES_EN,
            GalleryTagCategoryCodec.decodeSettings(null, japaneseDefaults = false).categories,
        )
    }

    @Test
    fun updatesTransferSettingsAndBuildsRustSettingsWithoutDroppingStructure() {
        val original = """
            {"includedPrefixes":["*"],"hiddenPrefixes":[],"colors":[],
            "noteStructure":{"memoHeadings":["Notes"]},"custom":"kept"}
        """.trimIndent()
        val categories = GalleryTagCategorySettings(
            categories = listOf(GalleryTagCategoryRule("Portfolio", "portfolio/*", true)),
            other = GalleryOtherCategorySettings(false, "Unmatched"),
        )
        val updated = GalleryTagCategoryCodec.update(original, categories)
        val rustSettings = GalleryTagCategoryCodec.encodeRustSettings(
            listOf("portfolio"),
            updated,
        )

        assertEquals(categories, GalleryTagCategoryCodec.decodeSettings(updated, false))
        assertTrue(updated.contains(""""custom":"kept""""))
        assertTrue(rustSettings.contains(""""galleryTagPrefixes":["portfolio"]"""))
        assertTrue(rustSettings.contains(""""memoHeadings":["Notes"]"""))
        assertTrue(rustSettings.contains(""""name":"Portfolio","path":"portfolio/*","splitDeep":true"""))
        assertFalse(rustSettings.contains(""""custom":"kept""""))
    }

    @Test
    fun rejectsInvalidPathsAndOversizedCategoryLists() {
        assertFalse(
            GalleryTagCategoryCodec.isValid(
                GalleryTagCategorySettings(
                    categories = listOf(GalleryTagCategoryRule("Bad", "source/*/extra")),
                ),
            ),
        )
        assertFalse(
            GalleryTagCategoryCodec.isValid(
                GalleryTagCategorySettings(
                    categories = List(GalleryTagCategoryCodec.MAX_CATEGORIES + 1) {
                        GalleryTagCategoryRule("Source", "source/*")
                    },
                ),
            ),
        )
    }
}
