package me.thimmaiah.voxbox.notes

enum class NoteBlockType {
    PARAGRAPH,
    HEADING,
    BULLET_POINT,
    PIE_CHART,
    MARKDOWN,
}

data class NewNoteBlock(
    val type: NoteBlockType,
    val content: String,
    val chartValue: Int? = null,
    val accentColor: String? = null,
    val label: String? = null,
)
