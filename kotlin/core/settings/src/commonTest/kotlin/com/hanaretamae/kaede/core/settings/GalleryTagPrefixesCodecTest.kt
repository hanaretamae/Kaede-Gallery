package com.hanaretamae.kaede.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GalleryTagPrefixesCodecTest {
    @Test
    fun storageCodecRoundTripsValidPrefixes() {
        val prefixes = listOf("source/art", "portfolio", "set/")
        assertEquals(prefixes, GalleryTagPrefixesCodec.decodeStorage(
            GalleryTagPrefixesCodec.encodeStorage(prefixes),
        ))
        assertEquals(emptyList(), GalleryTagPrefixesCodec.decodeStorage(""))
    }

    @Test
    fun rejectsUnsafeOrUnboundedPrefixes() {
        assertFalse(GalleryTagPrefixesCodec.isValid(listOf("source/", "source/")))
        assertFalse(GalleryTagPrefixesCodec.isValid(listOf(" spaced")))
        assertFalse(GalleryTagPrefixesCodec.isValid(listOf("source/\ninvalid")))
        assertFalse(
            GalleryTagPrefixesCodec.isValid(
                listOf("x".repeat(GalleryTagPrefixesCodec.MAX_PREFIX_LENGTH + 1)),
            ),
        )
        assertFalse(GalleryTagPrefixesCodec.isValid(listOf("あ".repeat(43))))
        assertFalse(GalleryTagPrefixesCodec.isValid(listOf("\uD800")))
        assertNull(GalleryTagPrefixesCodec.decodeStorage("01:a"))
        assertNull(GalleryTagPrefixesCodec.decodeStorage("2:a"))
    }

    @Test
    fun emitsValidRustSettingsJson() {
        assertEquals(
            """{"noteStructure":{"galleryTagPrefixes":["source/art","set/"]}}""",
            GalleryTagPrefixesCodec.encodeRustSettings(listOf("source/art", "set/")),
        )
    }
}
