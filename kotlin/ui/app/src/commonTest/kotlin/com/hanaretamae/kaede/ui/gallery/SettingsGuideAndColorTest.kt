package com.hanaretamae.kaede.ui.gallery

import androidx.compose.material3.ToggleButtonColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.hanaretamae.kaede.core.settings.GalleryNoteBlock
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SettingsGuideAndColorTest {
    @Test
    fun connectedChoiceColorsUseSemanticUnselectedRolesAndKeepOtherStates() {
        val defaults = ToggleButtonColors(
            containerColor = Color(0xff123456),
            contentColor = Color(0xff234567),
            disabledContainerColor = Color(0xff345678),
            disabledContentColor = Color(0xff456789),
            checkedContainerColor = Color(0xff56789a),
            checkedContentColor = Color(0xff6789ab),
        )
        val schemes = listOf(false, true).flatMap { darkTheme ->
            listOf(
                kaedeFallbackColorScheme(darkTheme),
                dynamicColorScheme(
                    seedColor = Color(0xff336699),
                    isDark = darkTheme,
                    style = PaletteStyle.Expressive,
                    specVersion = ColorSpec.SpecVersion.SPEC_2025,
                ),
            )
        }

        schemes.forEach { scheme ->
            val actual = connectedChoiceButtonColors(defaults, scheme)

            assertEquals(
                defaults.copy(
                    containerColor = scheme.surfaceContainerLow,
                    contentColor = scheme.onSurfaceVariant,
                ),
                actual,
            )
            assertNotEquals(defaults, actual)
            assertTrue(
                contrastRatio(scheme.onSurfaceVariant, scheme.surfaceContainerLow) >= 4.5f,
            )
            assertNotEquals(scheme.surfaceContainer, scheme.surfaceContainerLow)
        }
    }

    @Test
    fun exampleNoteFrameUsesOutlineVariantInBothThemes() {
        val schemes = listOf(false, true).flatMap { darkTheme ->
            listOf(
                kaedeFallbackColorScheme(darkTheme),
                dynamicColorScheme(
                    seedColor = Color(0xff336699),
                    isDark = darkTheme,
                    style = PaletteStyle.Expressive,
                    specVersion = ColorSpec.SpecVersion.SPEC_2025,
                ),
            )
        }

        schemes.forEach { scheme ->
            assertEquals(scheme.outline, exampleNoteFrameBorderColor(scheme))
            assertTrue(
                contrastRatio(exampleNoteFrameBorderColor(scheme), scheme.surfaceBright) >= 3f,
            )
        }
    }

    @Test
    fun helpSectionsKeepAllGuidanceGroupedAndLocalized() {
        val cases = listOf(
            EnglishGallerySettingsStrings to listOf(
                "Search",
                "Gallery",
                "Tags and filters",
                "Notes and links",
                "Note sections",
            ),
            JapaneseGallerySettingsStrings to listOf(
                "検索",
                "ギャラリー",
                "タグとフィルター",
                "ノートとリンク",
                "ノートの各項目",
            ),
        )

        cases.forEach { (strings, expectedHeadings) ->
            val sections = galleryHelpSections(strings)
            val entries = sections.flatMap { it.entries }

            assertEquals(expectedHeadings, sections.map { it.heading })
            assertEquals(13, entries.size)
            assertEquals(strings.searchAndFilteringHelp, entries[0].body)
            assertEquals(strings.galleryTagPrefixesHelp, entries[1].body)
            assertEquals(
                listOf(
                    strings.tagDisplayRulesHelp,
                    strings.tagColorsHelp,
                    strings.tagCategoriesHelp,
                ),
                sections[2].entries.map { it.body },
            )
            assertEquals(
                listOf(strings.relatedNotesHelp, strings.noteStructureUsageHelp),
                sections[3].entries.map { it.body },
            )
            assertEquals(
                GalleryNoteBlock.entries.map(strings.noteBlockHelp),
                sections[4].entries.map { it.body },
            )
            assertEquals(
                GalleryNoteBlock.entries.map { it.displayNameForTest(strings) },
                sections[4].entries.map { it.title },
            )
        }
    }

    private fun GalleryNoteBlock.displayNameForTest(strings: GallerySettingsStrings): String =
        when (this) {
            GalleryNoteBlock.AUTHOR -> strings.authorBlock
            GalleryNoteBlock.MEDIA -> strings.mediaBlock
            GalleryNoteBlock.POST_TEXT -> strings.postTextBlock
            GalleryNoteBlock.POST_TEXT_END -> strings.postTextEndBlock
            GalleryNoteBlock.MEMO -> strings.memoBlock
            GalleryNoteBlock.RELATED -> strings.relatedBlock
        }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
