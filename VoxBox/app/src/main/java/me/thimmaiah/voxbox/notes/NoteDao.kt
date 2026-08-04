package me.thimmaiah.voxbox.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note_blocks ORDER BY noteId ASC, position ASC")
    fun observeAllBlocks(): Flow<List<NoteBlockEntity>>

    @Query("SELECT * FROM note_blocks WHERE noteId = :noteId ORDER BY position ASC")
    fun observeBlocks(noteId: String): Flow<List<NoteBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBlock(block: NoteBlockEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBlocks(blocks: List<NoteBlockEntity>)

    @Transaction
    suspend fun insertNoteWithBlocks(note: NoteEntity, blocks: List<NoteBlockEntity>) {
        insertNote(note)
        if (blocks.isNotEmpty()) insertBlocks(blocks)
    }

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM note_blocks WHERE noteId = :noteId")
    suspend fun nextBlockPosition(noteId: String): Int

    @Query("UPDATE notes SET updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun touchNote(noteId: String, updatedAt: Long)

    @Query("""
        UPDATE note_blocks
        SET content = :content, chartValue = :chartValue, accentColor = :accentColor, label = :label
        WHERE id = :blockId AND noteId = :noteId
    """)
    suspend fun updateBlockFields(
        noteId: String,
        blockId: String,
        content: String,
        chartValue: Int?,
        accentColor: String?,
        label: String?,
    ): Int

    @Transaction
    suspend fun appendBlock(block: NoteBlockEntity, updatedAt: Long) {
        insertBlock(block)
        touchNote(block.noteId, updatedAt)
    }

    @Transaction
    suspend fun appendBlocks(blocks: List<NoteBlockEntity>, updatedAt: Long) {
        if (blocks.isEmpty()) return
        insertBlocks(blocks)
        touchNote(blocks.first().noteId, updatedAt)
    }

    @Transaction
    suspend fun updateBlock(
        noteId: String,
        blockId: String,
        update: NoteBlockUpdate,
        updatedAt: Long,
    ): Boolean {
        val changed = updateBlockFields(
            noteId = noteId,
            blockId = blockId,
            content = update.content,
            chartValue = update.chartValue,
            accentColor = update.accentColor,
            label = update.label,
        )
        if (changed == 1) touchNote(noteId, updatedAt)
        return changed == 1
    }
}
