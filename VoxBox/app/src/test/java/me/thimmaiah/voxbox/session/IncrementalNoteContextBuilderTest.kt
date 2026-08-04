package me.thimmaiah.voxbox.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncrementalNoteContextBuilderTest {
    @Test
    fun buildsBoundedOutlineTailAndStableHash() {
        val markdown = "# Mechanics\n\n## Force\n" + "A".repeat(30_000)

        val first = buildIncrementalNoteContext(" Physics ", markdown)
        val second = buildIncrementalNoteContext("Physics", markdown)

        assertEquals("Physics", first.title)
        assertTrue(first.outlineMarkdown.contains("# Mechanics"))
        assertTrue(first.outlineMarkdown.contains("## Force"))
        assertEquals(24_000, first.recentMarkdown.length)
        assertEquals(64, first.contentSha256.length)
        assertEquals(first.contentSha256, second.contentSha256)
    }

    @Test
    fun selectsRelevantBoundedSyllabusSections() {
        val syllabus = """
            # Cell biology
            Mitochondria and respiration.
            # Mechanics
            Newton force acceleration and momentum.
            # Optics
            Refraction reflection and lenses.
        """.trimIndent()

        val excerpts = selectRelevantSyllabusExcerpts(
            syllabusText = syllabus,
            evidenceText = "The teacher derives Newton's force and acceleration equation.",
        )

        assertEquals("Mechanics", excerpts.first().heading)
        assertTrue(excerpts.sumOf { it.text.length } <= 12_000)
        assertTrue(excerpts.all { it.text.length <= 2_000 })
    }
}
