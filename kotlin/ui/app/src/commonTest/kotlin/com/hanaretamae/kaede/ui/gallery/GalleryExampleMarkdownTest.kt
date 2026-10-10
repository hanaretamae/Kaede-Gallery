package com.hanaretamae.kaede.ui.gallery

import com.hanaretamae.kaede.core.settings.GalleryNoteBlock
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureSettings
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GalleryExampleMarkdownTest {
    @Test
    fun fictionalExampleUsesConfiguredHeadingsOrderAndTags() {
        val structure = GalleryNoteStructureSettings(
            memoHeadings = listOf("Fictional notes"),
            relatedHeadings = listOf("Fictional links"),
            galleryTagPrefixes = listOf("source/art"),
            blockOrder = listOf(
                GalleryNoteBlock.AUTHOR, GalleryNoteBlock.MEMO, GalleryNoteBlock.MEDIA,
                GalleryNoteBlock.POST_TEXT, GalleryNoteBlock.POST_TEXT_END, GalleryNoteBlock.RELATED,
            ),
            postTextIncludeQuote = false,
        )
        val markdown = galleryExampleMarkdown(structure)
        assertTrue(markdown.startsWith("---\ntitle:"))
        assertTrue("\"source/art\"" in markdown)
        assertTrue("# Fictional notes" in markdown)
        assertTrue("# Fictional links" in markdown)
        assertTrue(markdown.indexOf("# Fictional notes") < markdown.indexOf("![[media/"))
        assertFalse("> Fictional quotation." in markdown)
    }
}
