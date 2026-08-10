package me.thimmaiah.voxbox.library

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.thimmaiah.voxbox.notes.NoteEntity
import me.thimmaiah.voxbox.notes.NoteLibraryViewModel
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbEmptyState
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbIcons
import me.thimmaiah.voxbox.ui.VbInitial
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

        if (state.folders.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                VbSegmented(
                    options = listOf<String?>(null) + state.folders.map { it.id },
                    selected = state.selectedFolderId,
                    label = { id ->
                        if (id == null) "All" else state.folders.firstOrNull { it.id == id }?.name ?: "Folder"
                    },
                    onSelect = viewModel::selectFolder,
                )
            }
        }

        Spacer(Modifier.height(VbSpace.section))

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
                item {
                    VbEyebrow("${notes.size} note${if (notes.size == 1) "" else "s"}")
                    Spacer(Modifier.height(8.dp))
                }
                items(notes, key = { it.id }) { note ->
                    NoteRow(note) { onOpenNote(note.id) }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: NoteEntity, onClick: () -> Unit) {
    VbCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
                    text = "Tap to read",
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
