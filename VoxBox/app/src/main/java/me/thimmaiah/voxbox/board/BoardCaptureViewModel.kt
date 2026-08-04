package me.thimmaiah.voxbox.board

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BoardCaptureStage {
    LIVE_PREVIEW,
    CAPTURING,
    PROCESSING,
    REVIEW,
    SAVED,
    ERROR,
}

data class BoardExtractionDraft(
    val title: String,
    val summary: String,
    val visibleText: String,
    val conceptsText: String,
    val confidence: Double,
    val warnings: List<String>,
    val source: BoardExtractionSource,
    val equations: List<String>,
    val diagramRegions: List<DiagramRegion>,
) {
    fun toExtraction(): BoardExtraction = BoardExtraction(
        title = title.trim().ifBlank { "Board capture" },
        summary = summary.trim(),
        visibleText = visibleText.trim(),
        concepts = conceptsText
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(String::lowercase)
            .toList(),
        confidence = confidence.coerceIn(0.0, 1.0),
        warnings = warnings,
        source = source,
        equations = equations,
        diagramRegions = diagramRegions,
    )

    companion object {
        fun from(extraction: BoardExtraction): BoardExtractionDraft = BoardExtractionDraft(
            title = extraction.title,
            summary = extraction.summary,
            visibleText = extraction.visibleText,
            conceptsText = extraction.concepts.joinToString("\n"),
            confidence = extraction.confidence,
            warnings = extraction.warnings,
            source = extraction.source,
            equations = extraction.equations,
            diagramRegions = extraction.diagramRegions,
        )
    }
}

data class BoardCaptureUiState(
    val permissionGranted: Boolean = false,
    val cameraReady: Boolean = false,
    val stage: BoardCaptureStage = BoardCaptureStage.LIVE_PREVIEW,
    val draft: BoardExtractionDraft? = null,
    val status: String = "Camera permission required",
    val errorMessage: String? = null,
) {
    val canCapture: Boolean
        get() = permissionGranted && cameraReady && stage == BoardCaptureStage.LIVE_PREVIEW

    val canSave: Boolean
        get() = stage == BoardCaptureStage.REVIEW && draft != null
}

internal class BoardCaptureStateMachine(
    initialState: BoardCaptureUiState = BoardCaptureUiState(),
) {
    var state: BoardCaptureUiState = initialState
        private set

    fun onPermissionChanged(granted: Boolean) {
        state = state.copy(
            permissionGranted = granted,
            cameraReady = if (granted) state.cameraReady else false,
            status = when {
                !granted -> "Camera permission required"
                state.stage == BoardCaptureStage.REVIEW -> "Review the extraction before saving"
                state.stage == BoardCaptureStage.SAVED -> "Board capture saved"
                state.cameraReady -> "Aim at the board or projector, then capture one frame"
                else -> "Starting rear camera"
            },
            errorMessage = if (granted) null else state.errorMessage,
        )
    }

    fun onCameraReady() {
        if (state.stage != BoardCaptureStage.LIVE_PREVIEW) return
        state = state.copy(
            cameraReady = true,
            status = "Aim at the board or projector, then capture one frame",
            errorMessage = null,
        )
    }

    fun onCameraError(message: String) {
        state = state.copy(
            cameraReady = false,
            stage = BoardCaptureStage.ERROR,
            status = "Camera unavailable",
            errorMessage = message,
        )
    }

    fun beginCapture(): Boolean {
        if (!state.canCapture) return false
        state = state.copy(
            stage = BoardCaptureStage.CAPTURING,
            status = "Capturing board frame",
            errorMessage = null,
        )
        return true
    }

    fun beginProcessing() {
        if (state.stage != BoardCaptureStage.CAPTURING) return
        state = state.copy(
            stage = BoardCaptureStage.PROCESSING,
            status = "Extracting visible text and concepts",
            errorMessage = null,
        )
    }

    fun showReview(extraction: BoardExtraction) {
        state = state.copy(
            stage = BoardCaptureStage.REVIEW,
            draft = BoardExtractionDraft.from(extraction),
            status = "Review and edit before saving",
            errorMessage = null,
        )
    }

    fun showExtractionError(message: String) {
        state = state.copy(
            stage = BoardCaptureStage.ERROR,
            status = "Frame could not be extracted",
            errorMessage = message,
        )
    }

    fun updateDraft(transform: (BoardExtractionDraft) -> BoardExtractionDraft) {
        val currentDraft = state.draft ?: return
        if (state.stage != BoardCaptureStage.REVIEW) return
        state = state.copy(draft = transform(currentDraft))
    }

    fun consumeReviewedExtraction(): BoardExtraction? {
        if (!state.canSave) return null
        val extraction = state.draft?.toExtraction() ?: return null
        state = state.copy(
            stage = BoardCaptureStage.SAVED,
            status = "Board capture saved",
            errorMessage = null,
        )
        return extraction
    }

    fun retake() {
        state = state.copy(
            stage = BoardCaptureStage.LIVE_PREVIEW,
            draft = null,
            status = if (state.permissionGranted) {
                "Starting rear camera"
            } else {
                "Camera permission required"
            },
            errorMessage = null,
        )
    }
}

class BoardCaptureViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val stateMachine = BoardCaptureStateMachine()
    private val coordinator = BoardExtractionCoordinator(
        remoteClient = HttpBoardExtractionClient(),
        offlineClient = MlKitBoardExtractionClient(),
    )
    private val _uiState = MutableStateFlow(stateMachine.state)
    val uiState: StateFlow<BoardCaptureUiState> = _uiState.asStateFlow()
    private var extractionJob: Job? = null

    fun onPermissionChanged(granted: Boolean) = mutate {
        onPermissionChanged(granted)
    }

    fun onCameraReady() = mutate {
        onCameraReady()
    }

    fun onCameraError(message: String) = mutate {
        onCameraError(message)
    }

    fun onCaptureStarted(): Boolean {
        val started = stateMachine.beginCapture()
        publishState()
        return started
    }

    fun onCaptureFailed(message: String) = mutate {
        showExtractionError(message)
    }

    fun processCapturedFile(file: File) {
        stateMachine.beginProcessing()
        publishState()
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            try {
                val jpegBytes = withContext(Dispatchers.IO) { file.readBytes() }
                val extraction = coordinator.extract(jpegBytes)
                stateMachine.showReview(extraction)
                publishState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                stateMachine.showExtractionError(
                    error.message ?: "This frame could not be read. Check lighting and try again.",
                )
                publishState()
            } finally {
                withContext(NonCancellable + Dispatchers.IO) {
                    if (file.exists()) file.delete()
                }
            }
        }
    }

    fun updateTitle(value: String) = updateDraft { copy(title = value) }

    fun updateSummary(value: String) = updateDraft { copy(summary = value) }

    fun updateVisibleText(value: String) = updateDraft { copy(visibleText = value) }

    fun updateConcepts(value: String) = updateDraft { copy(conceptsText = value) }

    fun saveReviewedExtraction(): BoardExtraction? {
        val extraction = stateMachine.consumeReviewedExtraction()
        publishState()
        return extraction
    }

    fun retake() {
        extractionJob?.cancel()
        extractionJob = null
        mutate { retake() }
    }

    private fun updateDraft(transform: BoardExtractionDraft.() -> BoardExtractionDraft) = mutate {
        updateDraft(transform)
    }

    private inline fun mutate(block: BoardCaptureStateMachine.() -> Unit) {
        stateMachine.block()
        publishState()
    }

    private fun publishState() {
        _uiState.value = stateMachine.state
    }

    override fun onCleared() {
        extractionJob?.cancel()
        coordinator.close()
        super.onCleared()
    }
}
