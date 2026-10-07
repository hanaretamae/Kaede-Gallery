package com.hanaretamae.kaede.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GalleryTagColorCodecTest {
    @Test
    fun decodesFlutterColorsAndUsesTheMostSpecificPrefix() {
        val rules = GalleryTagColorCodec.decodeRules(
            """{"colors":[{"prefix":"source","color":"#112233"},{"prefix":"source/art","color":"#aabbcc"}]}""",
        )

        assertEquals(0xffaabbcc.toInt(), GalleryTagColorCodec.colorFor("source/art/style", rules))
        assertEquals(0xff112233.toInt(), GalleryTagColorCodec.colorFor("source/count", rules))
        assertEquals(GalleryTagColorCodec.DEFAULT_RULES, GalleryTagColorCodec.decodeRules(null))
    }

    @Test
    fun updatesColorsWithoutDroppingUnknownFlutterSettings() {
        val tags = """
            {"includedPrefixes":["*"],"hiddenPrefixes":["private"],"colors":[],
            "tagCategories":{"categories":[{"name":"Art","path":"source/art"}]}}
        """.trimIndent()
        val updated = GalleryTagColorCodec.update(
            tags,
            listOf(GalleryTagColorRule("source/art", 0xff123456.toInt())),
        )

        assertTrue(updated.contains(""""tagCategories":{"categories":[{"name":"Art","path":"source/art"}]}"""))
        assertEquals(
            listOf(GalleryTagColorRule("source/art", 0xff123456.toInt())),
            GalleryTagColorCodec.decodeRules(updated),
        )
    }

    @Test
    fun rejectsDuplicateAndMalformedRules() {
        assertFalse(
            GalleryTagColorCodec.isValid(
                listOf(
                    GalleryTagColorRule("source", 0xff112233.toInt()),
                    GalleryTagColorRule("source", 0xff445566.toInt()),
                ),
            ),
        )
        assertNull(
            SettingsTransferCodec.decode(
                SettingsTransferCodec.encode(GallerySettings())
                    .replace("#4dd0e1", "#xyzxyz"),
            ),
        )
    }
}
