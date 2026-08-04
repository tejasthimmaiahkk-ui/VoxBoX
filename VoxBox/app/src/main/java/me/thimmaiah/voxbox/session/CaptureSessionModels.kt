package me.thimmaiah.voxbox.session

enum class CaptureMode {
    VOICE,
    VIDEO,
}

enum class CaptureNotePolicy {
    RUNNABLE,
    VERBATIM,
}

enum class CaptureSessionStatus {
    RUNNING,
    STOPPED,
}

enum class VisualEvidenceState {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
    DISCARDED,
}

enum class NoteAssetKind {
    DIAGRAM,
    BOARD_CROP,
}

enum class ProvenanceSourceKind {
    SYNTHESIS,
    AUDIO,
    FRAME,
}

enum class ProvenanceReviewState {
    UNREVIEWED,
    CONFIRMED,
    NEEDS_REVIEW,
}

data class CaptureSessionSettings(
    val noteId: String,
    val mode: CaptureMode,
    val notePolicy: CaptureNotePolicy,
    val syllabusId: String? = null,
    val frameIntervalMs: Long = DEFAULT_FRAME_INTERVAL_MS,
    val changeThreshold: Double = DEFAULT_CHANGE_THRESHOLD,
) {
    init {
        require(noteId.isNotBlank()) { "A capture session needs a note id." }
        require(frameIntervalMs in MIN_FRAME_INTERVAL_MS..MAX_FRAME_INTERVAL_MS) {
            "Frame interval must be between 2 and 60 seconds."
        }
        require(changeThreshold.isFinite() && changeThreshold in 0.0..1.0) {
            "Frame change threshold must be between 0 and 1."
        }
    }

    companion object {
        const val MIN_FRAME_INTERVAL_MS = 2_000L
        const val MAX_FRAME_INTERVAL_MS = 60_000L
        const val DEFAULT_FRAME_INTERVAL_MS = 8_000L
        const val DEFAULT_CHANGE_THRESHOLD = 0.08
    }
}

data class NewTranscriptSegment(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val speakerId: String? = null,
    val isFinal: Boolean = true,
    val confidence: Double? = null,
) {
    init {
        require(text.isNotBlank()) { "Transcript text cannot be blank." }
        require(startMs >= 0) { "Transcript start time cannot be negative." }
        require(endMs >= startMs) { "Transcript end time cannot precede its start." }
        require(confidence == null || confidence.isFinite() && confidence in 0.0..1.0) {
            "Transcript confidence must be between 0 and 1."
        }
    }
}

data class NewVisualEvidence(
    val capturedAt: Long,
    val fingerprint: String,
    val deltaScore: Double,
    val temporaryPath: String,
) {
    init {
        require(capturedAt >= 0) { "Capture time cannot be negative." }
        require(fingerprint.isNotBlank()) { "Visual evidence needs a fingerprint." }
        require(deltaScore.isFinite() && deltaScore in 0.0..1.0) {
            "Frame delta score must be between 0 and 1."
        }
        requireValidRelativePath(temporaryPath)
    }
}

data class NewNoteAsset(
    val noteId: String,
    val evidenceId: String?,
    val kind: NoteAssetKind,
    val localRelativePath: String,
    val caption: String,
) {
    init {
        require(noteId.isNotBlank()) { "A note asset needs a note id." }
        requireValidRelativePath(localRelativePath)
    }
}

internal fun requireValidRelativePath(path: String) {
    val normalized = path.replace('\\', '/').trim()
    require(normalized.isNotBlank()) { "A local relative path cannot be blank." }
    require(!normalized.startsWith('/') && ':' !in normalized) {
        "Local file paths must be relative to VoxBox storage."
    }
    require(normalized.split('/').none { it == ".." }) {
        "Local file paths cannot leave VoxBox storage."
    }
}
