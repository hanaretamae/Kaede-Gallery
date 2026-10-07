package com.hanaretamae.kaede.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryNoteStructureCodecTest {
    @Test
    fun readsAndUpdatesRustSupportedNoteStructureAliases() {
        val original = """
            {"includedPrefixes":["*"],"hiddenPrefixes":[],"colors":[],
            "noteStructure":{
              "memoHeadings":["Notes"],
              "relatedHeadings":["Sources"],
              "postTextEndHeadings":["Details"],
              "galleryTagPrefixes":["source/art"],
              "frontmatter":{"tagsKeys":["tags","labels"],"titleKeys":["title","name"]},
              "linkResolution":"shortestPath",
              "postTextIncludeQuote":false,
              "blockOrder":["author","media"],
              "hiddenBlocks":["memo"]
            },"otherTagOption":true}
        """.trimIndent()
        val decoded = GalleryNoteStructureCodec.decodeSettings(
            original,
            listOf("source/art"),
        )
        val updated = GalleryNoteStructureCodec.update(
            original,
            decoded.copy(
                memoHeadings = listOf("Journal"),
                linkResolution = GalleryLinkResolution.ABSOLUTE_PATH,
            ),
        )

        assertEquals(listOf("Notes"), decoded.memoHeadings)
        assertEquals(listOf("tags", "labels"), decoded.frontmatter.tagsKeys)
        assertEquals(GalleryLinkResolution.SHORTEST_PATH, decoded.linkResolution)
        assertFalse(decoded.postTextIncludeQuote)
        assertEquals(
            listOf("author", "media", "postText", "postTextEnd", "related", "memo"),
            decoded.blockOrder.map { it.toFlutterNameForTest() },
        )
        assertTrue(
            updated.contains(
                """"blockOrder":["author","media","postText","postTextEnd","related","memo"]""",
            ),
        )
        assertTrue(updated.contains(""""hiddenBlocks":["memo"]"""))
        assertTrue(updated.contains(""""otherTagOption":true"""))
        assertEquals(
            GalleryLinkResolution.ABSOLUTE_PATH,
            GalleryNoteStructureCodec.decodeSettings(updated, listOf("source/art")).linkResolution,
        )
    }

    @Test
    fun validatesRequiredTagAliasesAndInputBounds() {
        val base = GalleryNoteStructureSettings()
        assertFalse(
            GalleryNoteStructureCodec.isValid(
                base.copy(frontmatter = base.frontmatter.copy(tagsKeys = emptyList())),
            ),
        )
        assertFalse(
            GalleryNoteStructureCodec.isValid(
                base.copy(memoHeadings = listOf("x".repeat(257))),
            ),
        )
        assertFalse(
            GalleryNoteStructureCodec.isValid(
                base.copy(frontmatter = base.frontmatter.copy(titleKeys = listOf("k".repeat(129)))),
            ),
        )
        assertFalse(
            GalleryNoteStructureCodec.isValid(
                base.copy(hiddenBlocks = listOf(GalleryNoteBlock.MEMO, GalleryNoteBlock.MEMO)),
            ),
        )
        assertFalse(
            GalleryNoteStructureCodec.isValid(
                base.copy(hiddenBlocks = listOf(GalleryNoteBlock.POST_TEXT_END)),
            ),
        )
    }

    @Test
    fun normalizesLegacyBlockOrderAndRejectsInvalidHiddenBlocks() {
        val settings = GalleryNoteStructureCodec.decodeSettings(
            """{"noteStructure":{"blockOrder":["author","media","postText","memo","related","postTextEnd"]}}""",
            GalleryTagPrefixesCodec.DEFAULT_PREFIXES,
        )
        assertEquals(GalleryNoteStructureSettings().blockOrder, settings.blockOrder)
        assertTrue(
            runCatching {
                GalleryNoteStructureCodec.decodeSettings(
                    """{"noteStructure":{"hiddenBlocks":["postTextEnd"]}}""",
                    GalleryTagPrefixesCodec.DEFAULT_PREFIXES,
                )
            }.isFailure,
        )
    }

    private fun GalleryNoteBlock.toFlutterNameForTest(): String = when (this) {
        GalleryNoteBlock.AUTHOR -> "author"
        GalleryNoteBlock.MEDIA -> "media"
        GalleryNoteBlock.POST_TEXT -> "postText"
        GalleryNoteBlock.POST_TEXT_END -> "postTextEnd"
        GalleryNoteBlock.RELATED -> "related"
        GalleryNoteBlock.MEMO -> "memo"
    }
}
