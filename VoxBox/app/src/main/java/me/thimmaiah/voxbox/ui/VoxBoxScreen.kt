package me.thimmaiah.voxbox.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.FileProvider
import java.io.File
import java.text.DateFormat
import java.util.Date
import me.thimmaiah.voxbox.board.BoardCaptureScreen
import me.thimmaiah.voxbox.notes.NoteBlockEditDraft
import me.thimmaiah.voxbox.notes.NoteBlockEntity
import me.thimmaiah.voxbox.notes.NoteBlockType
import me.thimmaiah.voxbox.notes.NoteEntity
import me.thimmaiah.voxbox.notes.NoteLibraryUiState
import me.thimmaiah.voxbox.notes.NoteLibraryViewModel
import me.thimmaiah.voxbox.notes.ReadOnlyNoteBlock
import me.thimmaiah.voxbox.notes.toReadOnlyBlockOrNull
import me.thimmaiah.voxbox.session.CaptureSessionScreen

private enum class VoxBoxDestination(
    val label: String,
    val supportingText: String,
    val icon: ImageVector,
) {
    Notes("Notes", "Your saved study library", VoxBoxIcons.Notes),
    Speak("Live", "Continuous voice or camera notes", VoxBoxIcons.Microphone),
    Board("Board", "Capture a board or projector", VoxBoxIcons.Board),
}

@Composable
fun VoxBoxScreen() {
    val noteLibraryViewModel: NoteLibraryViewModel = viewModel()
    val noteState by noteLibraryViewModel.uiState.collectAsState()
    var destination by rememberSaveable { mutableStateOf(VoxBoxDestination.Speak) }
    var noteDetailVisible by rememberSaveable { mutableStateOf(false) }
    var awaitingCreatedNote by rememberSaveable { mutableStateOf(false) }
    var previousActiveNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(noteState.activeNoteId, awaitingCreatedNote) {
        val currentId = noteState.activeNoteId
        if (awaitingCreatedNote && currentId != null && currentId != previousActiveNoteId) {
            awaitingCreatedNote = false
            noteDetailVisible = true
        }
    }

    val showingNoteDetail = destination == VoxBoxDestination.Notes && noteDetailVisible
    val leaveNoteDetail = {
        noteDetailVisible = false
        if (noteState.editDraft != null) {
            noteLibraryViewModel.cancelEditing()
        }
    }
    val createNote = {
        previousActiveNoteId = noteState.activeNoteId
        awaitingCreatedNote = true
        noteLibraryViewModel.updateSearchQuery("")
        noteLibraryViewModel.createNote()
    }

    BackHandler(enabled = showingNoteDetail) { leaveNoteDetail() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VoxBoxTopBar(
                destination = destination,
                showingNoteDetail = showingNoteDetail,
                onBackFromNote = leaveNoteDetail,
            )
        },
        bottomBar = {
            VoxBoxNavigationBar(
                selected = destination,
                onSelect = { selected -> destination = selected },
            )
        },
        floatingActionButton = {
            if (destination == VoxBoxDestination.Notes && !noteDetailVisible) {
                ExtendedFloatingActionButton(
                    onClick = createNote,
                    modifier = Modifier.semantics { contentDescription = "Create a new note" },
                    icon = { Icon(VoxBoxIcons.Add, contentDescription = null) },
                    text = { Text("New note") },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val pageModifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = VoxBoxLayout.contentMaxWidth)

            when (destination) {
                VoxBoxDestination.Notes -> {
                    if (noteDetailVisible) {
                        SavedNoteDetailScreen(
                            state = noteState,
                            modifier = pageModifier,
                            onStartEditing = noteLibraryViewModel::startEditing,
                            onUpdateEditDraft = noteLibraryViewModel::updateEditDraft,
                            onSaveEditing = noteLibraryViewModel::saveEditing,
                            onCancelEditing = noteLibraryViewModel::cancelEditing,
                            onExport = noteLibraryViewModel::exportActiveNote,
                            onExportShared = noteLibraryViewModel::consumeExport,
                        )
                    } else {
                        NoteLibraryScreen(
                            state = noteState,
                            modifier = pageModifier,
                            onSearchQueryChange = noteLibraryViewModel::updateSearchQuery,
                            onSelectFolder = noteLibraryViewModel::selectFolder,
                            onCreateNote = createNote,
                            onOpenNote = { noteId ->
                                noteLibraryViewModel.updateSearchQuery("")
                                noteLibraryViewModel.openNote(noteId)
                                noteDetailVisible = true
                            },
                        )
                    }
                }

                VoxBoxDestination.Speak -> CaptureSessionScreen(
                    modifier = pageModifier,
                    onOpenNote = { noteId ->
                        noteLibraryViewModel.updateSearchQuery("")
                        noteLibraryViewModel.openNote(noteId)
                        destination = VoxBoxDestination.Notes
                        noteDetailVisible = true
                    },
                )

                VoxBoxDestination.Board -> BoardCaptureScreen(
                    modifier = pageModifier,
                    onSaveExtraction = { extraction ->
                        previousActiveNoteId = noteState.activeNoteId
                        awaitingCreatedNote = true
                        noteLibraryViewModel.updateSearchQuery("")
                        noteLibraryViewModel.saveBoardCapture(
                            title = extraction.title,
                            summary = extraction.summary,
                            visibleText = extraction.visibleText,
                            concepts = extraction.concepts,
                        )
                        destination = VoxBoxDestination.Notes
                        noteDetailVisible = false
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VoxBoxTopBar(
    destination: VoxBoxDestination,
    showingNoteDetail: Boolean,
    onBackFromNote: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = if (showingNoteDetail) "Saved note" else destination.label,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                )
                Text(
                    text = if (showingNoteDetail) {
                        "Review and edit local blocks"
                    } else {
                        destination.supportingText
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            if (showingNoteDetail) {
                IconButton(onClick = onBackFromNote) {
                    Icon(VoxBoxIcons.Back, contentDescription = "Back to notes")
                }
            } else {
                Surface(
                    modifier = Modifier
                        .padding(start = VoxBoxSpacing.medium)
                        .size(38.dp)
                        .semantics { contentDescription = "VoxBox" },
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "V",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun VoxBoxNavigationBar(
    selected: VoxBoxDestination,
    onSelect: (VoxBoxDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        VoxBoxDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
                modifier = Modifier.semantics {
                    contentDescription = destination.label
                },
            )
        }
    }
}

@Composable
private fun NoteLibraryScreen(
    state: NoteLibraryUiState,
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = VoxBoxLayout.compactScreenPadding,
            top = VoxBoxSpacing.small,
            end = VoxBoxLayout.compactScreenPadding,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
    ) {
        item("library-search") {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search saved notes" },
                placeholder = { Text("Search titles and saved content") },
                leadingIcon = { Icon(VoxBoxIcons.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(VoxBoxIcons.Close, contentDescription = "Clear note search")
                        }
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                singleLine = true,
            )
        }

        if (state.folders.isNotEmpty()) {
            item("library-folders") {
                VoxBoxChipGroup {
                    VoxBoxChip(
                        label = "All notes",
                        selected = state.selectedFolderId == null,
                        onClick = { onSelectFolder(null) },
                        icon = VoxBoxIcons.Notes,
                    )
                    state.folders.take(8).forEach { folder ->
                        VoxBoxChip(
                            label = folder.name,
                            selected = state.selectedFolderId == folder.id,
                            onClick = { onSelectFolder(folder.id) },
                            icon = VoxBoxIcons.Folder,
                        )
                    }
                }
            }
        }

        item("library-summary") {
            Text(
                text = libraryCountLabel(state),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = VoxBoxSpacing.xSmall),
            )
        }

        if (state.totalNoteCount == 0) {
            item("empty-library") {
                VoxBoxEmptyState(
                    icon = VoxBoxIcons.Notes,
                    title = "Nothing saved yet",
                    message = "Record a live session, capture a board, or start an empty note. " +
                        "Everything stays on this phone.",
                    action = {
                        FilledTonalButton(onClick = onCreateNote) {
                            Text("Create first note")
                        }
                    },
                )
            }
        } else if (state.visibleNotes.isEmpty() && state.selectedFolderId != null) {
            item("empty-folder") {
                VoxBoxEmptyState(
                    icon = VoxBoxIcons.Folder,
                    title = "This folder is empty",
                    message = "Choose this folder when you start a live session, or return to all notes.",
                    action = {
                        OutlinedButton(onClick = { onSelectFolder(null) }) {
                            Text("Show all notes")
                        }
                    },
                )
            }
        } else if (state.visibleNotes.isEmpty()) {
            item("no-search-results") {
                VoxBoxEmptyState(
                    icon = VoxBoxIcons.Search,
                    title = "No matches",
                    message = "Nothing matched “${state.searchQuery}”. Try a title, concept, " +
                        "or phrase from a saved note.",
                    action = {
                        OutlinedButton(onClick = { onSearchQueryChange("") }) {
                            Text("Clear search")
                        }
                    },
                )
            }
        } else {
            items(
                items = state.visibleNotes,
                key = { note -> note.id },
            ) { note ->
                NoteLibraryRow(note = note, onOpenNote = onOpenNote)
            }
        }

        if (state.status.isNotBlank()) {
            item("library-status") {
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = VoxBoxSpacing.xSmall),
                )
            }
        }
    }
}

private fun libraryCountLabel(state: NoteLibraryUiState): String {
    val total = state.totalNoteCount
    val totalLabel = "$total ${if (total == 1) "note" else "notes"}"
    return when {
        state.searchQuery.isNotBlank() -> {
            val matches = state.visibleNotes.size
            "${if (matches == 1) "1 match" else "$matches matches"} in $totalLabel"
        }
        else -> totalLabel
    }
}

@Composable
private fun NoteLibraryRow(
    note: NoteEntity,
    onOpenNote: (String) -> Unit,
) {
    Card(
        onClick = { onOpenNote(note.id) },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Open note ${note.title}" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(13.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = note.title.firstOrNull()?.uppercase() ?: "N",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Updated ${formatTimestamp(note.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = VoxBoxIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SavedNoteDetailScreen(
    state: NoteLibraryUiState,
    modifier: Modifier = Modifier,
    onStartEditing: (NoteBlockEntity) -> Unit,
    onUpdateEditDraft: ((NoteBlockEditDraft) -> NoteBlockEditDraft) -> Unit,
    onSaveEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onExport: () -> Unit,
    onExportShared: () -> Unit,
) {
    val activeNote = state.allNotes.firstOrNull { it.id == state.activeNoteId }
    val context = LocalContext.current

    LaunchedEffect(state.exportZipPath) {
        val path = state.exportZipPath ?: return@LaunchedEffect
        val file = File(path)
        if (file.isFile) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, activeNote?.title ?: "VoxBox note")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share Obsidian-ready note",
                ),
            )
        }
        onExportShared()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = VoxBoxLayout.compactScreenPadding,
            top = VoxBoxSpacing.small,
            end = VoxBoxLayout.compactScreenPadding,
            bottom = VoxBoxLayout.listBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(VoxBoxLayout.sectionSpacing),
    ) {
        item("note-heading") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxStatusPill(label = "SAVED LOCALLY", tone = VoxBoxStatusTone.Success)
                Text(
                    text = activeNote?.title ?: "Preparing note…",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                if (activeNote != null) {
                    Text(
                        text = "Updated ${formatTimestamp(activeNote.updatedAt)} · " +
                            "${state.activeBlocks.size} block(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = onExport,
                    enabled = activeNote != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(VoxBoxIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Export Markdown + diagrams",
                        modifier = Modifier.padding(start = VoxBoxSpacing.small),
                    )
                }
                if (state.status.isNotBlank()) {
                    Text(
                        text = state.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (activeNote != null && state.activeBlocks.isEmpty()) {
            item("empty-note") {
                VoxBoxEmptyState(
                    icon = VoxBoxIcons.Waveform,
                    title = "This note is empty",
                    message = "Open Live or Board to add captured study content to it.",
                )
            }
        }

        items(
            items = state.activeBlocks,
            key = { block -> block.id },
        ) { block ->
            val editDraft = state.editDraft
            if (editDraft?.blockId == block.id) {
                BlockEditCard(
                    draft = editDraft,
                    onUpdateDraft = onUpdateEditDraft,
                    onSave = onSaveEditing,
                    onCancel = onCancelEditing,
                )
            } else {
                SavedBlockCard(
                    block = block,
                    onEdit = { onStartEditing(block) },
                )
            }
        }

        if (activeNote != null && state.activeBlocks.isNotEmpty()) {
            item("editing-help") {
                Text(
                    text = "Text and pie-chart blocks can be edited. Delete and reorder controls are planned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = VoxBoxSpacing.xSmall),
                )
            }
        }
    }
}

@Composable
private fun SavedBlockCard(
    block: NoteBlockEntity,
    onEdit: () -> Unit,
) {
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = block.type.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(VoxBoxLayout.minimumTouchTarget),
            ) {
                Icon(
                    imageVector = VoxBoxIcons.Edit,
                    contentDescription = "Edit block ${block.position + 1}",
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        ReadOnlyBlock(block.toReadOnlyBlockOrNull(), block.position)
    }
}

@Composable
private fun BlockEditCard(
    draft: NoteBlockEditDraft,
    onUpdateDraft: ((NoteBlockEditDraft) -> NoteBlockEditDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    VoxBoxSectionCard(
        modifier = Modifier.fillMaxWidth(),
        tone = VoxBoxStatusTone.Accent,
    ) {
        Text("Edit saved block", style = MaterialTheme.typography.titleMedium)
        if (draft.type == NoteBlockType.PIE_CHART.name) {
            OutlinedTextField(
                value = draft.percentageText,
                onValueChange = { value -> onUpdateDraft { it.copy(percentageText = value) } },
                label = { Text("Percentage (0–100)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.color,
                onValueChange = { value -> onUpdateDraft { it.copy(color = value) } },
                label = { Text("Color") },
                supportingText = {
                    Text("red, orange, yellow, green, blue, purple, pink, black or white")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.label,
                onValueChange = { value -> onUpdateDraft { it.copy(label = value) } },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        } else {
            OutlinedTextField(
                value = draft.content,
                onValueChange = { value -> onUpdateDraft { it.copy(content = value) } },
                label = { Text("Text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
        ) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Text("Save changes")
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun ReadOnlyBlock(block: ReadOnlyNoteBlock?, position: Int) {
    when (block) {
        is ReadOnlyNoteBlock.Paragraph -> SelectionContainer {
            Text(block.text, style = MaterialTheme.typography.bodyLarge)
        }

        is ReadOnlyNoteBlock.Markdown -> MarkdownNotePreview(
            markdown = block.text,
            modifier = Modifier.fillMaxWidth(),
        )

        is ReadOnlyNoteBlock.Heading -> Text(
            text = block.text,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )

        is ReadOnlyNoteBlock.BulletPoint -> Row(
            horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
        ) {
            Text("•", color = MaterialTheme.colorScheme.primary)
            SelectionContainer {
                Text(block.text, style = MaterialTheme.typography.bodyLarge)
            }
        }

        is ReadOnlyNoteBlock.PieChart -> PieChartVisual(
            percentage = block.percentage,
            colorName = block.color,
            label = block.label,
        )

        null -> Text(
            text = "Block ${position + 1} cannot be displayed because its data is incomplete.",
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun PieChartVisual(
    percentage: Int,
    colorName: String,
    label: String,
) {
    val sliceColor = namedColor(colorName)
    val remainderColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val outlineColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$label pie chart, $percentage percent $colorName"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.large),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(remainderColor),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(remainderColor)
                drawArc(
                    color = sliceColor,
                    startAngle = -90f,
                    sweepAngle = 360f * percentage / 100f,
                    useCenter = true,
                )
                drawCircle(outlineColor, style = Stroke(width = 1.dp.toPx()))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = label.replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.headlineMedium,
                color = sliceColor,
            )
            Text(
                text = "$colorName • remainder",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun namedColor(name: String): Color {
    val colors = MaterialTheme.colorScheme
    return when (name.lowercase()) {
        "red" -> colors.error
        "orange" -> Color(0xFFB95700)
        "yellow" -> colors.tertiary
        "green" -> Color(0xFF2E7D32)
        "blue" -> colors.primary
        "purple" -> Color(0xFF7B1FA2)
        "pink" -> Color(0xFFAD1457)
        "black" -> colors.onSurface
        else -> colors.surfaceContainerHighest
    }
}

internal fun formatTimestamp(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
