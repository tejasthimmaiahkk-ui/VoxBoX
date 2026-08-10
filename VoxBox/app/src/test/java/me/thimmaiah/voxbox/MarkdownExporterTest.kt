package me.thimmaiah.voxbox

import me.thimmaiah.voxbox.notes.NoteAssetEntity
import me.thimmaiah.voxbox.notes.NoteBlockEntity
import me.thimmaiah.voxbox.notes.NoteBlockType
import me.thimmaiah.voxbox.notes.NoteEntity
import me.thimmaiah.voxbox.notes.TranscriptSegmentEntity
import me.thimmaiah.voxbox.notes.formatClock
import me.thimmaiah.voxbox.notes.renderCapturedMarkdown
import me.thimmaiah.voxbox.notes.renderNoteMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownExporterTest {
    @Test
    fun rendersStructuredBlocksAndPortableAssetLinks() {
        val note = NoteEntity("n1", "Limits & Continuity", 1, 2)
        val blocks = listOf(
            NoteBlockEntity("b1", "n1", 0, NoteBlockType.HEADING.name, "Key ideas"),
            NoteBlockEntity("b2", "n1", 1, NoteBlockType.BULLET_POINT.name, "A limit describes approach."),
            NoteBlockEntity(
                "b3",
                "n1",
                2,
                NoteBlockType.MARKDOWN.name,
                "![Graph](note-assets/n1/a.jpg)\n\n**Important**",
            ),
        )
        val asset = NoteAssetEntity("a1", "n1", null, "DIAGRAM", "note-assets/n1/a.jpg", "Graph", 3)

        val markdown = renderNoteMarkdown(
            note,
            blocks,
            listOf(asset),
            mapOf("note-assets/n1/a.jpg" to "assets/diagram-1.jpg"),
        )

        assertTrue(markdown.startsWith("# Limits & Continuity"))
        assertTrue(markdown.contains("## Key ideas"))
        assertTrue(markdown.contains("- A limit describes approach."))
        assertTrue(markdown.contains("![Graph](assets/diagram-1.jpg)"))
        assertFalse(markdown.contains("note-assets/"))
    }

    @Test
    fun replacesMissingPrivateLinksWithPortableWarning() {
        val note = NoteEntity("n1", "Forces", 1, 2)
        val blocks = listOf(
            NoteBlockEntity(
                "b1",
                "n1",
                0,
                NoteBlockType.MARKDOWN.name,
                "![Free-body diagram](note-assets/n1/missing.jpg)",
            ),
        )
        val missing = NoteAssetEntity(
            "a1",
            "n1",
            null,
            "DIAGRAM",
            "note-assets/n1/missing.jpg",
            "Free-body diagram",
            3,
        )

        val markdown = renderNoteMarkdown(note, blocks, listOf(missing))

        assertFalse(markdown.contains("note-assets/"))
        assertTrue(markdown.contains("Diagram unavailable in this export: Free-body diagram"))
        assertTrue(markdown.contains("## Export warnings"))
    }

    // --- captured-evidence export -----------------------------------------------------

    private fun segment(id: String, position: Int, speaker: String?, startMs: Long, text: String) =
        TranscriptSegmentEntity(
            id = id,
            sessionId = "s1",
            position = position,
            speakerId = speaker,
            startMs = startMs,
            endMs = startMs + 4_000,
            text = text,
            isFinal = true,
            createdAt = startMs,
        )

    @Test
    fun capturedExportKeepsSpeechVerbatimWithTimingAndSpeakers() {
        val note = NoteEntity("n1", "Algebra", 1, 2)
        val transcript = listOf(
            segment("t1", 0, "A", 0, "Both terms share a factor of three x."),
            segment("t2", 1, "B", 65_000, "Does the bracket factorise again?"),
            segment("t3", 2, null, 130_000, "  Leading and trailing space is trimmed.  "),
        )

        val captured = renderCapturedMarkdown(note, transcript, listOf("diagram-1.jpg"))

        assertTrue(captured.startsWith("# Algebra — captured evidence"))
        assertTrue(captured.contains("- `00:00` **A** — Both terms share a factor of three x."))
        // A question from another speaker is part of the record and is never dropped here,
        // whatever the refined note chose to do with it.
        assertTrue(captured.contains("- `01:05` **B** — Does the bracket factorise again?"))
        assertTrue(captured.contains("- `02:10` **?** — Leading and trailing space is trimmed."))
        assertTrue(captured.contains("![Captured board](assets/diagram-1.jpg)"))
        assertTrue(captured.contains("no AI rewriting"))
        assertTrue(captured.contains("algebra.md"))
    }

    @Test
    fun capturedExportOmitsSectionsItHasNoEvidenceFor() {
        val note = NoteEntity("n1", "Algebra", 1, 2)

        val captured = renderCapturedMarkdown(note, listOf(segment("t1", 0, "A", 0, "One line.")))

        assertTrue(captured.contains("## Transcript"))
        assertFalse(captured.contains("## Captured board images"))
    }

    @Test
    fun clockFormattingCoversHourLongLectures() {
        assertEquals("00:00", formatClock(0))
        assertEquals("00:09", formatClock(9_400))
        assertEquals("59:59", formatClock(3_599_000))
        assertEquals("75:00", formatClock(4_500_000))
        assertEquals("00:00", formatClock(-1))
    }
}
