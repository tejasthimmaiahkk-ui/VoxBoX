package me.thimmaiah.voxbox.notes

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "note_blocks",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("noteId"), Index(value = ["noteId", "position"], unique = true)],
)
data class NoteBlockEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val position: Int,
    val type: String,
    val content: String,
    val chartValue: Int? = null,
    val accentColor: String? = null,
    val label: String? = null,
)

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["parentId"], name = "index_folders_parentId"),
        Index(
            value = ["parentId", "name"],
            unique = true,
            name = "index_folders_parentId_name",
        ),
    ],
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val parentId: String? = null,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "note_locations",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["folderId"], name = "index_note_locations_folderId")],
)
data class NoteLocationEntity(
    @PrimaryKey val noteId: String,
    val folderId: String,
)

@Entity(
    tableName = "syllabi",
    indices = [Index(value = ["sha256"], unique = true, name = "index_syllabi_sha256")],
)
data class SyllabusEntity(
    @PrimaryKey val id: String,
    val title: String,
    val localRelativePath: String,
    val extractedText: String? = null,
    val sha256: String,
    val importedAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "capture_sessions",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SyllabusEntity::class,
            parentColumns = ["id"],
            childColumns = ["syllabusId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = NoteBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["generatedBlockId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["noteId"], name = "index_capture_sessions_noteId"),
        Index(value = ["syllabusId"], name = "index_capture_sessions_syllabusId"),
        Index(
            value = ["generatedBlockId"],
            unique = true,
            name = "index_capture_sessions_generatedBlockId",
        ),
        Index(value = ["status"], name = "index_capture_sessions_status"),
    ],
)
data class CaptureSessionEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val mode: String,
    val notePolicy: String,
    val status: String,
    val syllabusId: String? = null,
    val generatedBlockId: String,
    val revision: Long,
    val lastPatchId: String? = null,
    val frameIntervalMs: Long,
    val changeThreshold: Double,
    val primarySpeakerId: String? = null,
    val startedAt: Long,
    val stoppedAt: Long? = null,
    val updatedAt: Long,
)

@Entity(
    tableName = "transcript_segments",
    foreignKeys = [
        ForeignKey(
            entity = CaptureSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"], name = "index_transcript_segments_sessionId"),
        Index(
            value = ["sessionId", "position"],
            unique = true,
            name = "index_transcript_segments_sessionId_position",
        ),
    ],
)
data class TranscriptSegmentEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val position: Int,
    val speakerId: String? = null,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val isFinal: Boolean,
    val confidence: Double? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "visual_evidence",
    foreignKeys = [
        ForeignKey(
            entity = CaptureSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"], name = "index_visual_evidence_sessionId"),
        Index(value = ["processingState"], name = "index_visual_evidence_processingState"),
    ],
)
data class VisualEvidenceEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val capturedAt: Long,
    val fingerprint: String,
    val deltaScore: Double,
    val processingState: String,
    val temporaryPath: String? = null,
    val error: String? = null,
    val processedAt: Long? = null,
)

@Entity(
    tableName = "note_assets",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = VisualEvidenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["evidenceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["noteId"], name = "index_note_assets_noteId"),
        Index(value = ["evidenceId"], name = "index_note_assets_evidenceId"),
        Index(
            value = ["localRelativePath"],
            unique = true,
            name = "index_note_assets_localRelativePath",
        ),
    ],
)
data class NoteAssetEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val evidenceId: String? = null,
    val kind: String,
    val localRelativePath: String,
    val caption: String,
    val createdAt: Long,
)

@Entity(
    tableName = "block_provenance",
    foreignKeys = [
        ForeignKey(
            entity = NoteBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CaptureSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"], name = "index_block_provenance_sessionId"),
        Index(value = ["sourceId"], name = "index_block_provenance_sourceId"),
    ],
)
data class BlockProvenanceEntity(
    @PrimaryKey val blockId: String,
    val sessionId: String,
    val sourceKind: String,
    val sourceId: String,
    val startMs: Long? = null,
    val endMs: Long? = null,
    val confidence: Double? = null,
    val reviewState: String,
)
