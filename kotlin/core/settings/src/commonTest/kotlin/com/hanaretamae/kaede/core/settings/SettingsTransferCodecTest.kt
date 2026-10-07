package com.hanaretamae.kaede.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsTransferCodecTest {
    @Test
    fun exportsVersionOneFlutterJsonAndRoundTripsKmpSettings() {
        val defaultExport = SettingsTransferCodec.encode(GallerySettings())
        assertTrue(defaultExport.contains(""""prefix":"source/art","color":"#4dd0e1""""))
        assertTrue(defaultExport.contains(""""prefix":"source/gender/female","color":"#f59eee""""))
        assertTrue(defaultExport.contains(""""useSystemColor":false"""))
        val settings = GallerySettings(
            appearance = AppearanceSettings(
                theme = ThemePreference.DARK,
                language = LanguagePreference.JAPANESE,
                useSystemColor = false,
                pureBlack = true,
            ),
            pageSize = 90,
            galleryTagPrefixes = listOf("portfolio", "set/"),
            showMissingMediaIcon = true,
            showLoadedRange = false,
            showTilePosition = true,
            showCounts = false,
        )

        val encoded = SettingsTransferCodec.encode(settings)
        assertTrue(encoded.startsWith("""{"version":1,"appearance":"""))
        val decoded = assertNotNull(SettingsTransferCodec.decode(encoded))
        assertEquals(settings.appearance, decoded.appearance)
        assertEquals(settings.pageSize, decoded.pageSize)
        assertEquals(settings.galleryTagPrefixes, decoded.galleryTagPrefixes)
        assertEquals(settings.showMissingMediaIcon, decoded.showMissingMediaIcon)
        assertEquals(settings.showLoadedRange, decoded.showLoadedRange)
        assertEquals(settings.showTilePosition, decoded.showTilePosition)
        assertEquals(settings.showCounts, decoded.showCounts)
        assertEquals(encoded, SettingsTransferCodec.encode(decoded))
    }

    @Test
    fun readsOlderKmpExportsWithoutConfusingTileCountsAndLoadedRange() {
        val olderKmpExport = SettingsTransferCodec.encode(
            GallerySettings(showLoadedRange = true, showCounts = false),
        ).replace("\"showItemCount\":true", "\"showItemCount\":false")
            .replace(",\"showCounts\":false", "")

        val decoded = assertNotNull(SettingsTransferCodec.decode(olderKmpExport))

        assertTrue(decoded.showLoadedRange)
        assertFalse(decoded.showCounts)
    }

    @Test
    fun preservesFlutterSettingsThatKmpDoesNotExposeWhenImportingAndExporting() {
        val flutterJson = """
            {
              "version": 1,
              "appearance": {
                "brightness": "light",
                "useSystemColor": false,
                "pureBlack": false,
                "language": "en"
              },
              "tags": {
                "includedPrefixes": ["source"],
                "hiddenPrefixes": ["private"],
                "colors": [{"prefix": "source/art", "color": "#4dd0e1"}],
                "noteStructure": {
                  "galleryTagPrefixes": ["source/art"],
                  "memoHeadings": ["Memo"],
                  "frontmatter": {"titleKeys": ["name"]},
                  "blockOrder": ["media", "memo"]
                },
                "pagination": {
                  "pageSize": 48,
                  "showItemCount": false,
                  "showItemNumberOnTiles": true,
                  "showMissingMediaIcon": true
                },
                "tagCategories": {"categories": [{"name": "Art", "path": "source/art"}]}
              }
            }
        """.trimIndent()

        val imported = assertNotNull(SettingsTransferCodec.decode(flutterJson))
        assertEquals(ThemePreference.LIGHT, imported.appearance.theme)
        assertEquals(LanguagePreference.ENGLISH, imported.appearance.language)
        assertEquals(listOf("source/art"), imported.galleryTagPrefixes)
        assertEquals(48, imported.pageSize)
        assertEquals(false, imported.showLoadedRange)
        assertEquals(true, imported.showCounts)
        assertEquals(true, imported.showTilePosition)
        assertEquals(true, imported.showMissingMediaIcon)
        assertEquals(listOf("source"), imported.includedTagPrefixes)
        assertEquals(listOf("private"), imported.hiddenTagPrefixes)
        assertEquals(
            GalleryTagCategorySettings(
                categories = listOf(GalleryTagCategoryRule("Art", "source/art")),
            ),
            GalleryTagCategoryCodec.decodeSettings(
                imported.flutterTagSettingsJson,
                japaneseDefaults = false,
            ),
        )
        val exported = SettingsTransferCodec.encode(imported)
        assertTrue(exported.contains(""""showItemCount":false"""))
        assertTrue(exported.contains(""""hiddenPrefixes":["private"]"""))
        assertTrue(exported.contains(""""colors":[{"prefix":"source/art","color":"#4dd0e1"}]"""))
        assertTrue(exported.contains(""""titleKeys":["name"]"""))
        assertTrue(exported.contains(""""blockOrder":["media","memo"]"""))
        assertTrue(exported.contains(""""tagCategories":{"categories":[{"name":"Art","path":"source/art"}]}"""))

        val edited = imported.copy(
            galleryTagPrefixes = listOf("portfolio"),
            includedTagPrefixes = listOf("portfolio", "*"),
            hiddenTagPrefixes = listOf("secret"),
        )
        val editedJson = SettingsTransferCodec.encode(edited)
        assertTrue(editedJson.contains(""""galleryTagPrefixes":["portfolio"]"""))
        assertTrue(editedJson.contains(""""includedPrefixes":["portfolio","*"]"""))
        assertTrue(editedJson.contains(""""hiddenPrefixes":["secret"]"""))
        val decodedEdited = SettingsTransferCodec.decode(editedJson)
        assertEquals(listOf("portfolio"), decodedEdited?.galleryTagPrefixes)
        assertEquals(listOf("portfolio", "*"), decodedEdited?.includedTagPrefixes)
        assertEquals(listOf("secret"), decodedEdited?.hiddenTagPrefixes)
    }

    @Test
    fun rejectsUnsupportedOrMalformedVersionOneSettings() {
        val valid = SettingsTransferCodec.encode(GallerySettings())
        for (invalid in listOf(
            valid.replace("\"version\":1", "\"version\":2"),
            valid.replace("\"pageSize\":24", "\"pageSize\":0"),
            valid.replace("\"pageSize\":24", "\"pageSize\":\"24\""),
            valid.replace("\"showMissingMediaIcon\":false", "\"showMissingMediaIcon\":\"maybe\""),
            valid.replace("\"includedPrefixes\":[\"*\"]", "\"includedPrefixes\":[false]"),
            valid.replace(
                "\"prefix\":\"source/art\",\"color\":\"#4dd0e1\"",
                "\"prefix\":\"source/art\",\"color\":\"red\"",
            ),
            valid.replace(
                "\"tags\":{",
                "\"tags\":{\"tagCategories\":{\"categories\":[{\"name\":\"Source\",\"path\":\"source/*/invalid\"}]},",
            ),
            valid.replace(
                "\"galleryTagPrefixes\":[\"source/art\"]",
                "\"galleryTagPrefixes\":[\"source/art\"],\"linkResolution\":\"unknown\"",
            ),
            valid.replace(
                "\"galleryTagPrefixes\":[\"source/art\"]",
                "\"galleryTagPrefixes\":[\"source/art\"],\"frontmatter\":{\"tagsKeys\":[]}",
            ),
            valid.replace("\"language\":\"system\"", "\"language\":true"),
            valid.replace("\"showLoadedRange\":true", "\"showLoadedRange\":5"),
            valid + "x".repeat(SettingsTransferCodec.MAX_BYTES),
            """{"version":1,"appearance":{},"tags":{}}""",
        )) {
            assertNull(SettingsTransferCodec.decode(invalid))
        }
    }

    @Test
    fun acceptsDuplicateFlutterDisplayPrefixes() {
        val duplicated = SettingsTransferCodec.encode(GallerySettings())
            .replace("\"includedPrefixes\":[\"*\"]", "\"includedPrefixes\":[\"*\",\"*\"]")

        val decoded = assertNotNull(SettingsTransferCodec.decode(duplicated))
        assertEquals(listOf("*", "*"), decoded.includedTagPrefixes)
        assertTrue(
            SettingsTransferCodec.encode(decoded).contains("\"includedPrefixes\":[\"*\",\"*\"]"),
        )
    }

    @Test
    fun refusesExportsThatWouldExceedTheTransferBound() {
        val advancedSettings = """
            {"includedPrefixes":["*"],"hiddenPrefixes":[],"colors":[],"other":"${"x".repeat(13_000)}"}
        """.trimIndent()
        val repeatedPrefix = "a".repeat(GalleryTagPrefixesCodec.MAX_PREFIX_LENGTH)
        val settings = GallerySettings(
            includedTagPrefixes = List(GalleryTagPrefixesCodec.MAX_PREFIXES) {
                repeatedPrefix
            },
            hiddenTagPrefixes = List(GalleryTagPrefixesCodec.MAX_PREFIXES) {
                repeatedPrefix
            },
            flutterTagSettingsJson = advancedSettings,
        )

        assertFailsWith<IllegalArgumentException> {
            SettingsTransferCodec.encode(settings)
        }
        assertFailsWith<IllegalArgumentException> {
            SettingsTransferCodec.encode(GallerySettings(flutterTagSettingsJson = "{"))
        }
    }

    @Test
    fun readsLegacyTransfersAndRoundTripsCustomTagPrefixes() {
        val legacy = """
            kaede-gallery-settings=1
            theme=SYSTEM
            language=SYSTEM
            use_system_color=true
            pure_black=false
            page_size=24
            gallery_tag_prefixes=10:source/art
            show_loaded_range=true
            show_tile_position=false
            show_counts=true
        """.trimIndent()
        assertEquals(
            GallerySettings(
                appearance = AppearanceSettings(useSystemColor = true),
            ),
            SettingsTransferCodec.decode(legacy),
        )
        assertEquals(
            GallerySettings(galleryTagPrefixes = emptyList()),
            SettingsTransferCodec.decode(
                SettingsTransferCodec.encode(GallerySettings(galleryTagPrefixes = emptyList())),
            )?.copy(flutterTagSettingsJson = null),
        )
    }
}
