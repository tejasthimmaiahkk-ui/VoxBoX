package me.thimmaiah.voxbox.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewFlagParserTest {

    private val note = """
        # Algebra

        A limit describes the value a function approaches.

        <!-- voxbox-review:start -->
        ## Review flags
        ### Warnings
        - The board was partly obscured for one frame.
        ### Suggested corrections
        - **Captured:** The derivative of x squared is three x.
          - **Suggested:** The derivative of x squared is two x.
          - **Reason:** Standard power rule gives 2x.
          - **Severity:** warning
          - **Evidence:** segment-4, segment-5
        - **Captured:** Energy is measured in newtons.
          - **Suggested:** Energy is measured in joules.
          - **Reason:** Newtons measure force.
          - **Severity:** warning
        <!-- voxbox-review:end -->
    """.trimIndent()

    @Test
    fun separatesReadableBodyFromDecisions() {
        val parsed = parseNoteForReview(note)

        assertTrue(parsed.body.startsWith("# Algebra"))
        // The markers and the flag text must not survive into the prose, or a suggestion the
        // model made would read as something the lecturer said.
        assertTrue(!parsed.body.contains("voxbox-review"))
        assertTrue(!parsed.body.contains("Suggested:"))
        assertTrue(!parsed.body.contains("Review flags"))
    }

    @Test
    fun readsEveryFieldOfEachCorrection() {
        val parsed = parseNoteForReview(note)

        assertEquals(2, parsed.flags.size)
        val first = parsed.flags[0]
        assertEquals("The derivative of x squared is three x.", first.captured)
        assertEquals("The derivative of x squared is two x.", first.suggested)
        assertEquals("Standard power rule gives 2x.", first.reason)
        assertEquals("warning", first.severity)
        assertEquals(listOf("segment-4", "segment-5"), first.evidenceIds)
    }

    @Test
    fun aCorrectionWithoutEvidenceStillParses() {
        val parsed = parseNoteForReview(note)

        val second = parsed.flags[1]
        assertEquals("Energy is measured in newtons.", second.captured)
        assertEquals(emptyList<String>(), second.evidenceIds)
    }

    @Test
    fun warningsAreKeptSeparateFromCorrections() {
        val parsed = parseNoteForReview(note)

        assertEquals(listOf("The board was partly obscured for one frame."), parsed.warnings)
    }

    @Test
    fun aNoteWithNoReviewSectionIsUnchanged() {
        val plain = "# Algebra\n\nJust a note."

        val parsed = parseNoteForReview(plain)

        assertEquals(plain, parsed.body)
        assertTrue(parsed.flags.isEmpty())
        assertTrue(parsed.warnings.isEmpty())
    }

    @Test
    fun anUnterminatedReviewSectionStillYieldsCleanProse() {
        val truncated = "# Algebra\n\nBody text.\n\n<!-- voxbox-review:start -->\n## Review flags\n" +
            "### Suggested corrections\n- **Captured:** Something\n  - **Suggested:** Something else"

        val parsed = parseNoteForReview(truncated)

        assertEquals("# Algebra\n\nBody text.", parsed.body)
        assertEquals(1, parsed.flags.size)
        assertEquals("Something else", parsed.flags[0].suggested)
    }
}
