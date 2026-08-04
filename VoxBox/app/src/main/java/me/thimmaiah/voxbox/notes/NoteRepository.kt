package me.thimmaiah.voxbox.notes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

interface NoteRepository {
    fun observeNotes(): Flow<List<NoteEntity>>
    fun observeNotes(searchQuery: String): Flow<List<NoteEntity>>
    fun observeBlocks(noteId: String): Flow<List<NoteBlockEntity>>
    suspend fun createNote(title: String): NoteEntity
    suspend fun createNoteWithBlocks(title: String, blocks: List<NewNoteBlock>): NoteEntity
    suspend fun appendBlock(noteId: String, block: NewNoteBlock)
    suspend fun appendBlocks(noteId: String, blocks: List<NewNoteBlock>)
    suspend fun updateBlock(noteId: String, blockId: String, update: NoteBlockUpdate): Boolean
}

class RoomNoteRepository(
    private val dao: NoteDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : NoteRepository {
    override fun observeNotes(): Flow<List<NoteEntity>> = dao.observeNotes()

    override fun observeNotes(searchQuery: String): Flow<List<NoteEntity>> {
        if (searchQuery.isBlank()) return observeNotes()
        return combine(dao.observeNotes(), dao.observeAllBlocks()) { notes, blocks ->
            filterNotesForQuery(notes, blocks, searchQuery)
        }
    }

    override fun observeBlocks(noteId: String): Flow<List<NoteBlockEntity>> = dao.observeBlocks(noteId)

    override suspend fun createNote(title: String): NoteEntity {
        val now = clock()
        val note = newNote(title, now)
        dao.insertNote(note)
        return note
    }

    override suspend fun createNoteWithBlocks(
        title: String,
        blocks: List<NewNoteBlock>,
    ): NoteEntity {
        val now = clock()
        val note = newNote(title, now)
        dao.insertNoteWithBlocks(
            note = note,
            blocks = blocks.mapIndexed { position, block ->
                block.toEntity(noteId = note.id, position = position)
            },
        )
        return note
    }

    override suspend fun appendBlock(noteId: String, block: NewNoteBlock) {
        val now = clock()
        dao.appendBlock(
            block.toEntity(noteId = noteId, position = dao.nextBlockPosition(noteId)),
            updatedAt = now,
        )
    }

    override suspend fun appendBlocks(noteId: String, blocks: List<NewNoteBlock>) {
        if (blocks.isEmpty()) return
        val firstPosition = dao.nextBlockPosition(noteId)
        dao.appendBlocks(
            blocks = blocks.mapIndexed { index, block ->
                block.toEntity(noteId = noteId, position = firstPosition + index)
            },
            updatedAt = clock(),
        )
    }

    override suspend fun updateBlock(noteId: String, blockId: String, update: NoteBlockUpdate): Boolean {
        return dao.updateBlock(noteId, blockId, update, clock())
    }

    private fun newNote(title: String, now: Long) = NoteEntity(
        id = UUID.randomUUID().toString(),
        title = title.trim().ifBlank { "Untitled note" },
        createdAt = now,
        updatedAt = now,
    )

    private fun NewNoteBlock.toEntity(noteId: String, position: Int) = NoteBlockEntity(
        id = UUID.randomUUID().toString(),
        noteId = noteId,
        position = position,
        type = type.name,
        content = content,
        chartValue = chartValue,
        accentColor = accentColor,
        label = label,
    )
}
