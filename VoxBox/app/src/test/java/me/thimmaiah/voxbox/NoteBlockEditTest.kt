package me.thimmaiah.voxbox

import me.thimmaiah.voxbox.notes.NoteBlockEditDraft
import me.thimmaiah.voxbox.notes.NoteBlockEditValidation
import me.thimmaiah.voxbox.notes.NoteBlockType
import me.thimmaiah.voxbox.notes.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBlockEditTest {
    @Test
    fun `text edit trims and preserves text block semantics`() {
        val result = NoteBlockEditDraft(
            blockId = "block-1", type = NoteBlockType.HEADING.name, content = "  Results  ",
        ).validate()

        assertTrue(result is NoteBlockEditValidation.Valid)
        assertEquals("Results", (result as NoteBlockEditValidation.Valid).update.content)
        assertEquals(null, result.update.chartValue)
    }

    @Test
    fun `pie edit keeps validated typed slots`() {
        val result = NoteBlockEditDraft(
            blockId = "block-2", type = NoteBlockType.PIE_CHART.name, content = "original command",
            percentageText = "40", color = "Blue", label = "  Revision  ",
        ).validate()

        assertTrue(result is NoteBlockEditValidation.Valid)
        val update = (result as NoteBlockEditValidation.Valid).update
        assertEquals(40, update.chartValue)
        assertEquals("blue", update.accentColor)
        assertEquals("Revision", update.label)
    }

    @Test
    fun `pie edit rejects an unsupported color`() {
        val result = NoteBlockEditDraft(
            blockId = "block-3", type = NoteBlockType.PIE_CHART.name, content = "original command",
            percentageText = "40", color = "teal", label = "Revision",
        ).validate()

        assertTrue(result is NoteBlockEditValidation.Invalid)
    }
}
