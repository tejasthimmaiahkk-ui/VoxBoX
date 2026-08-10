package me.thimmaiah.voxbox.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.thimmaiah.voxbox.data.SettingsRepository
import me.thimmaiah.voxbox.data.VbSettings
import me.thimmaiah.voxbox.notes.NoteBlockEntity
import me.thimmaiah.voxbox.notes.NoteBlockType
import me.thimmaiah.voxbox.notes.TranscriptSegmentEntity
import me.thimmaiah.voxbox.notes.NoteLibraryViewModel
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbIconButton
import me.thimmaiah.voxbox.ui.VbIcons
import me.thimmaiah.voxbox.ui.VbOutlineButton
import me.thimmaiah.voxbox.ui.VbPrimaryButton
import me.thimmaiah.voxbox.ui.VbSegmented
import me.thimmaiah.voxbox.ui.VbSwitchRow
import me.thimmaiah.voxbox.ui.shareFile
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbMono
import me.thimmaiah.voxbox.ui.theme.VbReadingSize
import me.thimmaiah.voxbox.ui.theme.VbShape
import me.thimmaiah.voxbox.ui.theme.VbSpace
import me.thimmaiah.voxbox.ui.theme.readerBodyStyle

private enum class Sheet { NONE, OUTLINE, READING, SHARE, EDIT }

/**
 * The note as a document rather than a preview.
 *
 * The rule that shapes this screen: nothing here may quietly change what was captured. Editing a
 * block writes a new revision and says so; a review flag shows the captured text beside the
 * suggestion and neither button replaces the original line.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteReaderScreen(
    noteId: String,
    viewModel: NoteLibraryViewModel,
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = VbSettings())
    val listState = rememberLazyListState()
    val sheetScope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf(Sheet.NONE) }
    var finding by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var focus by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) { viewModel.openNote(noteId) }

    // The export used to stop here: a zip was built and its path recorded, and nothing ever
    // turned that into an intent, so the button looked broken. Hand it straight to the system.
    val context = LocalContext.current
    var exporting by remember { mutableStateOf(false) }
    LaunchedEffect(state.exportZipPath) {
        val path = state.exportZipPath ?: return@LaunchedEffect
        shareFile(context, java.io.File(path), "application/zip", "VoxBox note export")
        viewModel.consumeExport()
        exporting = false
        sheet = Sheet.NONE
    }

    // Evidence quoting needs the transcript, which is not part of the note's blocks.
    var evidence by remember { mutableStateOf<List<TranscriptSegmentEntity>>(emptyList()) }
    LaunchedEffect(noteId) { evidence = viewModel.transcriptFor(noteId) }

    val note = state.allNotes.firstOrNull { it.id == noteId }
    val blocks = state.activeBlocks
    val readingSize = VbReadingSize.fromOrdinal(settings.readingSize)
    val bodyStyle = readerBodyStyle(readingSize, settings.readingSerif)
    val matches = if (query.isBlank()) 0 else blocks.count { it.content.contains(query, true) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AnimatedVisibility(
            visible = !focus,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(300)) + fadeOut(tween(300)),
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            VbIconButton(VbIcons.ArrowBack, "Back", onBack)
            Text(
                text = note?.title?.ifBlank { "Untitled note" } ?: "Note",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            VbIconButton(VbIcons.Outline, "Outline", { sheet = Sheet.OUTLINE })
            VbIconButton(VbIcons.Edit, "Reading options", { sheet = Sheet.READING })
            VbIconButton(VbIcons.Search, "Find in note", { finding = !finding })
            VbIconButton(VbIcons.Share, "Share", { sheet = Sheet.SHARE })
        }
        }

        AnimatedVisibility(
            visible = finding,
            enter = expandVertically(tween(240)) + fadeIn(),
            exit = shrinkVertically(tween(240)) + fadeOut(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VbSpace.screenH, vertical = 6.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Find in note") },
                    singleLine = true,
                    shape = VbShape.pill,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (query.isBlank()) "" else "$matches",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalVbStatus.current.fg2,
                )
                VbIconButton(VbIcons.Close, "Close find", {
                    finding = false
                    query = ""
                })
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = VbSpace.screenH,
                end = VbSpace.screenH,
                top = 12.dp,
                bottom = 60.dp,
            ),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            itemsIndexed(blocks, key = { _, block -> block.id }) { _, block ->
                val parsed = parseNoteForReview(block.content)
                val diagrams = findDiagramLinks(parsed.body)
                val prose = if (diagrams.isEmpty()) parsed.body else withoutDiagramLinks(parsed.body)
                MarkdownBody(
                    markdown = prose,
                    bodyStyle = bodyStyle,
                    query = query,
                    onEdit = {
                        viewModel.startEditing(block)
                        sheet = Sheet.EDIT
                    },
                )
                diagrams.forEach { link -> DiagramBlock(link) }
                parsed.warnings.forEach { warning ->
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalVbStatus.current.review,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                parsed.flags.forEach { flag ->
                    ReviewFlagCard(
                        flag = flag,
                        evidence = evidence,
                        onKeepCaptured = { viewModel.resolveFlagKeepingCapture(block.id, flag.suggested) },
                        onAddAnnotation = { viewModel.annotateFlag(block.id, flag) },
                    )
                }
            }
            if (blocks.isEmpty()) {
                item {
                    Text(
                        text = "This note has no content yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalVbStatus.current.fg2,
                    )
                }
            }
        }
    }

    when (sheet) {
        Sheet.NONE -> Unit

        Sheet.OUTLINE -> ModalBottomSheet(
            onDismissRequest = { sheet = Sheet.NONE },
            sheetState = rememberModalBottomSheetState(),
            shape = VbShape.sheet,
        ) {
            Column(Modifier.padding(horizontal = VbSpace.screenH, vertical = 8.dp)) {
                VbEyebrow("Outline")
                Spacer(Modifier.height(10.dp))
                val headings = blocks.withIndex()
                    .filter { it.value.type == NoteBlockType.HEADING.name }
                if (headings.isEmpty()) {
                    Text(
                        text = "No headings yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalVbStatus.current.fg2,
                    )
                }
                headings.forEach { (index, block) ->
                    Text(
                        text = block.content,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sheetScope.launch { listState.animateScrollToItem(index) }
                                sheet = Sheet.NONE
                            }
                            .padding(vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        Sheet.READING -> ModalBottomSheet(
            onDismissRequest = { sheet = Sheet.NONE },
            sheetState = rememberModalBottomSheetState(),
            shape = VbShape.sheet,
        ) {
            Column(Modifier.padding(horizontal = VbSpace.screenH, vertical = 8.dp)) {
                VbEyebrow("Reading size")
                Spacer(Modifier.height(10.dp))
                VbSegmented(
                    options = VbReadingSize.entries.toList(),
                    selected = readingSize,
                    label = { it.label },
                    onSelect = { scope.launch { settingsRepository.setReadingSize(it.ordinal) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                VbSegmented(
                    options = listOf(false, true),
                    selected = settings.readingSerif,
                    label = { if (it) "Serif" else "Sans" },
                    onSelect = { scope.launch { settingsRepository.setReadingSerif(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                VbSwitchRow(
                    title = "Focus mode",
                    supporting = "Hides the toolbar so only the note is on screen.",
                    checked = focus,
                    onCheckedChange = { focus = it },
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        Sheet.SHARE -> ModalBottomSheet(
            onDismissRequest = { sheet = Sheet.NONE },
            sheetState = rememberModalBottomSheetState(),
            shape = VbShape.sheet,
        ) {
            Column(Modifier.padding(horizontal = VbSpace.screenH, vertical = 8.dp)) {
                VbEyebrow("Share")
                Spacer(Modifier.height(12.dp))
                VbPrimaryButton(
                    text = if (exporting) "Preparing…" else "Export note and evidence",
                    onClick = {
                        exporting = true
                        viewModel.exportActiveNote()
                    },
                    enabled = !exporting,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "The export contains two documents: the refined note, and a " +
                        "captured-evidence note with the verbatim transcript.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalVbStatus.current.fg2,
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        Sheet.EDIT -> {
            val draft = state.editDraft
            ModalBottomSheet(
                onDismissRequest = {
                    viewModel.cancelEditing()
                    sheet = Sheet.NONE
                },
                sheetState = rememberModalBottomSheetState(),
                shape = VbShape.sheet,
            ) {
                Column(Modifier.padding(horizontal = VbSpace.screenH, vertical = 8.dp)) {
                    VbEyebrow("Markdown")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = draft?.content.orEmpty(),
                        onValueChange = { value ->
                            viewModel.updateEditDraft { it.copy(content = value) }
                        },
                        minLines = 4,
                        shape = VbShape.card,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Saving writes a new revision. The transcript evidence behind this " +
                            "line is not changed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalVbStatus.current.fg2,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VbOutlineButton(
                            text = "Cancel",
                            onClick = {
                                viewModel.cancelEditing()
                                sheet = Sheet.NONE
                            },
                            modifier = Modifier.weight(1f),
                        )
                        VbPrimaryButton(
                            text = "Save",
                            onClick = {
                                viewModel.saveEditing()
                                sheet = Sheet.NONE
                            },
                            height = 44.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
