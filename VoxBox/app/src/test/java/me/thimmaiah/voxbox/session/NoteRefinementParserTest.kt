package me.thimmaiah.voxbox.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteRefinementParserTest {
    @Test
    fun parsesRevisionedMarkdownAndCorrections() {
        val result = parseNoteRefinementResponse(
            """
            {
              "requestId":"r1",
              "sessionId":"s1",
              "baseRevision":3,
              "nextRevision":4,
              "title":"Calculus",
              "markdown":"# Calculus\n\n- **Power rule**",
              "corrections":[{
                "captured":"x squared is x",
                "suggested":"the derivative of x squared is 2x",
                "reason":"Board equation conflicts with the spoken phrase.",
                "severity":"warning",
                "evidenceIds":["segment-1","frame-1"]
              }],
              "consumedEvidenceIds":["segment-1"],
              "warnings":[],
              "source":"openai"
            }
            """.trimIndent(),
        )

        assertEquals(3, result.baseRevision)
        assertEquals(4, result.nextRevision)
        assertEquals(NoteRefinementSource.PROVIDER, result.source)
        assertEquals(NoteRefinementUpdateMode.FULL, result.updateMode)
        assertTrue(result.markdown.contains("Power rule"))
        assertEquals("warning", result.corrections.single().severity)
    }

    @Test
    fun parsesBoundedMarkdownDeltaWithoutRestatingTheFullNote() {
        val hash = "a".repeat(64)
        val result = parseNoteRefinementResponse(
            """
            {
              "requestId":"r2",
              "sessionId":"s1",
              "baseRevision":4,
              "nextRevision":5,
              "title":"Calculus",
              "updateMode":"delta",
              "baseContentSha256":"$hash",
              "markdownDelta":"## Chain rule\n\n- Differentiate the outer function first.",
              "corrections":[],
              "consumedEvidenceIds":["segment-2"],
              "warnings":[],
              "source":"openai"
            }
            """.trimIndent(),
        )

        assertEquals(NoteRefinementUpdateMode.DELTA, result.updateMode)
        assertEquals(hash, result.baseContentSha256)
        assertEquals("", result.markdown)
        assertEquals(
            "# Calculus\n\n## Chain rule\n\n- Differentiate the outer function first.",
            result.materializeMarkdown("# Calculus"),
        )
    }

    @Test
    fun rejectsDeltaWithInvalidBaseHash() {
        val error = runCatching {
            parseNoteRefinementResponse(
                """
                {
                  "requestId":"r3","sessionId":"s1","baseRevision":5,"nextRevision":6,
                  "title":"Calculus","updateMode":"delta","baseContentSha256":"not-a-hash",
                  "markdownDelta":"## New material","corrections":[],
                  "consumedEvidenceIds":[],"warnings":[],"source":"mock"
                }
                """.trimIndent(),
            )
        }.exceptionOrNull()

        assertTrue(error is NoteRefinementException)
        assertTrue(error?.message.orEmpty().contains("hash"))
    }

    @Test
    fun anEmptyDeltaIsAcceptedAndLeavesTheNoteUnchanged() {
        // The concise detail level asks the model to return an empty delta when a twenty-second
        // chunk adds nothing. Treating that as a failure showed "AI refinement was unavailable"
        // during a live session for a response that was correct.
        val json = """
            {
              "requestId": "r1",
              "sessionId": "s1",
              "baseRevision": 3,
              "nextRevision": 4,
              "title": "Algebra",
              "updateMode": "delta",
              "baseContentSha256": "${"a".repeat(64)}",
              "markdownDelta": "",
              "corrections": [],
              "consumedEvidenceIds": [],
              "warnings": [],
              "source": "openrouter"
            }
        """.trimIndent()

        val refinement = parseNoteRefinementResponse(json)

        val existing = "# Algebra\n\nExisting body."

        assertEquals("", refinement.markdownDelta)
        assertEquals(existing, refinement.materializeMarkdown(existing))
    }
}
