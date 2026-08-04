package me.thimmaiah.voxbox.session

import me.thimmaiah.voxbox.notes.NoteBlockEntity
import me.thimmaiah.voxbox.notes.NoteBlockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureNoteFormattingTest {
    @Test
    fun continuingNoteRendersAllExistingBlockTypesIntoContext() {
        val blocks = listOf(
            block(2, NoteBlockType.BULLET_POINT, "Newton's second law"),
            block(0, NoteBlockType.HEADING, "Mechanics"),
            block(1, NoteBlockType.PARAGRAPH, "Motion and forces."),
            block(3, NoteBlockType.MARKDOWN, "\$\$F = ma\$\$"),
        )

        val existing = renderExistingNoteContext(blocks)
        val combined = composeNoteContext(existing, "## Today's lecture\n\nMomentum")

        assertTrue(existing.startsWith("## Mechanics"))
        assertTrue(existing.contains("- Newton's second law"))
        assertTrue(existing.contains("\$\$F = ma\$\$"))
        assertTrue(combined.contains(existing))
        assertTrue(combined.endsWith("Momentum"))
    }

    @Test
    fun reviewWarningsAndCorrectionsArePersistableMarkdownWithoutDuplicateSection() {
        val correction = SuggestedCorrection(
            captured = "F = mv",
            suggested = "F = ma",
            reason = "The board and syllabus agree on Newton's second law.",
            severity = "high",
            evidenceIds = listOf("audio-1", "frame-1"),
        )
        val first = appendReviewAnnotations("# Dynamics", listOf(correction), listOf("Verify the symbol."))
        val second = appendReviewAnnotations(first, listOf(correction), listOf("Verify the symbol."))

        assertEquals(1, Regex("<!-- voxbox-review:start -->").findAll(second).count())
        assertEquals(1, Regex("Verify the symbol\\.").findAll(second).count())
        assertTrue(second.contains("**Captured:** F = mv"))
        assertTrue(second.contains("**Suggested:** F = ma"))
        assertTrue(second.contains("audio-1, frame-1"))
    }

    @Test
    fun safeRecoveryTokenCannotCreatePathSegments() {
        val token = safeFileToken(" ../session/id\\name ")

        assertFalse(token.contains('/'))
        assertFalse(token.contains('\\'))
        assertTrue(token.isNotBlank())
    }

    private fun block(position: Int, type: NoteBlockType, content: String) = NoteBlockEntity(
        id = "block-$position",
        noteId = "note",
        position = position,
        type = type.name,
        content = content,
    )
}
