package me.thimmaiah.voxbox.notes

/** UI-safe projection of a persisted block for the read-only note-detail view. */
sealed interface ReadOnlyNoteBlock {
    data class Paragraph(val text: String) : ReadOnlyNoteBlock
    data class Heading(val text: String) : ReadOnlyNoteBlock
    data class BulletPoint(val text: String) : ReadOnlyNoteBlock
    data class PieChart(val percentage: Int, val color: String, val label: String) : ReadOnlyNoteBlock
}

fun NoteBlockEntity.toReadOnlyBlockOrNull(): ReadOnlyNoteBlock? {
    return when (type) {
    NoteBlockType.PARAGRAPH.name -> ReadOnlyNoteBlock.Paragraph(content)
    NoteBlockType.HEADING.name -> ReadOnlyNoteBlock.Heading(content)
    NoteBlockType.BULLET_POINT.name -> ReadOnlyNoteBlock.BulletPoint(content)
    NoteBlockType.PIE_CHART.name -> {
        val value = chartValue ?: return null
        val color = accentColor?.takeIf { it.isNotBlank() } ?: return null
        val chartLabel = label?.takeIf { it.isNotBlank() } ?: return null
        if (value !in 0..100) return null
        ReadOnlyNoteBlock.PieChart(value, color, chartLabel)
    }
        else -> null
    }
}
