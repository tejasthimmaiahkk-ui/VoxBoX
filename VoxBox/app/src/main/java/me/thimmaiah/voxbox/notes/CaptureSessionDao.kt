package me.thimmaiah.voxbox.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.thimmaiah.voxbox.session.CaptureSessionStatus
import me.thimmaiah.voxbox.session.GeneratedMarkdownRevision
import me.thimmaiah.voxbox.session.GeneratedMarkdownUpdateResult

@Dao
interface CaptureSessionDao {
    @Query("SELECT * FROM capture_sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<CaptureSessionEntity>>

    @Query("SELECT * FROM capture_sessions WHERE noteId = :noteId ORDER BY updatedAt DESC")
    fun observeSessionsForNote(noteId: String): Flow<List<CaptureSessionEntity>>

    @Query("SELECT * FROM transcript_segments WHERE sessionId = :sessionId ORDER BY position ASC")
    fun observeTranscript(sessionId: String): Flow<List<TranscriptSegmentEntity>>

    @Query("SELECT * FROM visual_evidence WHERE sessionId = :sessionId ORDER BY capturedAt ASC")
    fun observeVisualEvidence(sessionId: String): Flow<List<VisualEvidenceEntity>>

    @Query("SELECT * FROM note_assets WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun observeAssets(noteId: String): Flow<List<NoteAssetEntity>>

    @Query("SELECT * FROM capture_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): CaptureSessionEntity?

    @Query("SELECT * FROM note_blocks WHERE id = :blockId LIMIT 1")
    suspend fun getBlock(blockId: String): NoteBlockEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM note_blocks WHERE noteId = :noteId")
    suspend fun nextBlockPosition(noteId: String): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM transcript_segments WHERE sessionId = :sessionId")
    suspend fun nextTranscriptPosition(sessionId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBlock(block: NoteBlockEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: CaptureSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProvenance(provenance: BlockProvenanceEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTranscript(segment: TranscriptSegmentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVisualEvidence(evidence: VisualEvidenceEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAsset(asset: NoteAssetEntity)

    @Query("UPDATE notes SET updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun touchNote(noteId: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE capture_sessions
        SET status = 'STOPPED', stoppedAt = :stoppedAt, updatedAt = :stoppedAt
        WHERE id = :sessionId AND status = 'RUNNING'
        """,
    )
    suspend fun stopRunningSession(
        sessionId: String,
        stoppedAt: Long,
    ): Int

    @Query(
        """
        UPDATE capture_sessions
        SET status = 'RUNNING', stoppedAt = NULL, updatedAt = :resumedAt
        WHERE id = :sessionId AND status = 'STOPPED'
        """,
    )
    suspend fun resumeStoppedSession(
        sessionId: String,
        resumedAt: Long,
    ): Int

    @Query(
        """
        UPDATE visual_evidence
        SET processingState = :processingState,
            temporaryPath = CASE
                WHEN :clearTemporaryPath = 1 THEN NULL
                ELSE COALESCE(:temporaryPath, temporaryPath)
            END,
            error = :error,
            processedAt = :processedAt
        WHERE id = :evidenceId
        """,
    )
    suspend fun updateVisualEvidenceState(
        evidenceId: String,
        processingState: String,
        temporaryPath: String?,
        clearTemporaryPath: Boolean,
        error: String?,
        processedAt: Long?,
    ): Int

    @Query(
        """
        UPDATE capture_sessions
        SET revision = :newRevision, lastPatchId = :patchId, updatedAt = :updatedAt
        WHERE id = :sessionId AND revision = :expectedRevision
        """,
    )
    suspend fun advanceRevision(
        sessionId: String,
        expectedRevision: Long,
        newRevision: Long,
        patchId: String,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE note_blocks
        SET content = :markdown
        WHERE id = :blockId AND noteId = :noteId AND type = 'MARKDOWN'
        """,
    )
    suspend fun updateGeneratedMarkdownBlock(
        blockId: String,
        noteId: String,
        markdown: String,
    ): Int

    @Transaction
    suspend fun createSessionWithGeneratedBlock(
        session: CaptureSessionEntity,
        generatedBlock: NoteBlockEntity,
        provenance: BlockProvenanceEntity,
    ): CaptureSessionEntity {
        require(session.status == CaptureSessionStatus.RUNNING.name) {
            "A new capture session must start in the running state."
        }
        require(generatedBlock.id == session.generatedBlockId) {
            "The generated block must match the capture session."
        }
        require(generatedBlock.noteId == session.noteId) {
            "The generated block and capture session must belong to the same note."
        }
        require(generatedBlock.type == NoteBlockType.MARKDOWN.name) {
            "Capture sessions require one stable Markdown block."
        }
        require(provenance.blockId == generatedBlock.id && provenance.sessionId == session.id) {
            "Generated-block provenance must match its session."
        }

        val positionedBlock = generatedBlock.copy(position = nextBlockPosition(session.noteId))
        insertBlock(positionedBlock)
        insertSession(session)
        insertProvenance(provenance)
        check(touchNote(session.noteId, session.updatedAt) == 1) {
            "The note for this capture session does not exist."
        }
        return session
    }

    @Transaction
    suspend fun appendTranscriptSegment(segment: TranscriptSegmentEntity): TranscriptSegmentEntity {
        val positioned = segment.copy(position = nextTranscriptPosition(segment.sessionId))
        insertTranscript(positioned)
        return positioned
    }

    @Transaction
    suspend fun applyGeneratedMarkdownUpdate(
        sessionId: String,
        patchId: String,
        expectedRevision: Long,
        markdown: String,
        updatedAt: Long,
    ): GeneratedMarkdownUpdateResult {
        val session = getSession(sessionId)
            ?: return GeneratedMarkdownUpdateResult.Missing(reason = "Capture session was not found.")
        val block = getBlock(session.generatedBlockId)
            ?: return GeneratedMarkdownUpdateResult.Missing(
                revision = session.revision,
                reason = "The generated Markdown block was not found.",
            )
        if (block.noteId != session.noteId || block.type != NoteBlockType.MARKDOWN.name) {
            return GeneratedMarkdownUpdateResult.Missing(
                revision = session.revision,
                reason = "The generated block no longer matches this capture session.",
            )
        }

        val decision = GeneratedMarkdownRevision.decide(
            currentRevision = session.revision,
            lastPatchId = session.lastPatchId,
            currentMarkdown = block.content,
            patchId = patchId,
            expectedRevision = expectedRevision,
            newMarkdown = markdown,
        )
        if (decision !is GeneratedMarkdownUpdateResult.Apply) return decision

        val advanced = advanceRevision(
            sessionId = session.id,
            expectedRevision = expectedRevision,
            newRevision = decision.revision,
            patchId = patchId,
            updatedAt = updatedAt,
        )
        if (advanced != 1) {
            val latestRevision = getSession(session.id)?.revision ?: session.revision
            return GeneratedMarkdownUpdateResult.Conflict(
                revision = latestRevision,
                reason = "The session changed while this patch was being applied.",
            )
        }

        check(
            updateGeneratedMarkdownBlock(
                blockId = session.generatedBlockId,
                noteId = session.noteId,
                markdown = markdown,
            ) == 1,
        ) { "The generated Markdown block could not be updated." }
        check(touchNote(session.noteId, updatedAt) == 1) {
            "The note for this capture session no longer exists."
        }
        return decision
    }

    @Transaction
    suspend fun stopSession(sessionId: String, stoppedAt: Long): Boolean {
        val session = getSession(sessionId) ?: return false
        val stopped = stopRunningSession(sessionId, stoppedAt) == 1
        if (stopped) touchNote(session.noteId, stoppedAt)
        return stopped
    }

    @Transaction
    suspend fun resumeSession(sessionId: String, resumedAt: Long): Boolean {
        val session = getSession(sessionId) ?: return false
        val resumed = resumeStoppedSession(sessionId, resumedAt) == 1
        if (resumed) touchNote(session.noteId, resumedAt)
        return resumed
    }
}
