package me.thimmaiah.voxbox

import me.thimmaiah.voxbox.notes.BoardNoteContent
import me.thimmaiah.voxbox.notes.NoteBlockType
import me.thimmaiah.voxbox.notes.toNoteBlocks
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardNoteMapperTest {
    @Test
    fun captureBecomesStructuredReviewableBlocks() {
        val blocks = BoardNoteContent(
            title = " Photosynthesis ",
            summary = "Plants convert light into chemical energy.",
            visibleText = "6CO2 + 6H2O -> C6H12O6 + 6O2",
            concepts = listOf("Chlorophyll", "Light reactions", "chlorophyll", " "),
        ).toNoteBlocks()

        assertEquals(
            listOf(
                NoteBlockType.HEADING,
                NoteBlockType.PARAGRAPH,
                NoteBlockType.HEADING,
                NoteBlockType.BULLET_POINT,
                NoteBlockType.BULLET_POINT,
                NoteBlockType.HEADING,
                NoteBlockType.PARAGRAPH,
            ),
            blocks.map { it.type },
        )
        assertEquals("Photosynthesis", blocks.first().content)
        assertEquals("Chlorophyll", blocks[3].content)
    }

    @Test
    fun blankCaptureStillCreatesNamedHeading() {
        val blocks = BoardNoteContent("", "", "", emptyList()).toNoteBlocks()

        assertEquals(1, blocks.size)
        assertEquals(NoteBlockType.HEADING, blocks.single().type)
        assertEquals("Board capture", blocks.single().content)
    }
}
