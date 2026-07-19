package me.thimmaiah.voxbox.speech

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.thimmaiah.voxbox.voxscript.VoxScriptParser
import me.thimmaiah.voxbox.voxscript.VoxScriptResult

data class VoiceCaptureUiState(
    val permissionGranted: Boolean = false,
    val recognitionAvailable: Boolean = false,
    val usesOnDeviceRecognizer: Boolean = false,
    val isListening: Boolean = false,
    val status: String = "Checking speech support…",
    val partialTranscript: String = "",
    val finalTranscript: String = "",
    val structuredResult: VoxScriptResult? = null,
    val error: String? = null,
)

class VoiceCaptureViewModel(application: Application) : AndroidViewModel(application) {
    private val parser = VoxScriptParser()
    private val _uiState = MutableStateFlow(VoiceCaptureUiState())
    val uiState: StateFlow<VoiceCaptureUiState> = _uiState.asStateFlow()

    private val controller = SpeechRecognitionController(
        context = application,
        callbacks = object : SpeechRecognitionCallbacks {
            override fun onReady() = updateListeningStatus("Ready—start speaking.")
            override fun onBeginningOfSpeech() = updateListeningStatus("Listening…")

            override fun onPartialResult(text: String) {
                _uiState.value = _uiState.value.copy(
                    partialTranscript = text,
                    status = "Listening…",
                    error = null,
                )
            }

            override fun onFinalResult(text: String) {
                applyTranscript(text)
            }

            override fun onError(message: String) {
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    status = "Ready to try again",
                    error = message,
                )
            }

            override fun onEndOfSpeech() {
                _uiState.value = _uiState.value.copy(status = "Processing speech…")
            }

            override fun onRecognizerModeChanged(
                usesOnDeviceRecognizer: Boolean,
                message: String,
            ) {
                _uiState.value = _uiState.value.copy(
                    usesOnDeviceRecognizer = usesOnDeviceRecognizer,
                    isListening = false,
                    status = "System fallback ready",
                    error = message,
                )
            }
        },
    )

    init {
        val capability = controller.initialize()
        _uiState.value = _uiState.value.copy(
            recognitionAvailable = capability.isAvailable,
            usesOnDeviceRecognizer = capability.usesOnDeviceRecognizer,
            status = if (capability.isAvailable) "Microphone permission required" else "Speech recognition unavailable",
        )
    }

    fun onPermissionChanged(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            permissionGranted = granted,
            status = when {
                !granted -> "Microphone permission required"
                !_uiState.value.recognitionAvailable -> "Speech recognition unavailable"
                else -> "Ready to listen"
            },
            error = null,
        )
    }

    fun startListening() {
        val state = _uiState.value
        if (!state.permissionGranted) {
            _uiState.value = state.copy(error = "Grant microphone permission before listening.")
            return
        }
        if (!state.recognitionAvailable) {
            _uiState.value = state.copy(error = "Speech recognition is unavailable on this device.")
            return
        }
        _uiState.value = state.copy(
            isListening = true,
            status = "Starting recognizer…",
            partialTranscript = "",
            error = null,
        )
        controller.startListening()
    }

    fun stopListening() {
        controller.stopListening()
        _uiState.value = _uiState.value.copy(status = "Processing speech…")
    }

    fun cancelListening() {
        controller.cancel()
        _uiState.value = _uiState.value.copy(
            isListening = false,
            status = "Ready to listen",
            error = null,
        )
    }

    fun loadExample() {
        applyTranscript("Tejas pie chart 25 percent yellow label wheat")
    }

    private fun applyTranscript(text: String) {
        val cleaned = text.trim()
        _uiState.value = _uiState.value.copy(
            isListening = false,
            status = if (cleaned.isBlank()) "No speech recognized" else "Structured preview ready",
            partialTranscript = "",
            finalTranscript = cleaned,
            structuredResult = cleaned.takeIf { it.isNotBlank() }?.let(parser::parse),
            error = if (cleaned.isBlank()) "No clear speech was recognized." else null,
        )
    }

    private fun updateListeningStatus(status: String) {
        _uiState.value = _uiState.value.copy(
            isListening = true,
            status = status,
            error = null,
        )
    }

    override fun onCleared() {
        controller.destroy()
        super.onCleared()
    }
}
