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
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.thimmaiah.voxbox.voxscript.VoxScriptResult

data class NoteLibraryUiState(
    val notes: List<NoteEntity> = emptyList(),
    val activeNoteId: String? = null,
    val activeBlocks: List<NoteBlockEntity> = emptyList(),
    val status: String = "Create a note to save structured speech.",
)

@OptIn(ExperimentalCoroutinesApi::class)
class NoteLibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RoomNoteRepository(NoteDatabase.get(application).noteDao())
    private val _uiState = MutableStateFlow(NoteLibraryUiState())
    val uiState: StateFlow<NoteLibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeNotes().collectLatest { notes ->
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
            val note = repository.createNote("Voice note ${_uiState.value.notes.size + 1}")
            _uiState.value = _uiState.value.copy(
                activeNoteId = note.id,
                status = "${note.title} is ready for blocks.",
            )
        }
    }

    fun openNote(noteId: String) {
        val note = _uiState.value.notes.firstOrNull { it.id == noteId } ?: return
        _uiState.value = _uiState.value.copy(
            activeNoteId = note.id,
            status = "Opened ${note.title}. Saved blocks are read-only in this milestone.",
        )
    }

    fun savePreview(result: VoxScriptResult) {
        val block = result.toNoteBlockOrNull()
        if (block == null) {
            _uiState.value = _uiState.value.copy(status = "Correct the command before saving it.")
            return
        }
        viewModelScope.launch {
            val activeId = _uiState.value.activeNoteId ?: repository.createNote(
                "Voice note ${_uiState.value.notes.size + 1}",
            ).also { created ->
                _uiState.value = _uiState.value.copy(activeNoteId = created.id)
            }.id
            repository.appendBlock(activeId, block)
            _uiState.value = _uiState.value.copy(status = "${block.type.name.lowercase().replace('_', ' ')} saved locally.")
        }
    }
}
