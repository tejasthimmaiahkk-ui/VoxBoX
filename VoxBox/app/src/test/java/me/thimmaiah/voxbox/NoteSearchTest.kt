package me.thimmaiah.voxbox

import me.thimmaiah.voxbox.notes.NoteBlockEntity
import me.thimmaiah.voxbox.notes.NoteEntity
import me.thimmaiah.voxbox.notes.NoteLibraryUiState
import me.thimmaiah.voxbox.notes.filterNotesForQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteSearchTest {
    @Test
    fun `blank query returns every note in repository order`() {
        val notes = listOf(note("recent", "Recent"), note("older", "Older"))

        assertEquals(notes, filterNotesForQuery(notes, emptyList(), "   "))
    }

    @Test
    fun `search is case insensitive across title content and label`() {
        val notes = listOf(
            note("title", "CALCULUS lecture"),
            note("content", "Monday class"),
            note("label", "Revision"),
            note("unmatched", "Physics"),
        )
        val blocks = listOf(
            block("content-block", "content", content = "calculus derivatives"),
            block("label-block", "label", content = "Pie chart", label = "CaLcUlUs"),
            block("other-block", "unmatched", content = "Mechanics", label = "Forces"),
        )

        val matches = filterNotesForQuery(notes, blocks, "  calculus  ")

        assertEquals(listOf("title", "content", "label"), matches.map { it.id })
    }

    @Test
    fun `search punctuation is treated literally rather than as a wildcard`() {
        val notes = listOf(note("percent", "Results"), note("plain", "Summary"))
        val blocks = listOf(
            block("percent-block", "percent", content = "Accuracy reached 95%"),
            block("plain-block", "plain", content = "Accuracy reached 95 points"),
        )

        val matches = filterNotesForQuery(notes, blocks, "%")

        assertEquals(listOf("percent"), matches.map { it.id })
    }

    @Test
    fun `filtered results do not change the unfiltered note count`() {
        val allNotes = listOf(
            note("calculus", "Calculus"),
            note("physics", "Physics"),
            note("chemistry", "Chemistry"),
        )
        val state = NoteLibraryUiState(
            notes = filterNotesForQuery(allNotes, emptyList(), "physics"),
            allNotes = allNotes,
            searchQuery = "physics",
        )

        assertEquals(listOf("physics"), state.notes.map { it.id })
        assertEquals(3, state.totalNoteCount)
    }

    private fun note(id: String, title: String) = NoteEntity(
        id = id,
        title = title,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun block(
        id: String,
        noteId: String,
        content: String,
        label: String? = null,
    ) = NoteBlockEntity(
        id = id,
        noteId = noteId,
        position = 0,
        type = "PARAGRAPH",
        content = content,
        label = label,
    )
}
