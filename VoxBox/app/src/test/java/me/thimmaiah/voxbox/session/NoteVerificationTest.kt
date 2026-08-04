package me.thimmaiah.voxbox.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteVerificationTest {
    private fun response(findings: String) = """
        {
          "sessionId":"s1",
          "requestId":"verify-1",
          "findings":[$findings],
          "checkedFormulas":["v = u + at"],
          "checkedConcepts":["kinematics"],
          "warnings":[],
          "source":"openrouter"
        }
    """.trimIndent()

    private val validFinding = """
        {
          "claim":"Energy is measured in newtons.",
          "issue":"Newtons measure force, not energy.",
          "suggestion":"Energy is measured in joules.",
          "kind":"units",
          "severity":"warning",
          "confidence":0.99
        }
    """.trimIndent()

    @Test
    fun parsesFindingsAndTheCheckedInventory() {
        val result = parseNoteVerificationResponse(response(validFinding))

        assertEquals("s1", result.sessionId)
        assertEquals(NoteRefinementSource.PROVIDER, result.source)
        assertEquals(1, result.findings.size)
        assertEquals(1, result.warningCount)
        val finding = result.findings.single()
        assertEquals(VerificationFindingKind.UNITS, finding.kind)
        assertEquals(0.99, finding.confidence, 1e-9)
        assertEquals(listOf("v = u + at"), result.checkedFormulas)
        assertEquals(listOf("kinematics"), result.checkedConcepts)
    }

    @Test
    fun rejectsOutOfContractSeverityKindAndConfidence() {
        for (bad in listOf(
            validFinding.replace("\"warning\"", "\"critical\""),
            validFinding.replace("\"units\"", "\"vibes\""),
            validFinding.replace("0.99", "1.4"),
            validFinding.replace("\"confidence\":0.99", "\"confidence\":\"high\""),
        )) {
            assertThrows(NoteVerificationException::class.java) {
                parseNoteVerificationResponse(response(bad))
            }
        }
    }

    @Test
    fun findingsAreAppendedAsReviewTextWithoutTouchingTheNote() {
        val note = "# Physics\n\n- Energy is measured in newtons.\n"
        val verification = NoteVerification(
            sessionId = "s1",
            requestId = "verify-1",
            findings = listOf(
                VerificationFinding(
                    claim = "Energy is measured in newtons.",
                    issue = "Newtons measure force.",
                    suggestion = "Energy is measured in joules.",
                    kind = VerificationFindingKind.UNITS,
                    severity = "warning",
                    confidence = 0.99,
                ),
            ),
            checkedFormulas = emptyList(),
            checkedConcepts = emptyList(),
            warnings = emptyList(),
            source = NoteRefinementSource.PROVIDER,
        )

        val annotated = appendVerificationFindings(note, verification)

        // The original note must survive untouched above the appended section.
        assertTrue(annotated.startsWith(note.trimEnd()))
        assertTrue(annotated.contains("### End-of-session check"))
        assertTrue(annotated.contains("nothing in the note above was changed"))
        assertTrue(annotated.contains("**Units · warning:** Energy is measured in newtons."))
        assertTrue(annotated.contains("Suggested: Energy is measured in joules."))
        // The claimed-wrong statement is still present exactly once, not replaced.
        assertEquals(2, Regex("Energy is measured in newtons\\.").findAll(annotated).count())
    }

    @Test
    fun anEmptyFindingListLeavesTheNoteAlone() {
        val note = "# Physics\n\n- All correct.\n"
        val verification = NoteVerification(
            sessionId = "s1",
            requestId = "verify-1",
            findings = emptyList(),
            checkedFormulas = listOf("v = u + at"),
            checkedConcepts = emptyList(),
            warnings = emptyList(),
            source = NoteRefinementSource.PROVIDER,
        )

        assertEquals(note, appendVerificationFindings(note, verification))
        assertFalse(appendVerificationFindings(note, verification).contains("End-of-session"))
    }
}
