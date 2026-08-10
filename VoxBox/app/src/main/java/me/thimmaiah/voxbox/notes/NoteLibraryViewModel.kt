package me.thimmaiah.voxbox.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.thimmaiah.voxbox.voxscript.VoxScriptResult

data class NoteLibraryUiState(
    val notes: List<NoteEntity> = emptyList(),
    val allNotes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val activeNoteId: String? = null,
    val activeBlocks: List<NoteBlockEntity> = emptyList(),
    val editDraft: NoteBlockEditDraft? = null,
    val exportZipPath: String? = null,
    val exportAssetCount: Int = 0,
    val folders: List<FolderEntity> = emptyList(),
    val noteLocations: List<NoteLocationEntity> = emptyList(),
    val selectedFolderId: String? = null,
    val status: String = "Create a note to save structured speech.",
) {
    val totalNoteCount: Int
        get() = allNotes.size

    val visibleNotes: List<NoteEntity>
        get() {
            val folderId = selectedFolderId ?: return notes
            val noteIds = noteLocations.asSequence()
                .filter { it.folderId == folderId }
                .map(NoteLocationEntity::noteId)
                .toSet()
            return notes.filter { it.id in noteIds }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NoteLibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NoteDatabase.get(application)
    private val repository = RoomNoteRepository(database.noteDao())
    private val sessionRepository = me.thimmaiah.voxbox.session.RoomCaptureSessionRepository(
        database.captureSessionDao(),
    )
    private val libraryRepository = RoomLibraryStructureRepository(database.libraryStructureDao())
    private val markdownExporter = MarkdownExporter(application)
    private val _uiState = MutableStateFlow(NoteLibraryUiState())
    val uiState: StateFlow<NoteLibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeNotes().collectLatest { notes ->
                _uiState.value = _uiState.value.copy(allNotes = notes)
            }
        }
        viewModelScope.launch {
            libraryRepository.observeFolders().collectLatest { folders ->
                _uiState.value = _uiState.value.copy(folders = folders)
            }
        }
        viewModelScope.launch {
            libraryRepository.observeNoteLocations().collectLatest { locations ->
                _uiState.value = _uiState.value.copy(noteLocations = locations)
            }
        }
        viewModelScope.launch {
            _uiState
                .map { it.searchQuery }
                .distinctUntilChanged()
                .flatMapLatest { query -> repository.observeNotes(query) }
                .collectLatest { notes ->
                    _uiState.value = _uiState.value.copy(notes = notes)
                }
        }
        viewModelScope.launch {
            _uiState
                .map { it.activeNoteId }
                .distinctUntilChanged()
                .flatMapLatest { noteId ->
                    if (noteId == null) flowOf(emptyList()) else repository.observeBlocks(noteId)
                }
                .collectLatest { blocks ->
                    _uiState.value = _uiState.value.copy(activeBlocks = blocks)
                }
        }
    }

    fun createNote() {
        viewModelScope.launch {
            val note = repository.createNote("Voice note ${_uiState.value.totalNoteCount + 1}")
            activateCreatedNote(
                note = note,
                status = "${note.title} is ready for blocks.",
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectFolder(folderId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedFolderId = folderId?.takeIf { id -> _uiState.value.folders.any { it.id == id } },
        )
    }

    fun openNote(noteId: String) {
        val note = _uiState.value.allNotes.firstOrNull { it.id == noteId } ?: return
        _uiState.value = _uiState.value.copy(
            activeNoteId = note.id,
            editDraft = null,
            exportZipPath = null,
            status = "Opened ${note.title}. Text and pie-chart blocks can be edited.",
        )
    }

    fun exportActiveNote() {
        val state = _uiState.value
        val note = state.allNotes.firstOrNull { it.id == state.activeNoteId } ?: return
        val blocks = state.activeBlocks
        _uiState.value = state.copy(status = "Preparing Markdown and diagram assets for export…", exportZipPath = null)
        viewModelScope.launch {
            try {
                val assets = sessionRepository.observeAssets(note.id).first()
                val transcript = sessionRepository.transcriptForNote(note.id)
                val export = markdownExporter.export(note, blocks, assets, transcript)
                _uiState.value = _uiState.value.copy(
                    exportZipPath = export.zipFile.absolutePath,
                    exportAssetCount = export.assetCount,
                    status = buildString {
                        append("Export created: refined note")
                        if (export.capturedFile != null) {
                            append(" plus captured evidence (${export.capturedSegmentCount} segments)")
                        }
                        append(", ${export.assetCount} diagram assets.")
                    },
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = error.message ?: "The note export could not be created.",
                    exportZipPath = null,
                )
            }
        }
    }

    fun consumeExport() {
        _uiState.value = _uiState.value.copy(exportZipPath = null)
    }

    fun startEditing(block: NoteBlockEntity) {
        val draft = block.toEditDraftOrNull()
        _uiState.value = if (draft == null) {
            _uiState.value.copy(status = "This saved block cannot be edited yet.")
        } else {
            _uiState.value.copy(editDraft = draft, status = "Editing saved ${block.type.lowercase().replace('_', ' ')}.")
        }
    }

    fun updateEditDraft(transform: (NoteBlockEditDraft) -> NoteBlockEditDraft) {
        val draft = _uiState.value.editDraft ?: return
        _uiState.value = _uiState.value.copy(editDraft = transform(draft))
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(editDraft = null, status = "Saved edit discarded.")
    }

    fun saveEditing() {
        val noteId = _uiState.value.activeNoteId ?: return
        val draft = _uiState.value.editDraft ?: return
        when (val validation = draft.validate()) {
            is NoteBlockEditValidation.Invalid -> {
                _uiState.value = _uiState.value.copy(status = validation.reason)
            }
            is NoteBlockEditValidation.Valid -> viewModelScope.launch {
                val saved = repository.updateBlock(noteId, draft.blockId, validation.update)
                _uiState.value = _uiState.value.copy(
                    editDraft = if (saved) null else draft,
                    status = if (saved) "Saved block changes locally." else "Block was not found; changes were not saved.",
                )
            }
        }
    }

    fun savePreview(result: VoxScriptResult) {
        val block = result.toNoteBlockOrNull()
        if (block == null) {
            _uiState.value = _uiState.value.copy(status = "Correct the command before saving it.")
            return
        }
        viewModelScope.launch {
            val activeId = _uiState.value.activeNoteId ?: run {
                val created = repository.createNote("Voice note ${_uiState.value.totalNoteCount + 1}")
                activateCreatedNote(created, status = "${created.title} is ready for blocks.")
                created.id
            }
            repository.appendBlock(activeId, block)
            _uiState.value = _uiState.value.copy(status = "${block.type.name.lowercase().replace('_', ' ')} saved locally.")
        }
    }

    fun saveBoardCapture(
        title: String,
        summary: String,
        visibleText: String,
        concepts: List<String>,
    ) {
        val content = BoardNoteContent(
            title = title,
            summary = summary,
            visibleText = visibleText,
            concepts = concepts,
        )
        val blocks = content.toNoteBlocks()
        viewModelScope.launch {
            val note = repository.createNoteWithBlocks(content.normalizedTitle(), blocks)
            activateCreatedNote(
                note = note,
                status = "Saved ${blocks.size} board-capture blocks locally in ${note.title}.",
            )
        }
    }

    private fun activateCreatedNote(note: NoteEntity, status: String) {
        val current = _uiState.value
        val allNotes = (listOf(note) + current.allNotes.filterNot { it.id == note.id })
            .sortedByDescending(NoteEntity::updatedAt)
        _uiState.value = current.copy(
            notes = allNotes,
            allNotes = allNotes,
            searchQuery = "",
            activeNoteId = note.id,
            editDraft = null,
            status = status,
        )
    }
}
