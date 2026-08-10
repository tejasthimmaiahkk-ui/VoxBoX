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
        // The tail is deliberately small: it is re-sent on every note update, roughly 180 times
        // an hour, so it only has to be long enough to avoid repeating the preceding sentence.
        assertEquals(3_000, first.recentMarkdown.length)
        assertEquals(64, first.contentSha256.length)
        assertEquals(first.contentSha256, second.contentSha256)
    }

    @Test
    fun forwardedContextStaysWithinAnAffordablePerCallBudget() {
        val markdown = "# Mechanics\n" + (1..400).joinToString("\n") { "## Section $it\n" + "A".repeat(200) }
        val syllabus = (1..40).joinToString("\n") { "# Topic $it\nForce acceleration momentum energy $it." }

        val context = buildIncrementalNoteContext("Physics", markdown)
        val excerpts = selectRelevantSyllabusExcerpts(syllabus, "force acceleration momentum")
        val total = context.outlineMarkdown.length +
            context.recentMarkdown.length +
            excerpts.sumOf { it.text.length }

        // Was ~48,000 characters per call, which is what made a lecture-hour cost $0.18.
        assertTrue("forwarded context was $total characters", total <= 8_000)
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
