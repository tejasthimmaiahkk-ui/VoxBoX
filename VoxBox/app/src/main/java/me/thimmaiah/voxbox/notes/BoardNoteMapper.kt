package me.thimmaiah.voxbox.notes

data class BoardNoteContent(
    val title: String,
    val summary: String,
    val visibleText: String,
    val concepts: List<String>,
) {
    fun normalizedTitle(): String = title.trim().ifBlank { "Board capture" }
}

fun BoardNoteContent.toNoteBlocks(): List<NewNoteBlock> = buildList {
    add(NewNoteBlock(NoteBlockType.HEADING, normalizedTitle()))

    summary.trim().takeIf(String::isNotBlank)?.let { cleanSummary ->
        add(NewNoteBlock(NoteBlockType.PARAGRAPH, cleanSummary))
    }

    val cleanConcepts = concepts
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy(String::lowercase)
        .take(8)
    if (cleanConcepts.isNotEmpty()) {
        add(NewNoteBlock(NoteBlockType.HEADING, "Key concepts"))
        cleanConcepts.forEach { concept ->
            add(NewNoteBlock(NoteBlockType.BULLET_POINT, concept))
        }
    }

    visibleText.trim().takeIf(String::isNotBlank)?.let { cleanVisibleText ->
        add(NewNoteBlock(NoteBlockType.HEADING, "Captured board text"))
        add(NewNoteBlock(NoteBlockType.PARAGRAPH, cleanVisibleText))
    }
}
