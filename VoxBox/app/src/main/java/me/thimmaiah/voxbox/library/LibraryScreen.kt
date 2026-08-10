package me.thimmaiah.voxbox.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.thimmaiah.voxbox.notes.FolderEntity
import me.thimmaiah.voxbox.notes.NoteEntity
import me.thimmaiah.voxbox.notes.NoteLibraryViewModel
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbEmptyState
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbIconButton
import me.thimmaiah.voxbox.ui.VbIcons
import me.thimmaiah.voxbox.ui.VbInitial
import me.thimmaiah.voxbox.ui.VbOutlineButton
import me.thimmaiah.voxbox.ui.VbSegmented
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbShape
import me.thimmaiah.voxbox.ui.theme.VbSpace

@Composable
fun LibraryScreen(
    viewModel: NoteLibraryViewModel,
    contentPadding: PaddingValues,
    onOpenNote: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notes = state.visibleNotes
    var actionsFor by remember { mutableStateOf<NoteEntity?>(null) }
    var renaming by remember { mutableStateOf<NoteEntity?>(null) }
    var deleting by remember { mutableStateOf<NoteEntity?>(null) }
    var creatingFolder by remember { mutableStateOf(false) }
    var movingNote by remember { mutableStateOf<NoteEntity?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = VbSpace.screenH),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Library",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            placeholder = { Text("Search notes") },
            leadingIcon = {
                Icon(
                    painter = painterResource(VbIcons.Search),
                    contentDescription = null,
                    tint = LocalVbStatus.current.fg3,
                    modifier = Modifier.size(18.dp),
                )
            },
            singleLine = true,
            shape = VbShape.pill,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
            ) {
                Crumb("All notes", active = state.selectedFolderId == null) { viewModel.selectFolder(null) }
                state.folderPath.forEachIndexed { index, folder ->
                    Icon(
                        painter = painterResource(VbIcons.ChevronRight),
                        contentDescription = null,
                        tint = LocalVbStatus.current.fg3,
                        modifier = Modifier.size(14.dp),
                    )
                    Crumb(label = folder.name, active = index == state.folderPath.lastIndex) {
                        viewModel.selectFolder(folder.id)
                    }
                }
            }
            VbIconButton(VbIcons.Plus, "New folder", { creatingFolder = true })
        }

        Spacer(Modifier.height(VbSpace.gap))

        val subfolders = state.visibleFolders
        if (subfolders.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(VbSpace.gap)) {
                subfolders.forEach { folder ->
                    val count = state.noteLocations.count { it.folderId == folder.id }
                    VbCard(onClick = { viewModel.selectFolder(folder.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(VbIcons.Folder),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (count == 1) "1 note" else "$count notes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalVbStatus.current.fg2,
                                )
                            }
                            Icon(
                                painter = painterResource(VbIcons.ChevronRight),
                                contentDescription = null,
                                tint = LocalVbStatus.current.fg3,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(VbSpace.section))
        }

        if (notes.isEmpty()) {
            VbEmptyState(
                title = if (state.searchQuery.isBlank()) "No notes yet" else "Nothing matches",
                body = if (state.searchQuery.isBlank()) {
                    "Start a capture session and the note will appear here as it is written."
                } else {
                    "Try a shorter search. Titles and note text are both searched."
                },
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(VbSpace.gap),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
            ) {
                // Grouped by recency rather than listed flat: the note you want is almost
                // always one from this week, and a date under every row reads as noise.
                val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                val (thisWeek, earlier) = notes.partition { it.updatedAt >= weekAgo }
                if (thisWeek.isNotEmpty()) {
                    item(key = "eyebrow-week") {
                        VbEyebrow("This week")
                        Spacer(Modifier.height(8.dp))
                    }
                    items(thisWeek, key = { it.id }) { note ->
                        NoteRow(note, onClick = { onOpenNote(note.id) }, onLongClick = { actionsFor = note })
                    }
                }
                if (earlier.isNotEmpty()) {
                    item(key = "eyebrow-earlier") {
                        Spacer(Modifier.height(10.dp))
                        VbEyebrow("Earlier")
                        Spacer(Modifier.height(8.dp))
                    }
                    items(earlier, key = { it.id }) { note ->
                        NoteRow(note, onClick = { onOpenNote(note.id) }, onLongClick = { actionsFor = note })
                    }
                }
            }
        }
    }

    // Rendered outside the Column so a sheet or dialog is never clipped by the list.
    actionsFor?.let { note ->
        NoteActionsSheet(
            note = note,
            onRename = {
                actionsFor = null
                renaming = note
            },
            onMove = {
                actionsFor = null
                movingNote = note
            },
            onDelete = {
                actionsFor = null
                deleting = note
            },
            onDismiss = { actionsFor = null },
        )
    }
    renaming?.let { note ->
        RenameDialog(
            note = note,
            onRename = { title ->
                viewModel.renameNote(note.id, title)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { note ->
        DeleteDialog(
            note = note,
            onDelete = {
                viewModel.deleteNote(note.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
    if (creatingFolder) {
        CreateFolderDialog(
            parentName = state.folderPath.lastOrNull()?.name,
            onCreate = { name ->
                viewModel.createFolder(name, state.selectedFolderId)
                creatingFolder = false
            },
            onDismiss = { creatingFolder = false },
        )
    }
    movingNote?.let { note ->
        MoveNoteSheet(
            note = note,
            folders = state.folders,
            onMove = { folderId ->
                viewModel.moveNoteToFolder(note.id, folderId)
                movingNote = null
            },
            onDismiss = { movingNote = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(note: NoteEntity, onClick: () -> Unit, onLongClick: () -> Unit) {
    VbCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VbInitial(note.title)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled note" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Tap to read · hold for options",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalVbStatus.current.fg2,
                )
            }
            Icon(
                painter = painterResource(VbIcons.ChevronRight),
                contentDescription = null,
                tint = LocalVbStatus.current.fg3,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Long-press actions. Rename and delete are here rather than in the reader, because this is
 *  where you are when you realise a note is misnamed or was a false start. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteActionsSheet(
    note: NoteEntity,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(), shape = VbShape.sheet) {
        Column(Modifier.padding(horizontal = VbSpace.screenH, vertical = 8.dp)) {
            Text(
                text = note.title.ifBlank { "Untitled note" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            VbOutlineButton("Rename", onClick = onRename, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            VbOutlineButton("Move to folder", onClick = onMove, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            VbOutlineButton(
                text = "Delete",
                onClick = onDelete,
                contentColor = LocalVbStatus.current.danger,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RenameDialog(note: NoteEntity, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename note") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                shape = VbShape.pill,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onRename(title) }) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Names what is destroyed rather than asking "are you sure?".
 *
 * Deleting a note also deletes the transcript and the board crops captured with it, and a lecture
 * cannot be recorded twice — so the dialog says so before the button is pressed.
 */
@Composable
private fun DeleteDialog(note: NoteEntity, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this note?") },
        text = {
            Text(
                "\"${note.title.ifBlank { "Untitled note" }}\" and everything captured with it — the " +
                    "transcript, the board text and any diagram crops — will be removed from this " +
                    "device. This cannot be undone.",
            )
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = LocalVbStatus.current.danger)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep") } },
    )
}

@Composable
private fun Crumb(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (active) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary,
        maxLines = 1,
        modifier = Modifier
            .clip(VbShape.pill)
            .clickable(enabled = !active, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun CreateFolderDialog(parentName: String?, onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (parentName == null) "New folder" else "New folder in " + parentName) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Folder name") },
                singleLine = true,
                shape = VbShape.pill,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Every folder with its full path, so two units both called "Unit 1" stay distinguishable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveNoteSheet(
    note: NoteEntity,
    folders: List<FolderEntity>,
    onMove: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val byId = folders.associateBy { it.id }
    fun path(folder: FolderEntity): String {
        val parts = mutableListOf(folder.name)
        var parent = folder.parentId?.let(byId::get)
        while (parent != null) {
            parts.add(0, parent.name)
            parent = parent.parentId?.let(byId::get)
        }
        return parts.joinToString(" / ")
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = VbShape.sheet,
    ) {
        Column(Modifier.padding(horizontal = VbSpace.screenH, vertical = 8.dp)) {
            VbEyebrow("Move to")
            Spacer(Modifier.height(4.dp))
            Text(
                text = note.title.ifBlank { "Untitled note" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "All notes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMove(null) }
                    .padding(vertical = 12.dp),
            )
            folders.sortedBy { path(it) }.forEach { folder ->
                Text(
                    text = path(folder),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMove(folder.id) }
                        .padding(vertical = 12.dp),
                )
            }
            if (folders.isEmpty()) {
                Text(
                    text = "No folders yet. Create one with the + button.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalVbStatus.current.fg2,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
