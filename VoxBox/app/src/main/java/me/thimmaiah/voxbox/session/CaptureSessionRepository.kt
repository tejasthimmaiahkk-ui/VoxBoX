package me.thimmaiah.voxbox.session

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import me.thimmaiah.voxbox.notes.BlockProvenanceEntity
import me.thimmaiah.voxbox.notes.CaptureSessionDao
import me.thimmaiah.voxbox.notes.CaptureSessionEntity
import me.thimmaiah.voxbox.notes.NoteAssetEntity
import me.thimmaiah.voxbox.notes.NoteBlockEntity
import me.thimmaiah.voxbox.notes.NoteBlockType
import me.thimmaiah.voxbox.notes.TranscriptSegmentEntity
import me.thimmaiah.voxbox.notes.VisualEvidenceEntity

interface CaptureSessionRepository {
    fun observeSessions(): Flow<List<CaptureSessionEntity>>
    fun observeSessionsForNote(noteId: String): Flow<List<CaptureSessionEntity>>
    fun observeTranscript(sessionId: String): Flow<List<TranscriptSegmentEntity>>
    fun observeVisualEvidence(sessionId: String): Flow<List<VisualEvidenceEntity>>
    fun observeAssets(noteId: String): Flow<List<NoteAssetEntity>>

    /** Every captured utterance behind a note, in lecture order, for the captured-evidence export. */
    suspend fun transcriptForNote(noteId: String): List<TranscriptSegmentEntity>
    suspend fun createSession(settings: CaptureSessionSettings): CaptureSessionEntity

    /** Loads a session that is not the currently active one, for retained-audio recovery. */
    suspend fun findSession(sessionId: String): CaptureSessionEntity?

    /** Current content of a session's stable generated Markdown block. */
    suspend fun generatedMarkdown(sessionId: String): String?
    suspend fun stopSession(sessionId: String): Boolean
    suspend fun resumeSession(sessionId: String): Boolean
    suspend fun appendTranscript(
        sessionId: String,
        segment: NewTranscriptSegment,
    ): TranscriptSegmentEntity
    suspend fun addVisualEvidence(
        sessionId: String,
        evidence: NewVisualEvidence,
    ): VisualEvidenceEntity
    suspend fun updateVisualEvidenceState(
        evidenceId: String,
        state: VisualEvidenceState,
        temporaryPath: String? = null,
        error: String? = null,
    ): Boolean
    suspend fun addAsset(asset: NewNoteAsset): NoteAssetEntity
    suspend fun applyGeneratedMarkdown(
        sessionId: String,
        patchId: String,
        expectedRevision: Long,
        markdown: String,
    ): GeneratedMarkdownUpdateResult
}

class RoomCaptureSessionRepository(
    private val dao: CaptureSessionDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : CaptureSessionRepository {
    override fun observeSessions(): Flow<List<CaptureSessionEntity>> = dao.observeSessions()

    override fun observeSessionsForNote(noteId: String): Flow<List<CaptureSessionEntity>> =
        dao.observeSessionsForNote(noteId)

    override fun observeTranscript(sessionId: String): Flow<List<TranscriptSegmentEntity>> =
        dao.observeTranscript(sessionId)

    override fun observeVisualEvidence(sessionId: String): Flow<List<VisualEvidenceEntity>> =
        dao.observeVisualEvidence(sessionId)

    override fun observeAssets(noteId: String): Flow<List<NoteAssetEntity>> = dao.observeAssets(noteId)

    override suspend fun transcriptForNote(noteId: String): List<TranscriptSegmentEntity> =
        dao.transcriptForNote(noteId)

    override suspend fun createSession(settings: CaptureSessionSettings): CaptureSessionEntity {
        val now = clock()
        val sessionId = idFactory()
        val generatedBlockId = idFactory()
        val session = CaptureSessionEntity(
            id = sessionId,
            noteId = settings.noteId.trim(),
            mode = settings.mode.name,
            notePolicy = settings.notePolicy.name,
            status = CaptureSessionStatus.RUNNING.name,
            syllabusId = settings.syllabusId?.trim()?.takeIf(String::isNotBlank),
            generatedBlockId = generatedBlockId,
            revision = 0,
            lastPatchId = null,
            frameIntervalMs = settings.frameIntervalMs,
            changeThreshold = settings.changeThreshold,
            primarySpeakerId = null,
            startedAt = now,
            stoppedAt = null,
            updatedAt = now,
        )
        val generatedBlock = NoteBlockEntity(
            id = generatedBlockId,
            noteId = session.noteId,
            position = -1,
            type = NoteBlockType.MARKDOWN.name,
            content = "",
        )
        val provenance = BlockProvenanceEntity(
            blockId = generatedBlockId,
            sessionId = sessionId,
            sourceKind = ProvenanceSourceKind.SYNTHESIS.name,
            sourceId = sessionId,
            reviewState = ProvenanceReviewState.UNREVIEWED.name,
        )
        return dao.createSessionWithGeneratedBlock(session, generatedBlock, provenance)
    }

    override suspend fun findSession(sessionId: String): CaptureSessionEntity? =
        dao.getSession(sessionId.trim())

    override suspend fun generatedMarkdown(sessionId: String): String? {
        val session = dao.getSession(sessionId.trim()) ?: return null
        return dao.getBlock(session.generatedBlockId)
            ?.takeIf { it.noteId == session.noteId && it.type == NoteBlockType.MARKDOWN.name }
            ?.content
    }

    override suspend fun stopSession(sessionId: String): Boolean =
        dao.stopSession(sessionId.trim(), clock())

    override suspend fun resumeSession(sessionId: String): Boolean =
        dao.resumeSession(sessionId.trim(), clock())

    override suspend fun appendTranscript(
        sessionId: String,
        segment: NewTranscriptSegment,
    ): TranscriptSegmentEntity {
        val entity = TranscriptSegmentEntity(
            id = idFactory(),
            sessionId = sessionId.trim().also {
                require(it.isNotBlank()) { "Session id cannot be blank." }
            },
            position = -1,
            speakerId = segment.speakerId?.trim()?.takeIf(String::isNotBlank),
            startMs = segment.startMs,
            endMs = segment.endMs,
            text = segment.text.trim(),
            isFinal = segment.isFinal,
            confidence = segment.confidence,
            createdAt = clock(),
        )
        return dao.appendTranscriptSegment(entity)
    }

    override suspend fun addVisualEvidence(
        sessionId: String,
        evidence: NewVisualEvidence,
    ): VisualEvidenceEntity {
        val entity = VisualEvidenceEntity(
            id = idFactory(),
            sessionId = sessionId.trim().also {
                require(it.isNotBlank()) { "Session id cannot be blank." }
            },
            capturedAt = evidence.capturedAt,
            fingerprint = evidence.fingerprint.trim(),
            deltaScore = evidence.deltaScore,
            processingState = VisualEvidenceState.PENDING.name,
            temporaryPath = evidence.temporaryPath.replace('\\', '/').trim(),
        )
        dao.insertVisualEvidence(entity)
        return entity
    }

    override suspend fun updateVisualEvidenceState(
        evidenceId: String,
        state: VisualEvidenceState,
        temporaryPath: String?,
        error: String?,
    ): Boolean {
        temporaryPath?.let(::requireValidRelativePath)
        val finalState = state in setOf(
            VisualEvidenceState.PROCESSED,
            VisualEvidenceState.FAILED,
            VisualEvidenceState.DISCARDED,
        )
        val retainedPath = if (state in setOf(
                VisualEvidenceState.PROCESSED,
                VisualEvidenceState.DISCARDED,
            )
        ) {
            null
        } else {
            temporaryPath?.replace('\\', '/')?.trim()
        }
        return dao.updateVisualEvidenceState(
            evidenceId = evidenceId,
            processingState = state.name,
            temporaryPath = retainedPath,
            clearTemporaryPath = state in setOf(
                VisualEvidenceState.PROCESSED,
                VisualEvidenceState.DISCARDED,
            ),
            error = error?.trim()?.takeIf(String::isNotBlank),
            processedAt = clock().takeIf { finalState },
        ) == 1
    }

    override suspend fun addAsset(asset: NewNoteAsset): NoteAssetEntity {
        val entity = NoteAssetEntity(
            id = idFactory(),
            noteId = asset.noteId.trim(),
            evidenceId = asset.evidenceId?.trim()?.takeIf(String::isNotBlank),
            kind = asset.kind.name,
            localRelativePath = asset.localRelativePath.replace('\\', '/').trim(),
            caption = asset.caption.trim(),
            createdAt = clock(),
        )
        dao.insertAsset(entity)
        return entity
    }

    override suspend fun applyGeneratedMarkdown(
        sessionId: String,
        patchId: String,
        expectedRevision: Long,
        markdown: String,
    ): GeneratedMarkdownUpdateResult = dao.applyGeneratedMarkdownUpdate(
        sessionId = sessionId.trim(),
        patchId = patchId.trim(),
        expectedRevision = expectedRevision,
        markdown = markdown,
        updatedAt = clock(),
    )
}
