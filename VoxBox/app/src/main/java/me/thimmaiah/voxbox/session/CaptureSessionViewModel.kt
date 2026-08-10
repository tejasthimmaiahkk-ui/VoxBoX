package me.thimmaiah.voxbox.session

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.thimmaiah.voxbox.audio.AudioTranscription
import me.thimmaiah.voxbox.audio.AudioTranscriptionClient
import me.thimmaiah.voxbox.audio.AudioTranscriptionException
import me.thimmaiah.voxbox.audio.HttpAudioTranscriptionClient
import me.thimmaiah.voxbox.audio.PerChunkSpeakerTracker
import me.thimmaiah.voxbox.debug.VbDebugLog
import me.thimmaiah.voxbox.audio.PcmAudioChunkRecorder
import me.thimmaiah.voxbox.audio.RecordedAudioChunk
import me.thimmaiah.voxbox.audio.SpeakerFocusSnapshot
import me.thimmaiah.voxbox.audio.SpeakerFocusStatus
import me.thimmaiah.voxbox.board.BoardExtraction
import me.thimmaiah.voxbox.board.BoardExtractionCoordinator
import me.thimmaiah.voxbox.board.DiagramCropper
import me.thimmaiah.voxbox.board.FrameChangeDetector
import me.thimmaiah.voxbox.board.HttpBoardExtractionClient
import me.thimmaiah.voxbox.board.MlKitBoardExtractionClient
import me.thimmaiah.voxbox.notes.CaptureSessionEntity
import me.thimmaiah.voxbox.notes.FolderEntity
import me.thimmaiah.voxbox.notes.NoteDatabase
import me.thimmaiah.voxbox.notes.NoteEntity
import me.thimmaiah.voxbox.notes.NoteBlockEntity
import me.thimmaiah.voxbox.notes.NoteBlockType
import me.thimmaiah.voxbox.network.VoxBoxServiceFailure
import me.thimmaiah.voxbox.notes.RoomLibraryStructureRepository
import me.thimmaiah.voxbox.notes.RoomNoteRepository
import me.thimmaiah.voxbox.notes.SyllabusEntity

enum class LiveCaptureStage {
    SETUP,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    ERROR,
}

data class LiveTranscriptLine(
    val id: String,
    val speakerId: String?,
    val startMs: Long,
    val text: String,
    val primary: Boolean,
)

/**
 * A privately retained recovery WAV whose transcript never committed.
 *
 * The file stays in app-private storage until the user retries or deletes it, so speech is never
 * silently discarded when the provider is unavailable.
 */
data class RetainedAudioChunk(
    val id: String,
    val sessionId: String,
    val offsetMs: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val path: String,
    val reason: String,
    val retrying: Boolean = false,
)

data class CaptureSessionUiState(
    val stage: LiveCaptureStage = LiveCaptureStage.SETUP,
    val notes: List<NoteEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val syllabi: List<SyllabusEntity> = emptyList(),
    val noteTitle: String = "",
    val selectedNoteId: String? = null,
    val selectedFolderId: String? = null,
    val selectedSyllabusId: String? = null,
    val mode: CaptureMode = CaptureMode.VOICE,
    val notePolicy: CaptureNotePolicy = CaptureNotePolicy.RUNNABLE,
    val noteDetail: NoteDetail = NoteDetail.CONCISE,
    val customInstruction: String = "",
    val frameIntervalMs: Long = CaptureSessionSettings.DEFAULT_FRAME_INTERVAL_MS,
    val changeThreshold: Double = CaptureSessionSettings.DEFAULT_CHANGE_THRESHOLD,
    val activeNoteId: String? = null,
    val activeSession: CaptureSessionEntity? = null,
    val startedAt: Long? = null,
    val revision: Long = 0,
    val existingNoteMarkdown: String = "",
    val generatedMarkdown: String = "",
    val transcript: List<LiveTranscriptLine> = emptyList(),
    val speakerFocus: SpeakerFocusSnapshot = SpeakerFocusSnapshot(
        status = SpeakerFocusStatus.LEARNING,
        selectedSpeakerId = null,
        observedVoicedMs = 0,
        leadingShare = 0.0,
        reason = "Speaker labels are evaluated independently inside each audio chunk.",
    ),
    val latestChunkSpeakerIds: List<String> = emptyList(),
    val serviceFailure: VoxBoxServiceFailure? = null,
    val verification: NoteVerification? = null,
    val verifying: Boolean = false,
    val retainedAudio: List<RetainedAudioChunk> = emptyList(),
    val pendingEvents: Int = 0,
    val pendingAudioChunks: Int = 0,
    val pendingFrames: Int = 0,
    val retainedAudioChunks: Int = 0,
    val acceptedFrames: Int = 0,
    val skippedFrames: Int = 0,
    val processedFrames: Int = 0,
    val lastFrameChangeScore: Double? = null,
    val corrections: List<SuggestedCorrection> = emptyList(),
    val warnings: List<String> = emptyList(),
    val status: String = "Choose a capture mode and start a note.",
    val error: String? = null,
) {
    val isActive: Boolean
        get() = stage in setOf(LiveCaptureStage.STARTING, LiveCaptureStage.RUNNING, LiveCaptureStage.STOPPING)

    val canEditSetup: Boolean
        get() = stage in setOf(LiveCaptureStage.SETUP, LiveCaptureStage.STOPPED, LiveCaptureStage.ERROR)
}

private data class QueuedAudioChunk(
    val id: String,
    val offsetMs: Long,
    val durationMs: Long,
    val recoveryFile: File?,
    val inMemoryWav: ByteArray?,
)

private data class QueuedFrame(
    val file: File,
    val capturedAt: Long,
)

class CaptureSessionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val database = NoteDatabase.get(application)
    private val noteRepository = RoomNoteRepository(database.noteDao())
    private val libraryRepository = RoomLibraryStructureRepository(database.libraryStructureDao())
    private val sessionRepository = RoomCaptureSessionRepository(database.captureSessionDao())
    private val transcriptionClient: AudioTranscriptionClient = HttpAudioTranscriptionClient()
    private val noteClient: NoteRefinementClient = HttpNoteRefinementClient()
    private val verificationClient: NoteVerificationClient = HttpNoteVerificationClient()
    private val boardCoordinator = BoardExtractionCoordinator(
        remoteClient = HttpBoardExtractionClient(),
        offlineClient = MlKitBoardExtractionClient(),
    )
    private val frameChangeDetector = FrameChangeDetector()
    private val diagramCropper = DiagramCropper(application.filesDir)
    private val syllabusImporter = SyllabusImporter(application)
    private val audioRecorder = PcmAudioChunkRecorder(viewModelScope)

    private val _uiState = MutableStateFlow(CaptureSessionUiState())
    val uiState: StateFlow<CaptureSessionUiState> = _uiState.asStateFlow()

    private var audioChannel: Channel<QueuedAudioChunk>? = null
    private var frameChannel: Channel<QueuedFrame>? = null
    private var audioProcessorJob: Job? = null
    private var frameProcessorJob: Job? = null
    private var stopJob: Job? = null
    private var stopRequestedDuringStart = false
    private var speakerTracker = PerChunkSpeakerTracker()
    private var latestPersistedTranscriptIds: Set<String> = emptySet()
    private val noteUpdateMutex = Mutex()
    private val trackedFrameFiles = synchronizedSetOf()
    private val retainedAudioIds = synchronizedSetOf()

    init {
        cleanupExpiredRawFrames()
        surfaceRetainedAudio()
        viewModelScope.launch {
            noteRepository.observeNotes().collectLatest { notes ->
                _uiState.update { state ->
                    state.copy(
                        notes = notes,
                        selectedNoteId = state.selectedNoteId?.takeIf { id -> notes.any { it.id == id } },
                    )
                }
            }
        }
        viewModelScope.launch {
            libraryRepository.observeFolders().collectLatest { folders ->
                _uiState.update { it.copy(folders = folders) }
            }
        }
        viewModelScope.launch {
            libraryRepository.observeSyllabi().collectLatest { syllabi ->
                _uiState.update { it.copy(syllabi = syllabi) }
            }
        }
    }

    fun setMode(mode: CaptureMode) = updateSetup { copy(mode = mode, error = null) }

    fun setNotePolicy(policy: CaptureNotePolicy) = updateSetup { copy(notePolicy = policy, error = null) }

    fun setNoteDetail(detail: NoteDetail) = updateSetup { copy(noteDetail = detail) }

    /** Free-text steer sent with every note update. The proxy ranks it below the evidence rules. */
    fun setCustomInstruction(instruction: String) = updateSetup {
        copy(customInstruction = instruction.take(500))
    }

    fun setNoteTitle(title: String) = updateSetup { copy(noteTitle = title.take(120), selectedNoteId = null) }

    fun selectExistingNote(noteId: String?) = updateSetup {
        val note = notes.firstOrNull { it.id == noteId }
        copy(selectedNoteId = note?.id, noteTitle = note?.title.orEmpty(), error = null)
    }

    fun selectFolder(folderId: String?) = updateSetup {
        copy(selectedFolderId = folderId?.takeIf { id -> folders.any { it.id == id } }, error = null)
    }

    fun selectSyllabus(syllabusId: String?) = updateSetup {
        copy(selectedSyllabusId = syllabusId?.takeIf { id -> syllabi.any { it.id == id } }, error = null)
    }

    fun setFrameIntervalMillis(value: Long) = updateSetup {
        copy(
            frameIntervalMs = value.coerceIn(
                CaptureSessionSettings.MIN_FRAME_INTERVAL_MS,
                CaptureSessionSettings.MAX_FRAME_INTERVAL_MS,
            ),
        )
    }

    fun setChangeThreshold(value: Double) = updateSetup {
        copy(changeThreshold = value.coerceIn(0.01, 0.5))
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            runCatching { libraryRepository.createFolder(name) }
                .onSuccess { folder -> _uiState.update { it.copy(selectedFolderId = folder.id, status = "Folder created.") } }
                .onFailure { error -> showError(error.message ?: "The folder could not be created.") }
        }
    }

    fun importSyllabus(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = "Importing syllabus locally…", error = null) }
            try {
                val imported = syllabusImporter.import(uri)
                val existing = _uiState.value.syllabi.firstOrNull { it.sha256 == imported.sha256 }
                val syllabus = existing ?: libraryRepository.addSyllabus(
                    title = imported.title,
                    localRelativePath = imported.localRelativePath,
                    sha256 = imported.sha256,
                    extractedText = imported.text,
                )
                _uiState.update {
                    it.copy(
                        selectedSyllabusId = syllabus.id,
                        status = if (existing == null) "Syllabus imported and selected." else "Existing syllabus selected.",
                        error = null,
                    )
                }
            } catch (error: Exception) {
                showError(error.message ?: "The syllabus could not be imported.")
            }
        }
    }

    fun startSession() {
        val state = _uiState.value
        if (!state.canEditSetup) return
        val microphoneGranted = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!microphoneGranted) {
            showError("Microphone permission is required in both voice and video mode.")
            return
        }
        stopRequestedDuringStart = false
        _uiState.update { it.copy(stage = LiveCaptureStage.STARTING, status = "Creating the live note…", error = null) }
        viewModelScope.launch {
            try {
                val selected = _uiState.value
                val existingNote = selected.notes.firstOrNull { it.id == selected.selectedNoteId }
                val note = existingNote
                    ?: noteRepository.createNote(selected.noteTitle.ifBlank { "Live note ${selected.notes.size + 1}" })
                val existingNoteMarkdown = if (existingNote == null) {
                    ""
                } else {
                    renderExistingNoteContext(noteRepository.observeBlocks(note.id).first())
                }
                selected.selectedFolderId?.let { libraryRepository.placeNote(note.id, it) }
                val session = sessionRepository.createSession(
                    CaptureSessionSettings(
                        noteId = note.id,
                        mode = selected.mode,
                        notePolicy = selected.notePolicy,
                        syllabusId = selected.selectedSyllabusId,
                        frameIntervalMs = selected.frameIntervalMs,
                        changeThreshold = selected.changeThreshold,
                    ),
                )
                beginEventLoops()
                frameChangeDetector.reset()
                speakerTracker = PerChunkSpeakerTracker()
                latestPersistedTranscriptIds = emptySet()
                _uiState.update {
                    it.copy(
                        stage = LiveCaptureStage.RUNNING,
                        activeNoteId = note.id,
                        activeSession = session,
                        startedAt = session.startedAt,
                        revision = 0,
                        existingNoteMarkdown = existingNoteMarkdown,
                        generatedMarkdown = "",
                        transcript = emptyList(),
                        latestChunkSpeakerIds = emptyList(),
                        pendingEvents = 0,
                        pendingAudioChunks = 0,
                        pendingFrames = 0,
                        acceptedFrames = 0,
                        skippedFrames = 0,
                        processedFrames = 0,
                        corrections = emptyList(),
                        warnings = emptyList(),
                        status = if (session.mode == CaptureMode.VIDEO.name) {
                            "Listening continuously; changed board frames will be captured automatically."
                        } else {
                            "Listening continuously; notes update after each audio chunk."
                        },
                        error = null,
                    )
                }
                audioRecorder.start(
                    onChunk = ::enqueueAudio,
                    onError = { message ->
                        _uiState.update {
                            it.copy(
                                error = message,
                                status = "Microphone capture stopped unexpectedly; finishing saved work.",
                            )
                        }
                        viewModelScope.launch {
                            persistOperationalWarning("Microphone capture stopped unexpectedly: $message")
                            stopSession()
                        }
                    },
                )
                if (stopRequestedDuringStart) stopSession()
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        stage = LiveCaptureStage.ERROR,
                        status = "The live session could not start.",
                        error = error.message ?: "Unknown session error.",
                    )
                }
            }
        }
    }

    fun stopSession() {
        if (_uiState.value.stage == LiveCaptureStage.STARTING) {
            stopRequestedDuringStart = true
            _uiState.update { it.copy(status = "Start is completing, then capture will stop and save.") }
            return
        }
        if (_uiState.value.stage != LiveCaptureStage.RUNNING || stopJob?.isActive == true) return
        _uiState.update { it.copy(stage = LiveCaptureStage.STOPPING, status = "Finishing queued audio and frames…") }
        stopJob = viewModelScope.launch {
            try {
                // The recorder callback can enqueue one final partial WAV before this returns.
                audioRecorder.stopAndDrain()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                persistOperationalWarning(
                    "The microphone did not drain cleanly; any WAV already completed remains recoverable (${error.message}).",
                )
            } finally {
                audioChannel?.close()
                frameChannel?.close()
            }
            audioProcessorJob?.join()
            frameProcessorJob?.join()
            finishStoppedSession()
        }
    }

    fun onLiveScreenLeaving() {
        when (_uiState.value.stage) {
            LiveCaptureStage.STARTING -> {
                stopRequestedDuringStart = true
                _uiState.update { it.copy(status = "Leaving Live will stop and save as soon as startup completes.") }
            }
            LiveCaptureStage.RUNNING -> stopSession()
            else -> Unit
        }
    }

    fun resetAfterStop() {
        if (_uiState.value.stage !in setOf(LiveCaptureStage.STOPPED, LiveCaptureStage.ERROR)) return
        _uiState.update {
            it.copy(
                stage = LiveCaptureStage.SETUP,
                activeNoteId = null,
                activeSession = null,
                startedAt = null,
                existingNoteMarkdown = "",
                verification = null,
                verifying = false,
                status = "Choose a capture mode and start a note.",
                error = null,
            )
        }
    }

    fun onFrameCaptured(file: File, capturedAt: Long = System.currentTimeMillis()) {
        if (_uiState.value.stage != LiveCaptureStage.RUNNING || _uiState.value.mode != CaptureMode.VIDEO) {
            file.delete()
            return
        }
        trackedFrameFiles.add(file.absolutePath)
        enqueueFrame(QueuedFrame(file, capturedAt))
    }

    fun selectPrimarySpeaker(speakerId: String?) {
        if (_uiState.value.stage != LiveCaptureStage.RUNNING) return
        val focus = speakerTracker.selectManually(speakerId)
        _uiState.update {
            it.copy(
                speakerFocus = focus,
                transcript = it.transcript.map { line ->
                    if (line.id in latestPersistedTranscriptIds) {
                        line.copy(primary = focus.selectedSpeakerId != null && line.speakerId == focus.selectedSpeakerId)
                    } else {
                        line
                    }
                },
                status = if (speakerId == null) {
                    "Automatic focus restored for the latest chunk only."
                } else {
                    "Speaker $speakerId marked only in the latest chunk; labels reset on the next request."
                },
            )
        }
    }

    private fun beginEventLoops() {
        audioProcessorJob?.cancel()
        frameProcessorJob?.cancel()
        audioChannel?.cancel()
        frameChannel?.cancel()

        val newAudioChannel = Channel<QueuedAudioChunk>(capacity = Channel.UNLIMITED)
        val newFrameChannel = Channel(
            capacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            onUndeliveredElement = ::discardQueuedFrame,
        )
        audioChannel = newAudioChannel
        frameChannel = newFrameChannel
        audioProcessorJob = viewModelScope.launch {
            for (chunk in newAudioChannel) {
                try {
                    processAudio(chunk)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    retainAudioWithWarning(
                        chunk,
                        "Audio processing failed after capture; its WAV was kept for recovery (${error.message}).",
                    )
                } finally {
                    decrementAudioPending()
                }
            }
        }
        frameProcessorJob = viewModelScope.launch {
            for (frame in newFrameChannel) {
                try {
                    processFrame(frame.file, frame.capturedAt)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "A board frame could not be processed.",
                            status = "Audio continues; the failed frame remains available for bounded retry.",
                        )
                    }
                } finally {
                    decrementFramePending()
                }
            }
        }
    }

    private fun enqueueAudio(chunk: RecordedAudioChunk) {
        val queued = storeRecoverableAudio(chunk)
        _uiState.update {
            it.copy(
                pendingAudioChunks = it.pendingAudioChunks + 1,
                pendingEvents = it.pendingEvents + 1,
            )
        }
        val channel = audioChannel
        if (channel == null || channel.trySend(queued).isFailure) {
            decrementAudioPending()
            viewModelScope.launch {
                retainAudioWithWarning(
                    queued,
                    "An audio chunk arrived after the processor closed; its WAV was retained locally.",
                )
            }
        }
    }

    private fun enqueueFrame(frame: QueuedFrame) {
        _uiState.update {
            it.copy(
                pendingFrames = it.pendingFrames + 1,
                pendingEvents = it.pendingEvents + 1,
            )
        }
        val channel = frameChannel
        if (channel == null || channel.trySend(frame).isFailure) {
            discardQueuedFrame(frame)
        }
    }

    private fun discardQueuedFrame(frame: QueuedFrame) {
        trackedFrameFiles.remove(frame.file.absolutePath)
        frame.file.delete()
        _uiState.update {
            it.copy(
                pendingFrames = (it.pendingFrames - 1).coerceAtLeast(0),
                pendingEvents = (it.pendingEvents - 1).coerceAtLeast(0),
                skippedFrames = it.skippedFrames + 1,
                status = "A superseded frame was dropped locally; audio capture was unaffected.",
            )
        }
    }

    private suspend fun processAudio(chunk: QueuedAudioChunk) {
        val session = _uiState.value.activeSession ?: return
        _uiState.update { it.copy(status = "Transcribing audio chunk ${formatTimestamp(chunk.offsetMs)}…") }
        val wavBytes = chunk.recoveryFile?.takeIf(File::isFile)?.readBytes()
            ?: chunk.inMemoryWav
            ?: error("The recoverable WAV is missing.")
        val transcription = try {
            transcribeWithRetry(session.id, chunk, wavBytes)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val failure = (error as? AudioTranscriptionException)?.failure
            recordServiceFailure(failure)
            val attempts = if (failure?.retryable == false) {
                "without a retry because the service reported a permanent failure"
            } else {
                "after retries"
            }
            val cause = failure?.describe() ?: error.message.orEmpty()
            retainAudioWithWarning(
                chunk,
                "Audio at ${formatTimestamp(chunk.offsetMs)} could not be transcribed $attempts" +
                    (if (cause.isBlank()) "" else " ($cause)") + "; " +
                    if (chunk.recoveryFile?.isFile == true) "its WAV is retained locally." else "local WAV retention failed.",
            )
            return
        }
        VbDebugLog.log(
            "transcribe",
            "chunk offset=${chunk.offsetMs} durationMs=${chunk.durationMs} " +
                "segments=${transcription.segments.size} bytes=${wavBytes.size}",
        )
        transcription.segments.forEachIndexed { index, segment ->
            VbDebugLog.logText(
                "transcribe",
                "seg[$index] ${segment.startMs}-${segment.endMs} speaker=${segment.speakerId}",
                segment.text,
            )
        }
        if (transcription.segments.isEmpty()) {
            deleteRecoveredAudio(chunk)
            persistOperationalWarning("No clear speech was found in the audio chunk at ${formatTimestamp(chunk.offsetMs)}.")
            return
        }
        val focus = speakerTracker.evaluate(chunk.id, transcription.segments)
        val evidence = transcription.segments.map { segment ->
            val persisted = sessionRepository.appendTranscript(
                session.id,
                NewTranscriptSegment(
                    text = segment.text,
                    startMs = segment.startMs,
                    endMs = segment.endMs,
                    speakerId = segment.speakerId,
                ),
            )
            TranscriptEvidence(
                id = persisted.id,
                speakerId = persisted.speakerId,
                startMs = persisted.startMs,
                endMs = persisted.endMs,
                text = persisted.text,
                isPrimarySpeaker = focus.selectedSpeakerId != null && persisted.speakerId == focus.selectedSpeakerId,
            )
        }
        latestPersistedTranscriptIds = evidence.mapTo(mutableSetOf(), TranscriptEvidence::id)
        _uiState.update { state ->
            state.copy(
                speakerFocus = focus,
                latestChunkSpeakerIds = speakerTracker.currentSpeakerIds(),
                transcript = (state.transcript + evidence.map { item ->
                    LiveTranscriptLine(item.id, item.speakerId, item.startMs, item.text, item.isPrimarySpeaker)
                }).takeLast(80),
            )
        }
        val applied = applyNoteUpdate(
            transcriptEvidence = evidence,
            boardEvidence = null,
            assetLinks = emptyList(),
            primarySpeakerId = focus.selectedSpeakerId,
        )
        if (applied) {
            deleteRecoveredAudio(chunk)
        } else {
            retainAudioWithWarning(
                chunk,
                "The transcript was captured but its structured-note revision did not commit; WAV retained locally.",
            )
        }
    }

    private suspend fun processFrame(file: File, capturedAt: Long) {
        val state = _uiState.value
        val session = state.activeSession ?: return deleteTrackedFrame(file)
        val bytes = file.readBytes()
        val decision = frameChangeDetector.evaluate(bytes, state.changeThreshold)
        VbDebugLog.log(
            "frame",
            "accepted=${decision.accepted} score=${"%.4f".format(decision.score)} " +
                "threshold=${state.changeThreshold} fp=${decision.fingerprint} reason=${decision.reason}",
        )
        _uiState.update { it.copy(lastFrameChangeScore = decision.score) }
        if (!decision.accepted) {
            deleteTrackedFrame(file)
            _uiState.update {
                it.copy(
                    skippedFrames = it.skippedFrames + 1,
                    status = "Similar frame skipped locally; no API or note work used.",
                )
            }
            return
        }
        val relativeTemporaryPath = "cache/${file.name}"
        val evidence = try {
            sessionRepository.addVisualEvidence(
                session.id,
                NewVisualEvidence(
                    capturedAt = capturedAt,
                    fingerprint = decision.fingerprint,
                    deltaScore = decision.score,
                    temporaryPath = relativeTemporaryPath,
                ),
            ).also { storedEvidence ->
                check(
                    sessionRepository.updateVisualEvidenceState(
                        storedEvidence.id,
                        VisualEvidenceState.PROCESSING,
                        temporaryPath = relativeTemporaryPath,
                    ),
                ) { "The changed frame could not enter processing state." }
            }
        } catch (error: Exception) {
            frameChangeDetector.discard(decision)
            trackedFrameFiles.remove(file.absolutePath)
            throw error
        }
        _uiState.update { it.copy(acceptedFrames = it.acceptedFrames + 1, status = "Reading changed board frame…") }
        try {
            val extraction = boardCoordinator.extract(bytes)
            val noteId = state.activeNoteId ?: error("The active note is missing.")
            val assetLinks = extraction.diagramRegions.mapIndexed { index, region ->
                val assetToken = "${evidence.id.take(12)}-${index + 1}"
                val stored = diagramCropper.cropAndStore(bytes, region, noteId, assetToken)
                try {
                    sessionRepository.addAsset(
                        NewNoteAsset(
                            noteId = noteId,
                            evidenceId = evidence.id,
                            kind = NoteAssetKind.DIAGRAM,
                            localRelativePath = stored.relativePath,
                            caption = stored.caption,
                        ),
                    )
                } catch (error: Exception) {
                    File(getApplication<Application>().filesDir, stored.relativePath).delete()
                    throw error
                }
                "![${escapeMarkdown(stored.caption)}](${stored.relativePath})"
            }
            val boardEvidence = extraction.toNoteEvidence(
                evidence.id,
                (capturedAt - session.startedAt).coerceAtLeast(0),
            )
            val applied = applyNoteUpdate(
                transcriptEvidence = emptyList(),
                boardEvidence = boardEvidence,
                assetLinks = assetLinks,
            )
            if (!applied) error("The board note revision could not be committed.")
            check(
                sessionRepository.updateVisualEvidenceState(
                    evidence.id,
                    VisualEvidenceState.PROCESSED,
                    temporaryPath = null,
                ),
            ) { "The processed frame state could not be saved." }
            check(frameChangeDetector.commit(decision)) {
                "The processed frame baseline could not be committed."
            }
            deleteTrackedFrame(file)
            _uiState.update {
                it.copy(
                    processedFrames = it.processedFrames + 1,
                    status = "Changed frame processed; raw frame deleted after note and crops committed.",
                )
            }
        } catch (error: Exception) {
            runCatching {
                sessionRepository.updateVisualEvidenceState(
                    evidence.id,
                    VisualEvidenceState.FAILED,
                    temporaryPath = relativeTemporaryPath,
                    error = error.message,
                )
            }
            frameChangeDetector.discard(decision)
            trackedFrameFiles.remove(file.absolutePath)
            // Failed evidence is retained in cache for a bounded retry window instead of being silently lost.
            throw error
        }
    }

    private suspend fun applyNoteUpdate(
        transcriptEvidence: List<TranscriptEvidence>,
        boardEvidence: BoardNoteEvidence?,
        assetLinks: List<String>,
        primarySpeakerId: String? = null,
    ): Boolean = noteUpdateMutex.withLock {
        val state = _uiState.value
        val session = state.activeSession ?: return@withLock false
        val requestId = UUID.randomUUID().toString()
        val syllabus = state.syllabi.firstOrNull { it.id == session.syllabusId }
        val noteTitle = state.notes.firstOrNull { it.id == state.activeNoteId }?.title.orEmpty()
        val completeNoteMarkdown = composeNoteContext(
            existingNoteMarkdown = state.existingNoteMarkdown,
            currentSessionMarkdown = state.generatedMarkdown,
        )
        val evidenceContext = buildString {
            transcriptEvidence.forEach { segment -> appendLine(segment.text) }
            boardEvidence?.let { board ->
                appendLine(board.summary)
                board.visibleText.forEach { appendLine(it) }
                board.concepts.forEach { appendLine(it) }
                board.equations.forEach { appendLine(it) }
                board.diagramCaptions.forEach { appendLine(it) }
            }
        }
        val result = if (state.notePolicy == CaptureNotePolicy.VERBATIM) {
            NoteRefinement(
                requestId = requestId,
                sessionId = session.id,
                baseRevision = state.revision,
                nextRevision = state.revision + 1,
                title = noteTitle,
                markdown = appendVerbatimEvidence(state.generatedMarkdown, transcriptEvidence, boardEvidence),
                corrections = emptyList(),
                consumedEvidenceIds = transcriptEvidence.map(TranscriptEvidence::id) + listOfNotNull(boardEvidence?.id),
                warnings = emptyList(),
                source = NoteRefinementSource.MOCK,
            )
        } else {
            try {
                noteClient.refine(
                    NoteRefinementRequest(
                        requestId = requestId,
                        sessionId = session.id,
                        baseRevision = state.revision,
                        mode = state.mode,
                        notePolicy = state.notePolicy,
                        primarySpeakerId = primarySpeakerId,
                        syllabusContext = "",
                        existingMarkdown = "",
                        responseMode = NoteRefinementResponseMode.DELTA,
                        noteContext = buildIncrementalNoteContext(
                            title = noteTitle,
                            completeMarkdown = completeNoteMarkdown,
                        ),
                        syllabusExcerpts = selectRelevantSyllabusExcerpts(
                            syllabusText = syllabus?.extractedText.orEmpty(),
                            evidenceText = evidenceContext,
                        ),
                        transcriptSegments = transcriptEvidence,
                        boardEvidence = boardEvidence,
                        noteDetail = state.noteDetail,
                        customInstruction = state.customInstruction,
                    ),
                )
            } catch (error: Exception) {
                VbDebugLog.log(
                    "refine",
                    "FAILED ${error::class.simpleName}: ${error.message} " +
                        "(segments=${transcriptEvidence.size} board=${boardEvidence != null} " +
                        "detail=${state.noteDetail} revision=${state.revision})",
                )
                appendWarning(
                    "AI refinement was unavailable; captured evidence was appended without silent correction (${error.message}).",
                )
                NoteRefinement(
                    requestId = requestId,
                    sessionId = session.id,
                    baseRevision = state.revision,
                    nextRevision = state.revision + 1,
                    title = "",
                    markdown = appendFallbackEvidence(state.generatedMarkdown, transcriptEvidence, boardEvidence),
                    corrections = emptyList(),
                    consumedEvidenceIds = transcriptEvidence.map(TranscriptEvidence::id) + listOfNotNull(boardEvidence?.id),
                    warnings = listOf("Unrefined local fallback; review this section."),
                    source = NoteRefinementSource.MOCK,
                )
            }
        }
        val latest = _uiState.value
        val mergedCorrections = (latest.corrections + result.corrections)
            .distinctBy { correction ->
                listOf(
                    correction.captured,
                    correction.suggested,
                    correction.reason,
                    correction.severity,
                    correction.evidenceIds.joinToString(","),
                ).joinToString("|")
            }
            .takeLast(12)
        val mergedWarnings = (latest.warnings + result.warnings).distinct().takeLast(12)
        val materializedMarkdown = result.materializeMarkdown(state.generatedMarkdown)
        val finalMarkdown = appendReviewAnnotations(
            markdown = appendUniqueAssetLinks(materializedMarkdown, assetLinks),
            corrections = mergedCorrections,
            warnings = mergedWarnings,
        )
        when (
            val update = sessionRepository.applyGeneratedMarkdown(
                sessionId = session.id,
                patchId = result.requestId,
                expectedRevision = state.revision,
                markdown = finalMarkdown,
            )
        ) {
            is GeneratedMarkdownUpdateResult.Apply,
            is GeneratedMarkdownUpdateResult.Duplicate,
            -> {
                _uiState.update {
                    it.copy(
                        revision = update.revision,
                        generatedMarkdown = finalMarkdown,
                        corrections = mergedCorrections,
                        warnings = mergedWarnings,
                        status = "Structured note updated to revision ${update.revision}.",
                    )
                }
                true
            }
            is GeneratedMarkdownUpdateResult.Conflict -> {
                showError("Note update conflict: ${update.reason}")
                false
            }
            is GeneratedMarkdownUpdateResult.Invalid -> {
                showError("Invalid note update: ${update.reason}")
                false
            }
            is GeneratedMarkdownUpdateResult.Missing -> {
                showError(update.reason)
                false
            }
        }
    }

    private fun audioRecoveryRoot(): File =
        File(getApplication<Application>().filesDir, "unrecovered-audio")

    private fun retainedAudioFiles(): List<File> {
        val root = audioRecoveryRoot()
        if (!root.isDirectory) return emptyList()
        return runCatching {
            root.listFiles().orEmpty()
                .flatMap { entry ->
                    when {
                        entry.isDirectory -> entry.listFiles().orEmpty().asList()
                        else -> listOf(entry)
                    }
                }
                .filter { file -> file.isFile && file.extension.equals("wav", ignoreCase = true) }
                .sortedBy(File::lastModified)
        }.getOrDefault(emptyList())
    }

    /**
     * Rebuilds the retained-audio list from disk so the user can review, retry, or delete each file.
     *
     * The session id comes from the containing folder and the chunk id/offset from the file name, so
     * a file written before this build still lists with a derived duration.
     */
    private fun refreshRetainedAudio(): List<RetainedAudioChunk> {
        val previous = _uiState.value.retainedAudio.associateBy(RetainedAudioChunk::path)
        val retained = retainedAudioFiles().map { file ->
            val descriptor = parseRetainedAudioName(file.name)
            val sessionId = file.parentFile
                ?.takeIf { it.name != audioRecoveryRoot().name }
                ?.name
                .orEmpty()
            RetainedAudioChunk(
                id = descriptor.chunkToken,
                sessionId = sessionId,
                offsetMs = descriptor.offsetMs ?: 0,
                durationMs = descriptor.durationMs ?: wavDurationMs(file),
                sizeBytes = file.length(),
                path = file.absolutePath,
                reason = previous[file.absolutePath]?.reason
                    ?: "Its transcript never committed, so the audio is kept instead of discarded.",
            )
        }
        retainedAudioIds.clear()
        retainedAudioIds.addAll(retained.map(RetainedAudioChunk::id))
        _uiState.update { it.copy(retainedAudio = retained, retainedAudioChunks = retained.size) }
        return retained
    }

    private fun surfaceRetainedAudio() {
        val retained = refreshRetainedAudio()
        if (retained.isEmpty()) return
        val warning = "${retained.size} unrecovered audio WAV file(s) from an earlier session remain private on this phone."
        _uiState.update {
            it.copy(
                warnings = (it.warnings + warning).distinct(),
                status = warning,
            )
        }
    }

    /**
     * Re-runs transcription for one retained WAV and appends its evidence to the original note.
     *
     * Recovery deliberately appends a labelled verbatim section instead of re-running AI refinement:
     * the note has moved on since the failure, so a delta cannot be validated against it, and the
     * captured speech must not be silently reinterpreted.
     */
    fun retryRetainedAudio(id: String) {
        if (_uiState.value.retainedAudio.any { it.id == id && it.retrying }) return
        viewModelScope.launch {
            updateRetainedAudio(id) { it.copy(retrying = true) }
            val chunk = _uiState.value.retainedAudio.firstOrNull { it.id == id } ?: return@launch
            val file = File(chunk.path)
            try {
                val session = sessionRepository.findSession(chunk.sessionId)
                    ?: error("The original capture session no longer exists, so this audio can only be deleted.")
                check(file.isFile) { "The retained WAV is no longer on this device." }
                val wavBytes = withContext(Dispatchers.IO) { file.readBytes() }
                val transcription = transcriptionClient.transcribe(
                    sessionId = session.id,
                    chunkId = "recovered-${chunk.id}",
                    offsetMs = chunk.offsetMs,
                    wavBytes = wavBytes,
                )
                if (transcription.segments.isEmpty()) {
                    file.delete()
                    refreshRetainedAudio()
                    appendWarning("Recovered audio at ${formatTimestamp(chunk.offsetMs)} contained no clear speech; the WAV was removed.")
                    return@launch
                }
                val evidence = transcription.segments.map { segment ->
                    val persisted = sessionRepository.appendTranscript(
                        session.id,
                        NewTranscriptSegment(
                            text = segment.text,
                            startMs = segment.startMs,
                            endMs = segment.endMs,
                            speakerId = segment.speakerId,
                        ),
                    )
                    TranscriptEvidence(
                        id = persisted.id,
                        speakerId = persisted.speakerId,
                        startMs = persisted.startMs,
                        endMs = persisted.endMs,
                        text = persisted.text,
                        isPrimarySpeaker = false,
                    )
                }
                val current = sessionRepository.generatedMarkdown(session.id).orEmpty()
                val recovered = appendRecoveredEvidence(current, chunk.offsetMs, evidence)
                val update = sessionRepository.applyGeneratedMarkdown(
                    sessionId = session.id,
                    patchId = "recovered-audio-${chunk.id}",
                    expectedRevision = session.revision,
                    markdown = recovered,
                )
                check(
                    update is GeneratedMarkdownUpdateResult.Apply ||
                        update is GeneratedMarkdownUpdateResult.Duplicate,
                ) { "The recovered transcript could not be saved into the note." }
                file.delete()
                file.parentFile?.takeIf { it.listFiles().isNullOrEmpty() }?.delete()
                refreshRetainedAudio()
                if (_uiState.value.activeSession?.id == session.id) {
                    _uiState.update { it.copy(revision = update.revision, generatedMarkdown = recovered) }
                }
                _uiState.update {
                    it.copy(status = "Recovered ${evidence.size} segment(s) from the audio at ${formatTimestamp(chunk.offsetMs)}.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                recordServiceFailure((error as? AudioTranscriptionException)?.failure)
                updateRetainedAudio(id) {
                    it.copy(retrying = false, reason = error.message ?: "The retry failed; the WAV is still retained.")
                }
                appendWarning("Retained audio recovery failed: ${error.message}")
                return@launch
            }
        }
    }

    /** Permanently removes one retained WAV after the user chooses to discard that evidence. */
    fun deleteRetainedAudio(id: String) {
        val chunk = _uiState.value.retainedAudio.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val file = File(chunk.path)
            file.delete()
            file.parentFile?.takeIf { it.listFiles().isNullOrEmpty() }?.delete()
            refreshRetainedAudio()
            appendWarning(
                "A retained WAV covering ${formatTimestamp(chunk.offsetMs)} was deleted at your request; " +
                    "that audio evidence is gone.",
            )
        }
    }

    private fun updateRetainedAudio(id: String, transform: (RetainedAudioChunk) -> RetainedAudioChunk) {
        _uiState.update { state ->
            state.copy(
                retainedAudio = state.retainedAudio.map { chunk ->
                    if (chunk.id == id) transform(chunk) else chunk
                },
            )
        }
    }

    private fun recordServiceFailure(failure: VoxBoxServiceFailure?) {
        if (failure == null) return
        _uiState.update { it.copy(serviceFailure = failure) }
    }

    private fun storeRecoverableAudio(chunk: RecordedAudioChunk): QueuedAudioChunk {
        val sessionId = _uiState.value.activeSession?.id ?: "orphan"
        val directory = File(audioRecoveryRoot(), safeFileToken(sessionId))
        var temporary: File? = null
        return try {
            if (!directory.exists() && !directory.mkdirs()) error("Recovery audio folder could not be created.")
            val target = File(
                directory,
                retainedAudioFileName(chunk.id, chunk.offsetMs, chunk.durationMs),
            )
            temporary = File(directory, ".${safeFileToken(chunk.id)}.tmp")
            FileOutputStream(temporary).use { output ->
                output.write(chunk.wavBytes)
                output.fd.sync()
            }
            if (target.exists() && !target.delete()) error("Previous recovery WAV could not be replaced.")
            if (!temporary.renameTo(target)) error("Recovery WAV could not be committed.")
            QueuedAudioChunk(
                id = chunk.id,
                offsetMs = chunk.offsetMs,
                durationMs = chunk.durationMs,
                recoveryFile = target,
                inMemoryWav = null,
            )
        } catch (error: Exception) {
            temporary?.delete()
            _uiState.update {
                it.copy(
                    error = "Audio recovery storage is unavailable.",
                    status = "The current WAV remains in memory; fix storage before a long session (${error.message}).",
                )
            }
            QueuedAudioChunk(
                id = chunk.id,
                offsetMs = chunk.offsetMs,
                durationMs = chunk.durationMs,
                recoveryFile = null,
                inMemoryWav = chunk.wavBytes,
            )
        }
    }

    private suspend fun transcribeWithRetry(
        sessionId: String,
        chunk: QueuedAudioChunk,
        wavBytes: ByteArray,
    ): AudioTranscription {
        var lastFailure: Exception? = null
        for (attempt in 0..TRANSCRIPTION_RETRY_DELAYS_MS.size) {
            if (attempt > 0) {
                val delayMs = TRANSCRIPTION_RETRY_DELAYS_MS[attempt - 1]
                _uiState.update {
                    it.copy(
                        status = "Retrying audio at ${formatTimestamp(chunk.offsetMs)} " +
                            "(${attempt + 1}/${TRANSCRIPTION_RETRY_DELAYS_MS.size + 1})…",
                    )
                }
                delay(delayMs)
            }
            try {
                return transcriptionClient.transcribe(
                    sessionId = sessionId,
                    chunkId = chunk.id,
                    offsetMs = chunk.offsetMs,
                    wavBytes = wavBytes,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: AudioTranscriptionException) {
                lastFailure = error
                // An exhausted quota, a rejected credential or a rejected request cannot succeed on
                // a second attempt. Retrying would only delay the warning and retain more audio.
                if (!error.retryable) throw error
            } catch (error: Exception) {
                lastFailure = error
            }
        }
        throw lastFailure ?: IllegalStateException("Audio transcription failed without an error.")
    }

    private suspend fun retainAudioWithWarning(chunk: QueuedAudioChunk, message: String) {
        val retained = chunk.recoveryFile?.isFile == true
        val finalMessage = if (retained) message else "$message No durable WAV file was available."
        if (retained) {
            refreshRetainedAudio()
            updateRetainedAudio(safeFileToken(chunk.id)) { it.copy(reason = finalMessage) }
        }
        persistOperationalWarning(finalMessage)
    }

    private fun deleteRecoveredAudio(chunk: QueuedAudioChunk) {
        chunk.recoveryFile?.delete()
        chunk.recoveryFile?.parentFile?.takeIf { it.listFiles().isNullOrEmpty() }?.delete()
        if (retainedAudioIds.contains(safeFileToken(chunk.id))) refreshRetainedAudio()
    }

    private fun decrementAudioPending() {
        _uiState.update {
            it.copy(
                pendingAudioChunks = (it.pendingAudioChunks - 1).coerceAtLeast(0),
                pendingEvents = (it.pendingEvents - 1).coerceAtLeast(0),
            )
        }
    }

    private fun decrementFramePending() {
        _uiState.update {
            it.copy(
                pendingFrames = (it.pendingFrames - 1).coerceAtLeast(0),
                pendingEvents = (it.pendingEvents - 1).coerceAtLeast(0),
            )
        }
    }

    private suspend fun persistOperationalWarning(message: String) {
        appendWarning(message)
        noteUpdateMutex.withLock {
            val state = _uiState.value
            val session = state.activeSession ?: return@withLock
            val warnings = (state.warnings + message).distinct().takeLast(12)
            val markdown = appendReviewAnnotations(
                markdown = state.generatedMarkdown,
                corrections = state.corrections,
                warnings = warnings,
            )
            when (
                val update = sessionRepository.applyGeneratedMarkdown(
                    sessionId = session.id,
                    patchId = "local-warning-${UUID.randomUUID()}",
                    expectedRevision = state.revision,
                    markdown = markdown,
                )
            ) {
                is GeneratedMarkdownUpdateResult.Apply,
                is GeneratedMarkdownUpdateResult.Duplicate,
                -> _uiState.update {
                    it.copy(
                        revision = update.revision,
                        generatedMarkdown = markdown,
                        warnings = warnings,
                        status = "A capture warning was saved into the note.",
                    )
                }
                is GeneratedMarkdownUpdateResult.Conflict -> showError("Warning save conflict: ${update.reason}")
                is GeneratedMarkdownUpdateResult.Invalid -> showError("Warning could not be saved: ${update.reason}")
                is GeneratedMarkdownUpdateResult.Missing -> showError(update.reason)
            }
        }
    }

    private suspend fun finishStoppedSession() {
        val sessionId = _uiState.value.activeSession?.id
        if (sessionId != null) sessionRepository.stopSession(sessionId)
        audioChannel = null
        frameChannel = null
        audioProcessorJob = null
        frameProcessorJob = null
        stopJob = null
        _uiState.update {
            it.copy(
                stage = LiveCaptureStage.STOPPED,
                pendingEvents = 0,
                pendingAudioChunks = 0,
                pendingFrames = 0,
                status = if (it.retainedAudioChunks > 0) {
                    "Session stopped. ${it.retainedAudioChunks} unrecovered WAV file(s) remain private on this phone."
                } else {
                    "Session stopped. The note, transcript evidence, and diagram crops are saved locally."
                },
            )
        }
        verifyFinishedNote()
    }

    /**
     * One end-of-session check of the finished note's formulas, units and concepts.
     *
     * Findings are appended as a clearly labelled review section through the same revision-guarded
     * path as everything else. The note itself is never rewritten: this is a second opinion on
     * captured evidence, so accepting a suggestion stays the user's decision. A failed check is a
     * warning, never a lost note.
     */
    private fun verifyFinishedNote() {
        val state = _uiState.value
        val session = state.activeSession ?: return
        val markdown = state.generatedMarkdown
        if (markdown.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(verifying = true, status = "Checking formulas and concepts…") }
            try {
                val verification = verificationClient.verify(
                    sessionId = session.id,
                    requestId = "verify-${session.id}",
                    noteMarkdown = markdown,
                    subjectHint = state.notes.firstOrNull { it.id == state.activeNoteId }?.title.orEmpty(),
                )
                _uiState.update { it.copy(verification = verification, verifying = false) }
                if (verification.findings.isEmpty()) {
                    _uiState.update {
                        it.copy(status = "End-of-session check found no problems in the saved note.")
                    }
                    return@launch
                }
                noteUpdateMutex.withLock {
                    val latest = _uiState.value
                    val annotated = appendVerificationFindings(latest.generatedMarkdown, verification)
                    val update = sessionRepository.applyGeneratedMarkdown(
                        sessionId = session.id,
                        patchId = "verification-${session.id}",
                        expectedRevision = latest.revision,
                        markdown = annotated,
                    )
                    when (update) {
                        is GeneratedMarkdownUpdateResult.Apply,
                        is GeneratedMarkdownUpdateResult.Duplicate,
                        -> _uiState.update {
                            it.copy(
                                revision = update.revision,
                                generatedMarkdown = annotated,
                                status = "End-of-session check added ${verification.findings.size} " +
                                    "suggestion(s) for review.",
                            )
                        }
                        else -> appendWarning(
                            "The end-of-session check found ${verification.findings.size} suggestion(s) " +
                                "but they could not be saved into the note.",
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                recordServiceFailure((error as? NoteVerificationException)?.failure)
                _uiState.update { it.copy(verifying = false) }
                appendWarning("The end-of-session check did not run: ${error.message}")
            }
        }
    }

    private fun updateSetup(transform: CaptureSessionUiState.() -> CaptureSessionUiState) {
        if (!_uiState.value.canEditSetup) return
        _uiState.update { it.transform() }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(error = message, status = message) }
    }

    private fun appendWarning(message: String) {
        _uiState.update { it.copy(warnings = (it.warnings + message).distinct().takeLast(12)) }
    }

    private fun deleteTrackedFrame(file: File) {
        trackedFrameFiles.remove(file.absolutePath)
        file.delete()
    }

    private fun cleanupExpiredRawFrames() {
        val cutoff = System.currentTimeMillis() - 30 * 60_000L
        getApplication<Application>().cacheDir.listFiles { file ->
            file.name.startsWith("voxbox-live-") && file.lastModified() < cutoff
        }?.forEach(File::delete)
    }

    override fun onCleared() {
        audioRecorder.cancelImmediately()
        stopJob?.cancel()
        audioProcessorJob?.cancel()
        frameProcessorJob?.cancel()
        audioChannel?.cancel()
        frameChannel?.cancel()
        boardCoordinator.close()
        (transcriptionClient as? java.io.Closeable)?.close()
        (noteClient as? java.io.Closeable)?.close()
        (verificationClient as? java.io.Closeable)?.close()
        super.onCleared()
    }
}

internal val TRANSCRIPTION_RETRY_DELAYS_MS = listOf(750L, 2_000L)

private const val REVIEW_START_MARKER = "<!-- voxbox-review:start -->"
private const val REVIEW_END_MARKER = "<!-- voxbox-review:end -->"

/** Recovery WAV names carry their session offset so a retained file stays reviewable after a relaunch. */
private val RETAINED_AUDIO_NAME = Regex("^(.*)-o(\\d{1,15})-d(\\d{1,15})$")

internal data class RetainedAudioDescriptor(
    val chunkToken: String,
    val offsetMs: Long?,
    val durationMs: Long?,
)

internal fun retainedAudioFileName(chunkId: String, offsetMs: Long, durationMs: Long): String =
    "${safeFileToken(chunkId)}-o${offsetMs.coerceAtLeast(0)}-d${durationMs.coerceAtLeast(0)}.wav"

internal fun parseRetainedAudioName(fileName: String): RetainedAudioDescriptor {
    val stem = fileName.removeSuffix(".wav").removeSuffix(".WAV")
    val match = RETAINED_AUDIO_NAME.matchEntire(stem)
        ?: return RetainedAudioDescriptor(chunkToken = stem, offsetMs = null, durationMs = null)
    return RetainedAudioDescriptor(
        chunkToken = match.groupValues[1],
        offsetMs = match.groupValues[2].toLongOrNull(),
        durationMs = match.groupValues[3].toLongOrNull(),
    )
}

/** Derives a duration for files written before the name carried one. */
internal fun wavDurationMs(file: File): Long {
    val bytes = (file.length() - 44).coerceAtLeast(0)
    // Capture is fixed at 16 kHz mono PCM16, so one second is 32,000 bytes.
    return bytes * 1_000 / 32_000
}

internal fun appendRecoveredEvidence(
    existing: String,
    offsetMs: Long,
    transcript: List<TranscriptEvidence>,
): String {
    val additions = buildList {
        add("## Recovered audio · ${formatTimestamp(offsetMs)}")
        add("> Transcribed later from a retained recording. It was not part of the live structured note.")
        transcript.forEach { segment ->
            val speaker = segment.speakerId?.let { " · $it" }.orEmpty()
            add("- **${formatTimestamp(segment.startMs)}$speaker:** ${segment.text.trim()}")
        }
    }
    return listOf(existing.trim(), additions.joinToString("\n"))
        .filter(String::isNotBlank)
        .joinToString("\n\n")
}

internal fun safeFileToken(value: String): String {
    val sanitized = value.trim()
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .take(96)
    return sanitized.takeUnless { it.isBlank() || it == "." || it == ".." } ?: "unknown"
}

internal fun renderExistingNoteContext(blocks: List<NoteBlockEntity>): String = blocks
    .sortedBy(NoteBlockEntity::position)
    .mapNotNull { block ->
        val content = block.content.trim()
        if (content.isBlank()) return@mapNotNull null
        when (runCatching { NoteBlockType.valueOf(block.type) }.getOrNull()) {
            NoteBlockType.HEADING -> "## ${content.trimStart('#').trim()}"
            NoteBlockType.BULLET_POINT -> "- ${content.removePrefix("- ").trim()}"
            NoteBlockType.PIE_CHART -> {
                val label = block.label?.trim()?.takeIf(String::isNotBlank) ?: content
                block.chartValue?.let { "- **$label:** ${it.coerceIn(0, 100)}%" } ?: "- $label"
            }
            NoteBlockType.PARAGRAPH,
            NoteBlockType.MARKDOWN,
            null,
            -> content
        }
    }
    .joinToString("\n\n")

internal fun composeNoteContext(
    existingNoteMarkdown: String,
    currentSessionMarkdown: String,
): String {
    val existing = existingNoteMarkdown.trim()
    val current = currentSessionMarkdown.trim()
    return when {
        existing.isBlank() -> current
        current.isBlank() -> existing
        current.contains(existing) -> current
        else -> "$existing\n\n$current"
    }
}

internal fun appendReviewAnnotations(
    markdown: String,
    corrections: List<SuggestedCorrection>,
    warnings: List<String>,
): String {
    val base = stripReviewAnnotations(markdown).trim()
    val uniqueWarnings = warnings.map(::reviewLine).filter(String::isNotBlank).distinct()
    val uniqueCorrections = corrections.distinctBy { correction ->
        listOf(
            correction.captured,
            correction.suggested,
            correction.reason,
            correction.severity,
            correction.evidenceIds.joinToString(","),
        ).joinToString("|")
    }
    if (uniqueWarnings.isEmpty() && uniqueCorrections.isEmpty()) return base
    val review = buildList {
        add(REVIEW_START_MARKER)
        add("## Review flags")
        if (uniqueWarnings.isNotEmpty()) {
            add("### Warnings")
            uniqueWarnings.forEach { add("- $it") }
        }
        if (uniqueCorrections.isNotEmpty()) {
            add("### Suggested corrections")
            uniqueCorrections.forEach { correction ->
                add("- **Captured:** ${reviewLine(correction.captured)}")
                add("  - **Suggested:** ${reviewLine(correction.suggested)}")
                add("  - **Reason:** ${reviewLine(correction.reason)}")
                correction.severity.trim().takeIf(String::isNotBlank)?.let {
                    add("  - **Severity:** ${reviewLine(it)}")
                }
                if (correction.evidenceIds.isNotEmpty()) {
                    add("  - **Evidence:** ${correction.evidenceIds.joinToString(", ") { reviewLine(it) }}")
                }
            }
        }
        add(REVIEW_END_MARKER)
    }.joinToString("\n")
    return listOf(base, review).filter(String::isNotBlank).joinToString("\n\n")
}

private fun stripReviewAnnotations(markdown: String): String {
    var result = markdown
    while (true) {
        val start = result.indexOf(REVIEW_START_MARKER)
        if (start < 0) return result
        val end = result.indexOf(REVIEW_END_MARKER, start + REVIEW_START_MARKER.length)
        if (end < 0) return result
        result = (
            result.substring(0, start).trimEnd() + "\n\n" +
                result.substring(end + REVIEW_END_MARKER.length).trimStart()
            ).trim()
    }
}

private fun reviewLine(value: String): String = value
    .lineSequence()
    .map(String::trim)
    .filter(String::isNotBlank)
    .joinToString(" ")
    .replace("<!--", "&lt;!--")

private fun BoardExtraction.toNoteEvidence(id: String, capturedAt: Long): BoardNoteEvidence = BoardNoteEvidence(
    id = id,
    capturedAtMs = capturedAt,
    summary = summary,
    visibleText = visibleText.lineSequence().map(String::trim).filter(String::isNotBlank).toList(),
    concepts = concepts,
    equations = equations,
    diagramCaptions = diagramRegions.map { it.caption.trim() }.filter(String::isNotBlank),
)

internal fun appendVerbatimEvidence(
    existing: String,
    transcript: List<TranscriptEvidence>,
    board: BoardNoteEvidence?,
): String {
    val additions = buildList {
        if (transcript.isNotEmpty()) {
            if (!existing.contains("## Verbatim transcript")) add("## Verbatim transcript")
            transcript.forEach { segment ->
                val speaker = segment.speakerId?.let { " · $it" }.orEmpty()
                add("- **${formatTimestamp(segment.startMs)}$speaker:** ${segment.text.trim()}")
            }
        }
        board?.let { evidence ->
            add("## Board evidence · ${formatTimestamp(evidence.capturedAtMs)}")
            evidence.visibleText.forEach { add("> ${it.trim()}") }
            evidence.equations.forEach { add("- \$${it.trim()}\$") }
        }
    }
    return listOf(existing.trim(), additions.joinToString("\n")).filter(String::isNotBlank).joinToString("\n\n")
}

internal fun appendFallbackEvidence(
    existing: String,
    transcript: List<TranscriptEvidence>,
    board: BoardNoteEvidence?,
): String {
    val additions = buildList {
        add("## Captured evidence (needs review)")
        transcript.forEach { segment ->
            add("- **${formatTimestamp(segment.startMs)}:** ${segment.text.trim()}")
        }
        board?.summary?.trim()?.takeIf(String::isNotBlank)?.let { add(it) }
        board?.concepts?.forEach { add("- ${it.trim()}") }
        board?.visibleText?.forEach { add("> ${it.trim()}") }
        board?.equations?.forEach { add("- \$${it.trim()}\$") }
    }
    return listOf(existing.trim(), additions.joinToString("\n")).filter(String::isNotBlank).joinToString("\n\n")
}

private fun appendUniqueAssetLinks(markdown: String, links: List<String>): String {
    val missing = links.filterNot(markdown::contains)
    return if (missing.isEmpty()) markdown else listOf(markdown.trim(), "## Captured diagrams\n${missing.joinToString("\n\n")}")
        .filter(String::isNotBlank)
        .joinToString("\n\n")
}

private fun escapeMarkdown(value: String): String = value.replace("[", "\\[").replace("]", "\\]")

private fun formatTimestamp(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun synchronizedSetOf(): MutableSet<String> =
    java.util.Collections.synchronizedSet(mutableSetOf<String>())
