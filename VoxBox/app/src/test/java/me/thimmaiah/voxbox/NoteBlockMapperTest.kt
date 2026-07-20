package me.thimmaiah.voxbox

import me.thimmaiah.voxbox.notes.NoteBlockType
import me.thimmaiah.voxbox.notes.toNoteBlockOrNull
import me.thimmaiah.voxbox.voxscript.VoxScriptResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteBlockMapperTest {
    @Test
    fun `dictation becomes a paragraph block`() {
        val block = VoxScriptResult.PlainDictation("Capture this idea").toNoteBlockOrNull()

        assertEquals(NoteBlockType.PARAGRAPH, block?.type)
        assertEquals("Capture this idea", block?.content)
    }

    @Test
    fun `heading keeps its typed block kind`() {
        val block = VoxScriptResult.Heading("Results", "Vox heading Results").toNoteBlockOrNull()

        assertEquals(NoteBlockType.HEADING, block?.type)
        assertEquals("Results", block?.content)
    }

    @Test
    fun `pie chart keeps editable visual slots`() {
        val block = VoxScriptResult.PieChart(
            percentage = 25,
            color = "yellow",
            label = "wheat",
            sourceText = "Tejas pie chart 25 percent yellow label wheat",
        ).toNoteBlockOrNull()

        assertEquals(NoteBlockType.PIE_CHART, block?.type)
        assertEquals(25, block?.chartValue)
        assertEquals("yellow", block?.accentColor)
        assertEquals("wheat", block?.label)
    }

    @Test
    fun `invalid commands cannot create persisted blocks`() {
        val block = VoxScriptResult.InvalidCommand("Missing label", "Vox pie chart 25 percent yellow")
            .toNoteBlockOrNull()

        assertNull(block)
    }
}
