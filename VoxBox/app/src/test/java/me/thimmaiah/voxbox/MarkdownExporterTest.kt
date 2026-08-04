package me.thimmaiah.voxbox

import me.thimmaiah.voxbox.notes.NoteAssetEntity
import me.thimmaiah.voxbox.notes.NoteBlockEntity
import me.thimmaiah.voxbox.notes.NoteBlockType
import me.thimmaiah.voxbox.notes.NoteEntity
import me.thimmaiah.voxbox.notes.renderNoteMarkdown
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
}
