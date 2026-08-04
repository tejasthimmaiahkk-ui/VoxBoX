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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import me.thimmaiah.voxbox.speech.VoiceCaptureUiState
import me.thimmaiah.voxbox.voxscript.VoxScriptResult
import me.thimmaiah.voxbox.session.CaptureSessionScreen

private enum class VoxBoxDestination(
    val label: String,
    val supportingText: String,
) {
    Notes("Notes", "Your saved study library"),
    Speak("Live", "Continuous voice or camera notes"),
    Board("Board", "Capture a board or projector"),
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

    BackHandler(enabled = destination == VoxBoxDestination.Notes && noteDetailVisible) {
        noteDetailVisible = false
        if (noteState.editDraft != null) {
            noteLibraryViewModel.cancelEditing()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VoxBoxTopBar(
                destination = destination,
                showingNoteDetail = destination == VoxBoxDestination.Notes && noteDetailVisible,
                onBackFromNote = {
                    noteDetailVisible = false
                    if (noteState.editDraft != null) {
                        noteLibraryViewModel.cancelEditing()
                    }
                },
            )
        },
        bottomBar = {
            VoxBoxNavigationBar(
                selected = destination,
                onSelect = { selected ->
                    destination = selected
                },
            )
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
                            onCreateNote = {
                                previousActiveNoteId = noteState.activeNoteId
                                awaitingCreatedNote = true
                                noteLibraryViewModel.updateSearchQuery("")
                                noteLibraryViewModel.createNote()
                            },
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
                IconButton(
                    onClick = onBackFromNote,
                    modifier = Modifier.semantics { contentDescription = "Back to notes" },
                ) {
                    Text("←", style = MaterialTheme.typography.headlineSmall)
                }
            } else {
                Surface(
                    modifier = Modifier
                        .padding(start = VoxBoxSpacing.medium)
                        .size(40.dp)
                        .semantics { contentDescription = "VoxBox" },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("V", style = MaterialTheme.typography.titleLarge)
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
                icon = {
                    DestinationIcon(
                        destination = destination,
                        selected = selected == destination,
                    )
                },
                label = { Text(destination.label) },
                modifier = Modifier.semantics {
                    contentDescription = destination.label
                },
            )
        }
    }
}

@Composable
private fun DestinationIcon(
    destination: VoxBoxDestination,
    selected: Boolean,
) {
    val color = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Canvas(Modifier.size(24.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx())
        val lineWidth = 1.6.dp.toPx()
        when (destination) {
            VoxBoxDestination.Notes -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(4.dp.toPx(), 2.dp.toPx()),
                    size = Size(16.dp.toPx(), 20.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = stroke,
                )
                listOf(8.dp, 12.dp, 16.dp).forEach { y ->
                    drawLine(
                        color = color,
                        start = Offset(7.dp.toPx(), y.toPx()),
                        end = Offset(17.dp.toPx(), y.toPx()),
                        strokeWidth = lineWidth,
                    )
                }
            }

            VoxBoxDestination.Speak -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(8.dp.toPx(), 2.dp.toPx()),
                    size = Size(8.dp.toPx(), 13.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = stroke,
                )
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(5.dp.toPx(), 7.dp.toPx()),
                    size = Size(14.dp.toPx(), 11.dp.toPx()),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(12.dp.toPx(), 18.dp.toPx()),
                    end = Offset(12.dp.toPx(), 22.dp.toPx()),
                    strokeWidth = lineWidth,
                )
                drawLine(
                    color = color,
                    start = Offset(8.dp.toPx(), 22.dp.toPx()),
                    end = Offset(16.dp.toPx(), 22.dp.toPx()),
                    strokeWidth = lineWidth,
                )
            }

            VoxBoxDestination.Board -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(2.dp.toPx(), 3.dp.toPx()),
                    size = Size(20.dp.toPx(), 15.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(12.dp.toPx(), 18.dp.toPx()),
                    end = Offset(12.dp.toPx(), 22.dp.toPx()),
                    strokeWidth = lineWidth,
                )
                drawLine(
                    color = color,
                    start = Offset(7.dp.toPx(), 22.dp.toPx()),
                    end = Offset(17.dp.toPx(), 22.dp.toPx()),
                    strokeWidth = lineWidth,
                )
            }
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
            top = VoxBoxSpacing.medium,
            end = VoxBoxLayout.compactScreenPadding,
            bottom = VoxBoxSpacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
    ) {
        item("library-summary") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Study library",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = if (state.searchQuery.isBlank()) {
                                "${state.totalNoteCount} saved " +
                                    if (state.totalNoteCount == 1) "note" else "notes"
                            } else {
                                "${state.visibleNotes.size} " +
                                    (if (state.visibleNotes.size == 1) "match" else "matches") +
                                    " in ${state.totalNoteCount} " +
                                    (if (state.totalNoteCount == 1) "note" else "notes")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = onCreateNote,
                        modifier = Modifier.semantics {
                            contentDescription = "Create a new note"
                        },
                    ) {
                        Text("New note")
                    }
                }
                LibraryStatus(state.status)
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Search saved notes"
                        },
                    label = { Text("Search notes") },
                    placeholder = { Text("Title or saved content") },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.semantics {
                                    contentDescription = "Clear note search"
                                },
                            ) {
                                Text("×", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    },
                    singleLine = true,
                )
                if (state.folders.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Folders", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(
                        onClick = { onSelectFolder(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.selectedFolderId == null) "✓ All notes" else "All notes")
                    }
                    state.folders.take(5).forEach { folder ->
                        OutlinedButton(
                            onClick = { onSelectFolder(folder.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.selectedFolderId == folder.id) "✓ ${folder.name}" else folder.name)
                        }
                    }
                }
            }
        }

        if (state.totalNoteCount == 0) {
            item("empty-library") {
                EmptyNoteLibrary(onCreateNote)
            }
        } else if (state.visibleNotes.isEmpty() && state.selectedFolderId != null) {
            item("empty-folder") {
                VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                    VoxBoxStatusPill(label = "EMPTY FOLDER", tone = VoxBoxStatusTone.Warning)
                    Text("No notes in this folder yet.", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Choose this folder when starting a live session, or return to all notes.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { onSelectFolder(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Show all notes")
                    }
                }
            }
        } else if (state.visibleNotes.isEmpty()) {
            item("no-search-results") {
                NoSearchResults(
                    query = state.searchQuery,
                    onClearSearch = { onSearchQueryChange("") },
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
    }
}

@Composable
private fun NoSearchResults(
    query: String,
    onClearSearch: () -> Unit,
) {
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        VoxBoxStatusPill(label = "NO MATCHES", tone = VoxBoxStatusTone.Warning)
        Text(
            text = "Nothing matched “$query”.",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Try a title, concept, or phrase from a saved note.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onClearSearch,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Clear search")
        }
    }
}

@Composable
private fun LibraryStatus(status: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(
                horizontal = VoxBoxSpacing.medium,
                vertical = VoxBoxSpacing.small,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun EmptyNoteLibrary(onCreateNote: () -> Unit) {
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        VoxBoxStatusPill(label = "READY FOR YOUR FIRST NOTE")
        Text(
            text = "Keep everything you capture in one place.",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Create an empty note, dictate a structured block, or capture a board frame.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(
            onClick = onCreateNote,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create first note")
        }
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
            .semantics {
                contentDescription = "Open note ${note.title}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VoxBoxSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.medium,
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
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Updated ${formatTimestamp(note.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
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
            top = VoxBoxSpacing.medium,
            end = VoxBoxLayout.compactScreenPadding,
            bottom = VoxBoxSpacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
    ) {
        item("note-heading") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxStatusPill(
                    label = "SAVED LOCALLY",
                    tone = VoxBoxStatusTone.Success,
                )
                Text(
                    text = activeNote?.title ?: "Preparing note…",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                if (activeNote != null) {
                    Text(
                        text = "Updated ${formatTimestamp(activeNote.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onExport,
                    enabled = activeNote != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Export Markdown + diagrams")
                }
            }
        }

        if (activeNote != null && state.activeBlocks.isEmpty()) {
            item("empty-note") {
                VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                    Text("This note is empty.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Switch to Speak or Board to add study content.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

        if (activeNote != null) {
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
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.semantics {
                    contentDescription = "Edit block ${block.position + 1}"
                },
            ) {
                Text("Edit")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(VoxBoxSpacing.large),
            verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
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
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save changes")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
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
            style = MaterialTheme.typography.headlineSmall,
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
private fun SpeakScreen(
    state: VoiceCaptureUiState,
    modifier: Modifier = Modifier,
    onPrimaryAction: () -> Unit,
    onCancel: () -> Unit,
    onSavePreview: (VoxScriptResult) -> Unit,
    onLoadExample: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = VoxBoxLayout.compactScreenPadding,
            top = VoxBoxSpacing.medium,
            end = VoxBoxLayout.compactScreenPadding,
            bottom = VoxBoxSpacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
    ) {
        item("capture") {
            CaptureCard(
                state = state,
                onPrimaryAction = onPrimaryAction,
                onCancel = onCancel,
            )
        }
        item("speech-status") {
            PermissionAndRecognizerCard(state)
        }
        item("transcript") {
            TranscriptCard(state)
        }
        state.structuredResult?.let { result ->
            item("structured-preview") {
                StructuredPreview(result)
            }
            item("save-preview") {
                Button(
                    onClick = { onSavePreview(result) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Save structured preview to notes"
                        },
                    enabled = result !is VoxScriptResult.InvalidCommand,
                ) {
                    Text("Save to notes")
                }
            }
        }
        item("example") {
            OutlinedButton(
                onClick = onLoadExample,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Try the 25% wheat chart example")
            }
        }
        item("privacy-note") {
            Text(
                text = "Speech uses the Android recognizer shown above. Board AI is handled separately.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    start = VoxBoxSpacing.xSmall,
                    end = VoxBoxSpacing.xSmall,
                    bottom = VoxBoxSpacing.small,
                ),
            )
        }
    }
}

@Composable
private fun PermissionAndRecognizerCard(state: VoiceCaptureUiState) {
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Speech engine", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when {
                        !state.recognitionAvailable -> {
                            "No Android speech-recognition service was detected."
                        }

                        state.usesOnDeviceRecognizer -> "Android on-device recognizer selected."
                        else -> "Android system recognizer selected; it may use a network service."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            VoxBoxStatusPill(
                label = when {
                    !state.recognitionAvailable -> "UNAVAILABLE"
                    state.usesOnDeviceRecognizer -> "ON-DEVICE"
                    else -> "SYSTEM"
                },
                tone = when {
                    !state.recognitionAvailable -> VoxBoxStatusTone.Error
                    state.usesOnDeviceRecognizer -> VoxBoxStatusTone.Success
                    else -> VoxBoxStatusTone.Accent
                },
            )
        }
    }
}

@Composable
private fun CaptureCard(
    state: VoiceCaptureUiState,
    onPrimaryAction: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VoxBoxSpacing.xLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
        ) {
            VoxBoxStatusPill(
                label = when {
                    state.isListening -> "LISTENING"
                    !state.permissionGranted -> "PERMISSION NEEDED"
                    else -> "READY"
                },
                tone = when {
                    state.isListening -> VoxBoxStatusTone.Warning
                    !state.permissionGranted -> VoxBoxStatusTone.Warning
                    else -> VoxBoxStatusTone.Success
                },
            )
            Button(
                onClick = onPrimaryAction,
                enabled = state.recognitionAvailable || !state.permissionGranted,
                modifier = Modifier
                    .size(104.dp)
                    .semantics {
                        contentDescription = when {
                            !state.permissionGranted -> "Grant microphone permission"
                            state.isListening -> "Stop listening"
                            else -> "Start listening"
                        }
                    },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isListening) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (state.isListening) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                ),
            ) {
                Text(
                    text = when {
                        !state.permissionGranted -> "ALLOW"
                        state.isListening -> "STOP"
                        else -> "MIC"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = state.status,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { contentDescription = "Speech status: ${state.status}" },
            )
            Text(
                text = when {
                    !state.permissionGranted -> "Tap to allow microphone access."
                    state.isListening -> "Speak naturally, then tap STOP."
                    else -> "Dictate a paragraph or speak a VoxScript command."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (state.isListening) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel session")
                }
            }
        }
    }
}

@Composable
private fun TranscriptCard(state: VoiceCaptureUiState) {
    val visibleText = state.partialTranscript.ifBlank { state.finalTranscript }
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Transcript",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (visibleText.isNotBlank()) {
                VoxBoxStatusPill(
                    label = if (state.isListening) "LIVE" else "CAPTURED",
                    tone = if (state.isListening) {
                        VoxBoxStatusTone.Warning
                    } else {
                        VoxBoxStatusTone.Success
                    },
                )
            }
        }
        SelectionContainer {
            Text(
                text = visibleText.ifBlank { "Recognized speech will appear here." },
                style = MaterialTheme.typography.bodyLarge,
                color = if (visibleText.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        state.error?.let { error ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(VoxBoxSpacing.medium),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun StructuredPreview(result: VoxScriptResult) {
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Structured preview",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            VoxBoxStatusPill(
                label = if (result is VoxScriptResult.InvalidCommand) "CHECK" else "READY",
                tone = if (result is VoxScriptResult.InvalidCommand) {
                    VoxBoxStatusTone.Error
                } else {
                    VoxBoxStatusTone.Success
                },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when (result) {
            is VoxScriptResult.PlainDictation -> {
                Text("Paragraph", style = MaterialTheme.typography.labelMedium)
                Text(result.sourceText, style = MaterialTheme.typography.bodyLarge)
            }

            is VoxScriptResult.Heading -> Text(
                text = result.text,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )

            is VoxScriptResult.BulletPoint -> Row(
                horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
            ) {
                Text("•", color = MaterialTheme.colorScheme.primary)
                Text(result.text, style = MaterialTheme.typography.bodyLarge)
            }

            is VoxScriptResult.PieChart -> PieChartVisual(
                percentage = result.percentage,
                colorName = result.color,
                label = result.label,
            )

            is VoxScriptResult.InvalidCommand -> {
                Text("Command needs correction", style = MaterialTheme.typography.titleMedium)
                Text(result.reason, color = MaterialTheme.colorScheme.error)
            }
        }
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
                .size(96.dp)
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
                style = MaterialTheme.typography.titleLarge,
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

private fun formatTimestamp(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
