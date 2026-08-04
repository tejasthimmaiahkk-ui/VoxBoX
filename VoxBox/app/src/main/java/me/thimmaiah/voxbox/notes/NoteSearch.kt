package me.thimmaiah.voxbox.notes

internal fun filterNotesForQuery(
    notes: List<NoteEntity>,
    blocks: List<NoteBlockEntity>,
    query: String,
): List<NoteEntity> {
    val searchTerm = query.trim()
    if (searchTerm.isEmpty()) return notes

    val matchingBlockNoteIds = blocks.asSequence()
        .filter { block ->
            block.content.contains(searchTerm, ignoreCase = true) ||
                block.label?.contains(searchTerm, ignoreCase = true) == true
        }
        .mapTo(mutableSetOf()) { block -> block.noteId }

    return notes.filter { note ->
        note.title.contains(searchTerm, ignoreCase = true) ||
            note.id in matchingBlockNoteIds
    }
}
