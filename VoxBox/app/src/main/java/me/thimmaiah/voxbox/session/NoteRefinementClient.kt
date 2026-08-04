package me.thimmaiah.voxbox.session

data class TranscriptEvidence(
    val id: String,
    val speakerId: String?,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val isPrimarySpeaker: Boolean,
)

data class BoardNoteEvidence(
    val id: String,
    val capturedAtMs: Long,
    val summary: String,
    val visibleText: List<String>,
    val concepts: List<String>,
    val equations: List<String>,
    val diagramCaptions: List<String>,
)

enum class NoteRefinementResponseMode {
    FULL,
    DELTA,
}

data class IncrementalNoteContext(
    val title: String,
    val outlineMarkdown: String,
    val recentMarkdown: String,
    val contentSha256: String,
)

data class SyllabusContextExcerpt(
    val id: String,
    val heading: String,
    val text: String,
)

data class NoteRefinementRequest(
    val requestId: String,
    val sessionId: String,
    val baseRevision: Long,
    val mode: CaptureMode,
    val notePolicy: CaptureNotePolicy,
    val primarySpeakerId: String?,
    val syllabusContext: String,
    val existingMarkdown: String,
    val transcriptSegments: List<TranscriptEvidence>,
    val boardEvidence: BoardNoteEvidence?,
    val responseMode: NoteRefinementResponseMode = NoteRefinementResponseMode.FULL,
    val noteContext: IncrementalNoteContext? = null,
    val syllabusExcerpts: List<SyllabusContextExcerpt> = emptyList(),
)

enum class NoteRefinementSource {
    OPENAI,
    MOCK,
}

enum class NoteRefinementUpdateMode {
    FULL,
    DELTA,
}

data class SuggestedCorrection(
    val captured: String,
    val suggested: String,
    val reason: String,
    val severity: String,
    val evidenceIds: List<String>,
)

data class NoteRefinement(
    val requestId: String,
    val sessionId: String,
    val baseRevision: Long,
    val nextRevision: Long,
    val title: String,
    val markdown: String,
    val corrections: List<SuggestedCorrection>,
    val consumedEvidenceIds: List<String>,
    val warnings: List<String>,
    val source: NoteRefinementSource,
    val updateMode: NoteRefinementUpdateMode = NoteRefinementUpdateMode.FULL,
    val baseContentSha256: String = "",
    val markdownDelta: String = "",
)

/** Applies a validated response while keeping the legacy full-document response contract intact. */
fun NoteRefinement.materializeMarkdown(existingMarkdown: String): String = when (updateMode) {
    NoteRefinementUpdateMode.FULL -> markdown
    NoteRefinementUpdateMode.DELTA -> listOf(existingMarkdown.trimEnd(), markdownDelta.trim())
        .filter(String::isNotBlank)
        .joinToString("\n\n")
}

interface NoteRefinementClient {
    suspend fun refine(request: NoteRefinementRequest): NoteRefinement
}
