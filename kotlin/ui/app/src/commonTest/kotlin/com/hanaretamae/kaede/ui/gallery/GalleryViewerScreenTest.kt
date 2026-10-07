package com.hanaretamae.kaede.ui.gallery

import com.hanaretamae.kaede.core.settings.GalleryNoteBlock
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryViewerScreenTest {
    @Test
    fun appliesConfiguredNoteDetailOrderAndVisibility() {
        val structure = GalleryNoteStructureSettings(
            blockOrder = listOf(
                GalleryNoteBlock.MEMO,
                GalleryNoteBlock.MEDIA,
                GalleryNoteBlock.RELATED,
                GalleryNoteBlock.POST_TEXT_END,
                GalleryNoteBlock.AUTHOR,
                GalleryNoteBlock.POST_TEXT,
            ),
            hiddenBlocks = listOf(GalleryNoteBlock.MEMO, GalleryNoteBlock.AUTHOR),
        )

        assertEquals(
            listOf(
                GalleryNoteBlock.RELATED,
                GalleryNoteBlock.POST_TEXT_END,
                GalleryNoteBlock.POST_TEXT,
            ),
            noteDetailContentBlocks(structure),
        )
    }
}
