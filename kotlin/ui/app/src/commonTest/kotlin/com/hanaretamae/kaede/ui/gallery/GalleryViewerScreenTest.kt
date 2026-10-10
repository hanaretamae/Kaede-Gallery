package com.hanaretamae.kaede.ui.gallery

import com.hanaretamae.kaede.core.model.GalleryDetailLine
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.settings.GalleryNoteBlock
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureSettings
import com.hanaretamae.kaede.core.settings.GalleryTagDisplayPrefixesCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun viewerUsesHumanReadableDetailLinkLabelsAndRejectsUnsafeTargets() {
        val webLink = GalleryDetailLine(
            text = "fictional link",
            urls = listOf("https://example.invalid/related"),
            isBullet = true,
            indentLevel = 0,
            linkedNoteId = null,
        )
        val noteLink = GalleryDetailLine(
            text = "note-000005",
            urls = emptyList(),
            isBullet = true,
            indentLevel = 0,
            linkedNoteId = NoteId(5),
        )
        val unsafeLink = webLink.copy(urls = listOf("javascript:alert(1)"))
        val multipleLinks = webLink.copy(
            urls = listOf("https://example.invalid/one", "https://example.invalid/two"),
        )

        assertEquals("fictional link", galleryViewerDetailLineActionLabel(webLink, "Open link"))
        assertEquals("note-000005", galleryViewerDetailLineActionLabel(noteLink, "Open link"))
        assertEquals("Open link", galleryViewerDetailLineActionLabel(multipleLinks, "Open link"))
        assertEquals(null, galleryViewerDetailLineActionLabel(unsafeLink, "Open link"))
    }

    @Test
    fun viewerHidesConfiguredTagPrefixesLikeFlutterDetails() {
        assertEquals(
            listOf("copyright/pin", "source/service/example"),
            galleryViewerVisibleTags(
                listOf("source/service/example", "source/art", "copyright/pin"),
                GalleryTagDisplayPrefixesCodec.DEFAULT_HIDDEN,
            ),
        )
        assertEquals(
            listOf("source/art"),
            galleryViewerVisibleTags(listOf("source/art"), emptyList()),
        )
    }

    @Test
    fun imageZoomRemainsWithinSupportedScale() {
        assertEquals(1f, nextImageScale(1f, 0.5f))
        assertEquals(2f, nextImageScale(1f, 2f))
        assertEquals(5f, nextImageScale(4f, 2f))
    }

    @Test
    fun horizontalSwipesSwitchMediaButDoNotOverrideVerticalOrZoomGestures() {
        assertEquals(
            GalleryViewerSwipeDirection.NEXT,
            galleryViewerSwipeDirection(-120f, 8f, 16f, 72f, imageZoomed = false),
        )
        assertEquals(
            GalleryViewerSwipeDirection.PREVIOUS,
            galleryViewerSwipeDirection(120f, 8f, 16f, 72f, imageZoomed = false),
        )
        assertEquals(
            null,
            galleryViewerSwipeDirection(-80f, 90f, 16f, 72f, imageZoomed = false),
        )
        assertEquals(
            null,
            galleryViewerSwipeDirection(-120f, 8f, 16f, 72f, imageZoomed = true),
        )
        assertEquals(
            null,
            galleryViewerSwipeDirection(-70f, 8f, 16f, 72f, imageZoomed = false),
        )
    }

    @Test
    fun horizontalTrackpadScrollSwitchesMediaButRespectsThresholdAndZoom() {
        assertEquals(
            GalleryViewerSwipeDirection.NEXT,
            galleryViewerTrackpadSwipeDirection(-56f, 48f, imageZoomed = false),
        )
        assertEquals(
            GalleryViewerSwipeDirection.PREVIOUS,
            galleryViewerTrackpadSwipeDirection(56f, 48f, imageZoomed = false),
        )
        assertEquals(null, galleryViewerTrackpadSwipeDirection(47f, 48f, imageZoomed = false))
        assertEquals(null, galleryViewerTrackpadSwipeDirection(-56f, 48f, imageZoomed = true))
    }

    @Test
    fun wallpaperActionIsLimitedToSupportedImageFiles() {
        assertTrue(galleryCanSetWallpaper("media/photo.webp", isVideo = false))
        assertFalse(galleryCanSetWallpaper("media/clip.mp4", isVideo = false))
        assertFalse(galleryCanSetWallpaper("media/photo.png", isVideo = true))
        assertFalse(galleryCanSetWallpaper(null, isVideo = false))
    }

    @Test
    fun viewerOnlyAcceptsWebUrlsWithoutCredentials() {
        assertEquals(true, isGalleryViewerWebUrl("https://x.com/name/status/123"))
        assertEquals(true, isGalleryViewerWebUrl("http://example.com:8080/path"))
        assertEquals(false, isGalleryViewerWebUrl("javascript:alert(1)"))
        assertEquals(false, isGalleryViewerWebUrl("https://user@example.com/path"))
        assertEquals(false, isGalleryViewerWebUrl("https://example.com:99999/path"))
    }

    @Test
    fun viewerDerivesTitleAuthorAndXProfileFromFilenameAndPostUrl() {
        val postUrl = "https://x.com/fictional_author/status/123456"
        val profileUrl = galleryViewerAuthorProfileUrl(null, postUrl)

        assertEquals(
            "A fictional title",
            galleryViewerTitle("fictional_author-on-X-A fictional title.md", "Unparsed title"),
        )
        assertEquals("Fallback", galleryViewerTitle("plain-name.md", "Fallback"))
        assertEquals("https://x.com/fictional_author", profileUrl)
        assertEquals(
            "@fictional_author",
            galleryViewerAuthor(
                "fictional_author-on-X-A fictional title.md",
                null,
                profileUrl,
            ),
        )
        assertEquals(
            null,
            galleryViewerAuthorProfileUrl(
                null,
                "https://x.com.evil/fictional_author/status/123",
            ),
        )
    }

    @Test
    fun viewerUsesAuthorUrlAndRespectsAuthorBlockPosition() {
        assertEquals(
            "https://example.com/fictional-author",
            galleryViewerAuthorProfileUrl(
                "https://example.com/fictional-author",
                "https://x.com/fictional_author/status/123",
            ),
        )
        assertEquals(true, galleryViewerShowsAuthorAboveMedia(GalleryNoteStructureSettings()))
        assertEquals(
            false,
            galleryViewerShowsAuthorAboveMedia(
                GalleryNoteStructureSettings(
                    blockOrder = listOf(
                        GalleryNoteBlock.MEDIA,
                        GalleryNoteBlock.AUTHOR,
                        GalleryNoteBlock.POST_TEXT,
                        GalleryNoteBlock.POST_TEXT_END,
                        GalleryNoteBlock.RELATED,
                        GalleryNoteBlock.MEMO,
                    ),
                ),
            ),
        )
    }

    @Test
    fun mediaScrollForwardingIsDisabledWhenHiddenZoomedOrFullscreen() {
        assertTrue(
            galleryViewerCanForwardMediaVerticalScroll(
                detailsVisible = true,
                fullscreen = false,
                isVideo = false,
                imageZoomed = false,
            ),
        )
        assertFalse(
            galleryViewerCanForwardMediaVerticalScroll(
                detailsVisible = false,
                fullscreen = false,
                isVideo = false,
                imageZoomed = false,
            ),
        )
        assertFalse(
            galleryViewerCanForwardMediaVerticalScroll(
                detailsVisible = true,
                fullscreen = true,
                isVideo = false,
                imageZoomed = false,
            ),
        )
        assertFalse(
            galleryViewerCanForwardMediaVerticalScroll(
                detailsVisible = true,
                fullscreen = false,
                isVideo = true,
                imageZoomed = false,
            ),
        )
        assertFalse(
            galleryViewerCanForwardMediaVerticalScroll(
                detailsVisible = true,
                fullscreen = false,
                isVideo = false,
                imageZoomed = true,
            ),
        )
    }

    @Test
    fun detailPanelExpandsWithScrollAndKeepsMediaVisible() {
        assertEquals(0.4f, galleryViewerDetailsPanelFraction(0), 0.001f)
        assertEquals(0.61f, galleryViewerDetailsPanelFraction(110), 0.001f)
        assertEquals(0.82f, galleryViewerDetailsPanelFraction(220), 0.001f)
        assertEquals(0.82f, galleryViewerDetailsPanelFraction(440), 0.001f)
        assertEquals(0.4f, galleryViewerDetailsPanelFraction(-10), 0.001f)
        assertEquals(0.61f, galleryViewerDetailsPanelFraction(330, 660f), 0.001f)
        assertEquals(0.82f, galleryViewerDetailsPanelFraction(660, 660f), 0.001f)
    }

    @Test
    fun viewerStartsWithMediaOnlyAndMediaTapTogglesDetails() {
        val initial = GalleryViewerPresentationState()

        assertEquals(false, initial.detailsVisible)
        assertEquals(
            GalleryViewerPresentationState(detailsVisible = true),
            initial.toggleMedia(),
        )
        assertEquals(
            GalleryViewerPresentationState(),
            initial.toggleMedia().toggleMedia(),
        )
    }

    @Test
    fun fullscreenEntryHidesDetailsAndExitRestoresThem() {
        val details = GalleryViewerPresentationState().toggleMedia()
        val fullscreen = details.enterFullscreen()

        assertEquals(GalleryViewerPresentationState(fullscreen = true), fullscreen)
        assertEquals(
            details,
            fullscreen.toggleMedia(),
        )
        assertEquals(
            details,
            fullscreen.exitFullscreen(),
        )
    }
}
