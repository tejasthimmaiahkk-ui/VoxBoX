package me.thimmaiah.voxbox.notes

import me.thimmaiah.voxbox.voxscript.VoxScriptResult

enum class NoteBlockType {
    PARAGRAPH,
    HEADING,
    BULLET_POINT,
    PIE_CHART,
}

data class NewNoteBlock(
    val type: NoteBlockType,
    val content: String,
    val chartValue: Int? = null,
    val accentColor: String? = null,
    val label: String? = null,
)

fun VoxScriptResult.toNoteBlockOrNull(): NewNoteBlock? = when (this) {
    is VoxScriptResult.PlainDictation -> NewNoteBlock(NoteBlockType.PARAGRAPH, sourceText)
    is VoxScriptResult.Heading -> NewNoteBlock(NoteBlockType.HEADING, text)
    is VoxScriptResult.BulletPoint -> NewNoteBlock(NoteBlockType.BULLET_POINT, text)
    is VoxScriptResult.PieChart -> NewNoteBlock(
        type = NoteBlockType.PIE_CHART,
        content = sourceText,
        chartValue = percentage,
        accentColor = color,
        label = label,
    )
    is VoxScriptResult.InvalidCommand -> null
}
