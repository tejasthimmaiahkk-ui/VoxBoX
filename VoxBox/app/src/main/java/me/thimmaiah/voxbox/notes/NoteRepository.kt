package me.thimmaiah.voxbox.notes

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface NoteRepository {
    fun observeNotes(): Flow<List<NoteEntity>>
    fun observeBlocks(noteId: String): Flow<List<NoteBlockEntity>>
    suspend fun createNote(title: String): NoteEntity
    suspend fun appendBlock(noteId: String, block: NewNoteBlock)
}

class RoomNoteRepository(
    private val dao: NoteDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : NoteRepository {
    override fun observeNotes(): Flow<List<NoteEntity>> = dao.observeNotes()

    override fun observeBlocks(noteId: String): Flow<List<NoteBlockEntity>> = dao.observeBlocks(noteId)

    override suspend fun createNote(title: String): NoteEntity {
        val now = clock()
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "Untitled note" },
            createdAt = now,
            updatedAt = now,
        )
        dao.insertNote(note)
        return note
    }

    override suspend fun appendBlock(noteId: String, block: NewNoteBlock) {
        val now = clock()
        dao.appendBlock(
            NoteBlockEntity(
                id = UUID.randomUUID().toString(),
                noteId = noteId,
                position = dao.nextBlockPosition(noteId),
                type = block.type.name,
                content = block.content,
                chartValue = block.chartValue,
                accentColor = block.accentColor,
                label = block.label,
            ),
            updatedAt = now,
        )
    }
}
