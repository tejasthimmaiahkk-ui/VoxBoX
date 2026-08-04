package me.thimmaiah.voxbox.notes

/** A narrow, typed draft used when changing an already persisted block. */
data class NoteBlockEditDraft(
    val blockId: String,
    val type: String,
    val content: String,
    val percentageText: String = "",
    val color: String = "",
    val label: String = "",
)

data class NoteBlockUpdate(
    val content: String,
    val chartValue: Int?,
    val accentColor: String?,
    val label: String?,
)

sealed interface NoteBlockEditValidation {
    data class Valid(val update: NoteBlockUpdate) : NoteBlockEditValidation
    data class Invalid(val reason: String) : NoteBlockEditValidation
}

private val editableTextTypes = setOf(
    NoteBlockType.PARAGRAPH.name,
    NoteBlockType.HEADING.name,
    NoteBlockType.BULLET_POINT.name,
)

private val supportedChartColors = setOf(
    "red", "orange", "yellow", "green", "blue", "purple", "pink", "black", "white",
)

fun NoteBlockEntity.toEditDraftOrNull(): NoteBlockEditDraft? = when (type) {
    in editableTextTypes -> NoteBlockEditDraft(blockId = id, type = type, content = content)
    NoteBlockType.PIE_CHART.name -> {
        val value = chartValue ?: return null
        val color = accentColor?.takeIf { it.isNotBlank() } ?: return null
        val chartLabel = label?.takeIf { it.isNotBlank() } ?: return null
        NoteBlockEditDraft(
            blockId = id,
            type = type,
            content = content,
            percentageText = value.toString(),
            color = color,
            label = chartLabel,
        )
    }
    else -> null
}

fun NoteBlockEditDraft.validate(): NoteBlockEditValidation = when (type) {
    in editableTextTypes -> {
        val text = content.trim()
        if (text.isBlank()) {
            NoteBlockEditValidation.Invalid("Text blocks cannot be empty.")
        } else {
            NoteBlockEditValidation.Valid(NoteBlockUpdate(text, null, null, null))
        }
    }
    NoteBlockType.PIE_CHART.name -> {
        val percentage = percentageText.trim().toIntOrNull()
        val normalizedColor = color.trim().lowercase()
        val chartLabel = label.trim()
        when {
            percentage == null || percentage !in 0..100 ->
                NoteBlockEditValidation.Invalid("Pie-chart percentage must be a whole number from 0 to 100.")
            normalizedColor !in supportedChartColors ->
                NoteBlockEditValidation.Invalid("Choose a supported chart color.")
            chartLabel.isBlank() -> NoteBlockEditValidation.Invalid("Pie charts need a label.")
            else -> NoteBlockEditValidation.Valid(
                NoteBlockUpdate(content, percentage, normalizedColor, chartLabel),
            )
        }
    }
    else -> NoteBlockEditValidation.Invalid("This saved block type cannot be edited yet.")
}
