package com.hanaretamae.kaede.core.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class GalleryTagDisplayPrefixesCodecTest {
    @Test
    fun displayRulesIncludeMatchingDescendantsAndApplyHiddenPrefixesLast() {
        val included = listOf("source/type", "copyright")
        val hidden = listOf("source/type/private")

        assertTrue(isGalleryTagDisplayed("source/type/human", included, hidden))
        assertTrue(isGalleryTagDisplayed("copyright/original", included, hidden))
        assertFalse(isGalleryTagDisplayed("source/format/anime", included, hidden))
        assertFalse(isGalleryTagDisplayed("source/type/private/secret", included, hidden))
    }

    @Test
    fun wildcardIncludesAllUnlessAHiddenRuleMatches() {
        assertTrue(isGalleryTagDisplayed("source/art", listOf("*"), emptyList()))
        assertFalse(isGalleryTagDisplayed("source/art", listOf("*"), listOf("source")))
    }

    @Test
    fun duplicatePrefixesRemainTransferCompatibleAndPersistable() {
        val prefixes = listOf("source/type", "source/type")

        assertTrue(GalleryTagDisplayPrefixesCodec.isValid(prefixes))
        assertEquals(
            prefixes,
            GalleryTagDisplayPrefixesCodec.decodeStorage(
                GalleryTagDisplayPrefixesCodec.encodeStorage(prefixes),
            ),
        )
    }
}
