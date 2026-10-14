package com.hanaretamae.kaede.ui.gallery

import com.hanaretamae.kaede.core.settings.GalleryNoteBlock
import com.hanaretamae.kaede.core.settings.GalleryFrontmatterSettings
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureSettings
import com.hanaretamae.kaede.core.settings.LanguagePreference
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
            frontmatter = GalleryFrontmatterSettings(
                tagsKeys = listOf("labels"),
                titleKeys = listOf("name"),
                authorKeys = listOf("creator"),
                coverKeys = listOf("image"),
            ),
        )
        val markdown = galleryExampleMarkdown(structure, LanguagePreference.ENGLISH)
        assertTrue(markdown.startsWith("---\nname: Fictional gallery note\ncreator:\n  - Fictional author"))
        assertTrue(markdown.indexOf("creator:") < markdown.indexOf("url:"))
        assertTrue(markdown.indexOf("url:") < markdown.indexOf("labels:"))
        assertTrue("labels:\n  - source/art" in markdown)
        assertTrue("image: media/fictional-image.png" in markdown)
        assertTrue("## Fictional notes\n\n- Fictional memo" in markdown)
        assertTrue("## Fictional links" in markdown)
        assertTrue(markdown.indexOf("## Fictional notes") < markdown.indexOf("![[media/"))
        assertFalse("> Fictional quotation" in markdown)
    }

    @Test
    fun fictionalExampleIsLocalizedAndUsesNestedDocumentHeadings() {
        val english = galleryExampleMarkdown(
            GalleryNoteStructureSettings(),
            LanguagePreference.ENGLISH,
        )
        val japanese = galleryExampleMarkdown(
            GalleryNoteStructureSettings(),
            LanguagePreference.JAPANESE,
        )

        assertTrue("published: 2026-01-01T00:00:00" in english)
        assertTrue("created: 2026-01-01T00:00:00" in english)
        assertTrue("updated: 2026-01-02T00:00:00" in english)
        assertTrue("# Heading" in english)
        assertTrue("Fictional post text\n> Fictional quotation" in english)
        assertTrue("# Document\n\n## Related\n\n- [[fictional-related-note]]" in english)
        assertTrue("## Memo\n\n- Fictional memo" in english)
        assertFalse("架空の" in english)

        assertTrue("title: 架空のギャラリーノート" in japanese)
        assertTrue("author:\n  - 架空の作者" in japanese)
        assertTrue("# 見出し" in japanese)
        assertTrue("架空の投稿文\n> 架空の引用" in japanese)
        assertTrue("# 文書\n\n## 関連\n\n- [[架空の関連ノート]]" in japanese)
        assertTrue("## 覚書\n\n- 架空のメモ" in japanese)
    }
}
