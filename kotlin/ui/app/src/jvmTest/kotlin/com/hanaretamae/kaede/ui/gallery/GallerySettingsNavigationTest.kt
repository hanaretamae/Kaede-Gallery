package com.hanaretamae.kaede.ui.gallery

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsActions
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.settings.AppearanceSettings
import com.hanaretamae.kaede.core.settings.GallerySettings
import com.hanaretamae.kaede.core.settings.SettingsRepository
import com.hanaretamae.kaede.core.settings.SettingsStateHolder
import org.junit.Rule
import org.junit.Test

class GallerySettingsNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun exampleOpensOnItsOwnPageAndCanBeCopiedWithoutAVault() {
        var copied: androidx.compose.ui.text.AnnotatedString? = null
        val clipboard = object : androidx.compose.ui.platform.ClipboardManager {
            override fun getText() = copied
            override fun setText(annotatedString: androidx.compose.ui.text.AnnotatedString) {
                copied = annotatedString
            }
        }
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalClipboardManager provides clipboard,
            ) {
                val scope = rememberCoroutineScope()
                val holder = remember {
                    SettingsStateHolder(object : SettingsRepository {
                        override suspend fun load() = RepositoryResult.Success(GallerySettings())
                        override suspend fun save(settings: GallerySettings) = RepositoryResult.Success(Unit)
                    }, scope)
                }
                KaedeGalleryTheme(AppearanceSettings()) {
                    GallerySettingsScreen(holder, onClose = {}, strings = EnglishGallerySettingsStrings)
                }
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(EnglishGallerySettingsStrings.theme)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val strings = EnglishGallerySettingsStrings
        compose.onAllNodes(hasText(strings.exampleNote) and hasClickAction())[0]
            .performScrollTo().performClick()
        compose.onNodeWithText(strings.exampleNoteExplanations.first()).assertIsDisplayed()
        compose.onNode(hasText("# Fictional post text.", substring = true)).assertExists()
        compose.onNodeWithText(strings.copyExample).performScrollTo().performClick()
        compose.waitUntil(5_000) { copied != null }
        compose.runOnIdle {
            kotlin.test.assertEquals(
                galleryExampleMarkdown(com.hanaretamae.kaede.core.settings.GalleryNoteStructureSettings()),
                copied?.text,
            )
        }
    }

    @Test
    fun welcomeExampleRouteOpensDirectlyOnTheExamplePage() {
        var closed = false
        compose.setContent {
            val scope = rememberCoroutineScope()
            val holder = remember {
                SettingsStateHolder(object : SettingsRepository {
                    override suspend fun load() = RepositoryResult.Success(GallerySettings())
                    override suspend fun save(settings: GallerySettings) = RepositoryResult.Success(Unit)
                }, scope)
            }
            KaedeGalleryTheme(AppearanceSettings()) {
                GallerySettingsScreen(
                    holder,
                    onClose = { closed = true },
                    strings = EnglishGallerySettingsStrings,
                    openExampleOnStart = true,
                    exampleOnly = true,
                )
            }
        }
        compose.onNodeWithText(EnglishGallerySettingsStrings.exampleNoteExplanations.first())
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(EnglishGallerySettingsStrings.close).performClick()
        compose.runOnIdle { kotlin.test.assertTrue(closed) }
    }

    @Test
    fun sharedSettingsHolderStaysLoadedWhenSettingsPageIsLeft() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            val holder = remember {
                SettingsStateHolder(object : SettingsRepository {
                    override suspend fun load() = RepositoryResult.Success(GallerySettings())
                    override suspend fun save(settings: GallerySettings) = RepositoryResult.Success(Unit)
                }, scope)
            }
            KaedeGalleryTheme(AppearanceSettings()) {
                GallerySettingsScreen(
                    holder,
                    onClose = {},
                    loadOnEnter = true,
                    disposeStateHolderOnExit = false,
                )
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(EnglishGallerySettingsStrings.theme)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun showSettings(onSaved: (GallerySettings) -> Unit = {}) {
        compose.setContent {
            val scope = rememberCoroutineScope()
            val holder = remember {
                SettingsStateHolder(object : SettingsRepository {
                    override suspend fun load() = RepositoryResult.Success(GallerySettings())
                    override suspend fun save(settings: GallerySettings): RepositoryResult<Unit> {
                        onSaved(settings)
                        return RepositoryResult.Success(Unit)
                    }
                }, scope)
            }
            KaedeGalleryTheme(AppearanceSettings()) {
                GallerySettingsScreen(
                    holder, onClose = {},
                    onImportSettings = {},
                    onExportSettings = {},
                )
            }
        }
        val strings = EnglishGallerySettingsStrings
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(strings.theme).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun homeShowsAppearanceAndAboutDataHelpPagesHaveVisibleContent() {
        showSettings()
        val strings = EnglishGallerySettingsStrings
        compose.onNodeWithText(strings.theme).assertIsDisplayed()
        listOf(
            strings.about to "Kaede Gallery",
            strings.importExportReset to strings.reset,
            strings.helpAndUse to strings.relatedNotes,
        ).forEach { (page, content) ->
            compose.onNode(hasText(page) and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText(content).assertIsDisplayed()
            if (page == strings.about) {
                compose.onNodeWithText(GalleryAppVersion).assertIsDisplayed()
                compose.onNodeWithText(strings.aboutDescription).assertIsDisplayed()
            }
            compose.onNodeWithContentDescription(strings.close).performClick()
        }
        compose.onNodeWithText(strings.theme).assertIsDisplayed()
    }

    @Test
    fun noteBlockHeadingAliasesEditAndPersistAsListItems() {
        var savedSettings: GallerySettings? = null
        showSettings { savedSettings = it }
        val strings = EnglishGallerySettingsStrings
        compose.onNode(hasText(strings.noteStructure) and hasClickAction())
            .performScrollTo().performClick()
        compose.onNodeWithText(strings.frontmatterAliases).assertIsDisplayed()
        compose.onNodeWithText(strings.titleKeys).assertIsDisplayed()
        compose.onNodeWithText("title").assertIsDisplayed()
        compose.onNodeWithText(strings.linkResolution).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(strings.memoBlock).performScrollTo().performClick()
        compose.onNode(
            hasText("Memo") and hasClickAction() and hasAnyAncestor(isDialog()),
        ).performClick()
        compose.onNode(
            hasText(strings.memoHeadings) and
                SemanticsMatcher.keyIsDefined(SemanticsActions.SetText),
        ).performTextReplacement("Fictional annotations")
        compose.onAllNodesWithText(strings.close)[0].performClick()
        compose.onAllNodesWithText(strings.close)[0].performClick()
        compose.waitUntil(5_000) {
            val settings = savedSettings ?: return@waitUntil false
            com.hanaretamae.kaede.core.settings.GalleryNoteStructureCodec.decodeSettings(
                settings.flutterTagSettingsJson,
                settings.galleryTagPrefixes,
            ).memoHeadings.contains("Fictional annotations")
        }
    }

    @Test
    fun noteStructureVisibilitySwitchPrecedesReorderActions() {
        showSettings()
        val strings = EnglishGallerySettingsStrings
        compose.onNode(hasText(strings.noteStructure) and hasClickAction())
            .performScrollTo().performClick()
        val visibility = compose.onNodeWithContentDescription(
            strings.showBlock(strings.memoBlock),
        ).performScrollTo().fetchSemanticsNode().boundsInRoot.center.x
        val moveUp = compose.onAllNodesWithContentDescription(strings.moveUp)[4]
            .fetchSemanticsNode().boundsInRoot.center.x
        compose.runOnIdle {
            kotlin.test.assertTrue(
                visibility < moveUp,
                "visibility switch ($visibility) must precede move up ($moveUp)",
            )
        }
    }

    @Test
    fun settingsPagesCanScrollToTheirLastSectionsAfterNavigation() {
        showSettings()
        val strings = EnglishGallerySettingsStrings
        compose.onNode(hasText(strings.noteStructure) and hasClickAction())
            .performScrollTo().performClick()
        compose.onNodeWithText(strings.detailBlockOrder).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(strings.close).performClick()
        compose.onNode(hasText(strings.tagSettings) and hasClickAction())
            .performScrollTo().performClick()
        compose.onNodeWithText(strings.tagCategories).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tagSettingsPanelsAreNotShownOnNoteStructurePage() {
        showSettings()
        val strings = EnglishGallerySettingsStrings
        compose.onNode(hasText(strings.noteStructure) and hasClickAction())
            .performScrollTo().performClick()

        listOf(
            strings.galleryEligibility,
            strings.tagDisplayRules,
            strings.tagColors,
            strings.tagCategories,
        ).forEach { compose.onNodeWithText(it).assertDoesNotExist() }

        compose.onNodeWithText(strings.detailBlockOrder).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(strings.close).performClick()
        compose.onNode(hasText(strings.tagSettings) and hasClickAction())
            .performScrollTo().performClick()
        listOf(
            strings.galleryEligibility,
            strings.tagDisplayRules,
            strings.tagColors,
            strings.tagCategories,
        ).forEach { compose.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
    }

    @Test
    fun tagPrefixesCanBeAddedAndRemovedThroughValidatedListEditor() {
        showSettings()
        val strings = EnglishGallerySettingsStrings
        compose.onNode(hasText(strings.tagSettings) and hasClickAction()).performScrollTo().performClick()
        compose.onAllNodesWithText("source/art")[0].assertIsDisplayed()
        compose.onAllNodesWithText(strings.addItem, substring = false)[0]
            .performScrollTo().performClick()
        compose.onNode(
            hasText(strings.galleryTagPrefixes) and SemanticsMatcher.keyIsDefined(SemanticsActions.SetText),
        ).performTextReplacement("source/fictional")
        compose.onNodeWithText(strings.close).performClick()
        compose.onNodeWithText("source/fictional").assertIsDisplayed()
        compose.onNodeWithContentDescription("${strings.removeItem}: source/fictional").performClick()
        compose.onNodeWithText("source/fictional").assertDoesNotExist()
    }

    @Test
    fun tagColorsUseSeparateEditableNameAndColorFields() {
        showSettings()
        val strings = EnglishGallerySettingsStrings
        compose.onNode(hasText(strings.tagSettings) and hasClickAction())
            .performScrollTo().performClick()
        compose.onNodeWithContentDescription("${strings.editItem}: source/gender/female")
            .performScrollTo().performClick()
        compose.onNodeWithText(strings.tagName).assertIsDisplayed()
        compose.onNode(
            hasText(strings.tagColor) and
                SemanticsMatcher.keyIsDefined(SemanticsActions.SetText),
        ).assertIsDisplayed()
        compose.onNodeWithText(strings.chooseTagColor).assertIsDisplayed()
        compose.onNodeWithContentDescription(strings.chooseTagColor).assertIsDisplayed()
        compose.onNodeWithContentDescription("#EF5350").assertDoesNotExist()
        compose.onNodeWithText(strings.close).performClick()
    }
}
