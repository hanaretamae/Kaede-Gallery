package com.hanaretamae.kaede.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.hanaretamae.kaede.core.model.GalleryDetailLine
import com.hanaretamae.kaede.core.model.GalleryNoteDetail
import com.hanaretamae.kaede.core.model.MediaSummary
import com.hanaretamae.kaede.core.model.NoteId
import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.settings.GalleryTagColorCodec
import com.hanaretamae.kaede.core.settings.GalleryTagColorRule
import com.hanaretamae.kaede.core.settings.GalleryNoteBlock
import com.hanaretamae.kaede.core.settings.GalleryNoteStructureSettings

data class GalleryViewerStrings(
    val close: String,
    val previous: String,
    val next: String,
    val loading: String,
    val noMedia: String,
    val author: String,
    val link: String,
    val memo: String,
    val related: String,
    val postTextEnd: String,
    val published: String,
    val created: String,
    val updated: String,
    val tags: String,
    val openNote: String,
    val error: (RepositoryError) -> String,
)

val EnglishGalleryViewerStrings = GalleryViewerStrings(
    close = "Close",
    previous = "Previous",
    next = "Next",
    loading = "Loading note…",
    noMedia = "This note has no media",
    author = "Author",
    link = "Open link",
    memo = "Memo",
    related = "Related links",
    postTextEnd = "Post text end",
    published = "Published",
    created = "Created",
    updated = "Updated",
    tags = "Tags",
    openNote = "Open note",
    error = ::englishError,
)

val JapaneseGalleryViewerStrings = GalleryViewerStrings(
    close = "閉じる",
    previous = "前へ",
    next = "次へ",
    loading = "ノートを読み込み中…",
    noMedia = "このノートにメディアはありません",
    author = "作者",
    link = "リンクを開く",
    memo = "メモ",
    related = "関連リンク",
    postTextEnd = "本文終端",
    published = "公開日",
    created = "作成日",
    updated = "更新日",
    tags = "タグ",
    openNote = "ノートを開く",
    error = ::japaneseError,
)

/**
 * The platform renderer owns media decoding and receives the resolved media
 * state; the common viewer never opens a path or launches an external URL.
 */
@Composable
fun GalleryViewerScreen(
    stateHolder: GalleryViewerStateHolder,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    strings: GalleryViewerStrings = EnglishGalleryViewerStrings,
    mediaContent: @Composable (GalleryViewerMediaState) -> Unit,
    onExternalLink: (String) -> Unit,
    onOpenNote: (NoteId) -> Unit = {},
    tagColorRules: List<GalleryTagColorRule> = GalleryTagColorCodec.DEFAULT_RULES,
    noteStructure: GalleryNoteStructureSettings = GalleryNoteStructureSettings(),
) {
    val state by stateHolder.state.collectAsState()
    val horizontalOffset = remember(stateHolder) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    LaunchedEffect(stateHolder) { stateHolder.start() }
    DisposableEffect(stateHolder) {
        onDispose(stateHolder::dispose)
    }

    Surface(
        modifier = modifier.fillMaxSize().focusable().onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) {
                false
            } else {
                when (event.key) {
                    Key.DirectionLeft -> {
                        stateHolder.showPreviousMedia()
                        true
                    }
                    Key.DirectionRight -> {
                        stateHolder.showNextMedia()
                        true
                    }
                    else -> false
                }
            }
        },
        color = Color.Black,
        contentColor = Color.White,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClose) { Text(strings.close) }
                Spacer(Modifier.weight(1f))
                if (state.selectedMedia != null) {
                    Text("${state.selectedMediaIndex + 1} / ${state.media.size}")
                    TextButton(
                        onClick = stateHolder::showPreviousMedia,
                        enabled = state.selectedMediaIndex > 0,
                    ) { Text(strings.previous) }
                    TextButton(
                        onClick = stateHolder::showNextMedia,
                        enabled = state.selectedMediaIndex < state.media.lastIndex,
                    ) { Text(strings.next) }
                }
            }

            val media = state.selectedMedia
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(strings.loading, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (media == null && state.error == null) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(rememberScrollState()).padding(16.dp),
                ) {
                    Text(strings.noMedia, style = MaterialTheme.typography.titleMedium)
                    state.note?.let {
                        NoteDetails(
                            it,
                            strings,
                            onExternalLink,
                            onOpenNote,
                            tagColorRules,
                            noteStructure,
                        )
                    }
                }
            } else if (media != null) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .scrollable(
                            orientation = Orientation.Horizontal,
                            state = rememberScrollableState { delta ->
                                horizontalOffset.floatValue += delta
                                val threshold = with(density) { 72.dp.toPx() }
                                when {
                                    horizontalOffset.floatValue <= -threshold -> {
                                        stateHolder.showNextMedia()
                                        horizontalOffset.floatValue = 0f
                                    }
                                    horizontalOffset.floatValue >= threshold -> {
                                        stateHolder.showPreviousMedia()
                                        horizontalOffset.floatValue = 0f
                                    }
                                }
                                delta
                            },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        mediaContent(
                            GalleryViewerMediaState(
                                media = media,
                                location = state.mediaLocation,
                                locationLoading = state.mediaLocationLoading,
                            ),
                        )
                    }
                    state.note?.let { note ->
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                                .background(Color(0xFF151515))
                                .padding(16.dp),
                        ) {
                            NoteDetails(
                                note,
                                strings,
                                onExternalLink,
                                onOpenNote,
                                tagColorRules,
                                noteStructure,
                            )
                        }
                    }
                }
            }
            state.error?.let {
                Text(
                    strings.error(it),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun NoteDetails(
    note: GalleryNoteDetail,
    strings: GalleryViewerStrings,
    onExternalLink: (String) -> Unit,
    onOpenNote: (NoteId) -> Unit,
    tagColorRules: List<GalleryTagColorRule>,
    noteStructure: GalleryNoteStructureSettings,
) {
    Text(note.title, style = MaterialTheme.typography.titleLarge)
    note.published?.let { Text("${strings.published}: ${it.replace('T', ' ')}") }
    note.created?.let { Text("${strings.created}: ${it.replace('T', ' ')}") }
    note.updated?.let { Text("${strings.updated}: ${it.replace('T', ' ')}") }
    if (note.tags.isNotEmpty()) {
        Text(strings.tags, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            note.tags.forEach { tag ->
                AssistChip(
                    onClick = {},
                    label = { Text(tag) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(
                            GalleryTagColorCodec.colorFor(tag, tagColorRules),
                        ).copy(alpha = 0.22f),
                    ),
                )
            }
        }
    }
    noteDetailContentBlocks(noteStructure)
        .forEach { block ->
            when (block) {
                GalleryNoteBlock.AUTHOR -> {
                    note.author?.let { Text("${strings.author}: $it") }
                    note.authorUrl?.let { url ->
                        TextButton(onClick = { onExternalLink(url) }) { Text(strings.link) }
                    }
                    note.url?.let { url ->
                        TextButton(onClick = { onExternalLink(url) }) { Text(strings.link) }
                    }
                }
                GalleryNoteBlock.POST_TEXT -> {
                    note.bodyText.takeIf(String::isNotBlank)?.let { Text(it) }
                }
                GalleryNoteBlock.MEMO -> {
                    DetailLines(
                        heading = strings.memo,
                        lines = note.memoLines,
                        onExternalLink = onExternalLink,
                    )
                }
                GalleryNoteBlock.RELATED -> {
                    DetailLines(
                        heading = strings.related,
                        lines = note.relatedLines,
                        onExternalLink = onExternalLink,
                        onOpenNote = onOpenNote,
                        openNoteLabel = strings.openNote,
                    )
                }
                GalleryNoteBlock.POST_TEXT_END -> {
                    DetailLines(
                        heading = strings.postTextEnd,
                        lines = note.postTextEndLines,
                        onExternalLink = onExternalLink,
                        onOpenNote = onOpenNote,
                        openNoteLabel = strings.openNote,
                    )
                }
                GalleryNoteBlock.MEDIA -> Unit
            }
        }
}

internal fun noteDetailContentBlocks(
    noteStructure: GalleryNoteStructureSettings,
): List<GalleryNoteBlock> {
    return noteStructure.blockOrder.filter {
        it in RENDERED_NOTE_DETAIL_BLOCKS && it !in noteStructure.hiddenBlocks
    }
}

private val RENDERED_NOTE_DETAIL_BLOCKS = setOf(
    GalleryNoteBlock.AUTHOR,
    GalleryNoteBlock.POST_TEXT,
    GalleryNoteBlock.MEMO,
    GalleryNoteBlock.RELATED,
    GalleryNoteBlock.POST_TEXT_END,
)

@Composable
private fun DetailLines(
    heading: String,
    lines: List<GalleryDetailLine>,
    onExternalLink: (String) -> Unit,
    onOpenNote: (NoteId) -> Unit = {},
    openNoteLabel: String = "",
) {
    if (lines.isEmpty()) return
    Text(heading, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    lines.forEach { line ->
        Text(
            line.text,
            modifier = Modifier.padding(start = (line.indentLevel * 12).dp),
        )
        line.urls.forEach { url ->
            TextButton(onClick = { onExternalLink(url) }) { Text(url) }
        }
        line.linkedNoteId?.let { noteId ->
            TextButton(onClick = { onOpenNote(noteId) }) { Text(openNoteLabel) }
        }
    }
}
