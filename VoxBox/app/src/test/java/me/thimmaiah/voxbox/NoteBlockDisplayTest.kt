package me.thimmaiah.voxbox

import me.thimmaiah.voxbox.notes.NoteBlockEntity
import me.thimmaiah.voxbox.notes.ReadOnlyNoteBlock
import me.thimmaiah.voxbox.notes.toReadOnlyBlockOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteBlockDisplayTest {
    @Test
    fun `saved pie chart reopens with its visual slots`() {
        val result = NoteBlockEntity(
            id = "block-1", noteId = "note-1", position = 0, type = "PIE_CHART",
            content = "Tejas pie chart 25 percent yellow label wheat",
            chartValue = 25, accentColor = "yellow", label = "wheat",
        ).toReadOnlyBlockOrNull()

        assertEquals(ReadOnlyNoteBlock.PieChart(25, "yellow", "wheat"), result)
    }

    @Test
    fun `malformed saved chart is not rendered as another block`() {
        val result = NoteBlockEntity(
            id = "block-2", noteId = "note-1", position = 1, type = "PIE_CHART",
            content = "broken chart", chartValue = 125, accentColor = "yellow", label = "wheat",
        ).toReadOnlyBlockOrNull()

        assertNull(result)
    }
}
