package me.thimmaiah.voxbox.session

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Request-side contract tests.
 *
 * A frame-only update carries no transcript segments, so the proxy rejects the request outright
 * when board evidence is dropped during serialization. That regression reached a live session and
 * silently downgraded every board update to the local fallback, so it is pinned here.
 */
class NoteRefinementRequestJsonTest {
    private val boardEvidence = BoardNoteEvidence(
        id = "frame-1",
        capturedAtMs = 42_000,
        summary = "Derivative rules on the whiteboard",
        visibleText = listOf("d/dx x^2 = 2x"),
        concepts = listOf("power rule"),
        equations = listOf("f'(x) = 2x"),
        diagramCaptions = listOf("Tangent slope sketch"),
    )

    private fun request(
        transcriptSegments: List<TranscriptEvidence> = emptyList(),
        boardEvidence: BoardNoteEvidence? = this.boardEvidence,
    ) = NoteRefinementRequest(
        requestId = "r1",
        sessionId = "s1",
        baseRevision = 2,
        mode = CaptureMode.VIDEO,
        notePolicy = CaptureNotePolicy.RUNNABLE,
        primarySpeakerId = null,
        syllabusContext = "",
        existingMarkdown = "",
        transcriptSegments = transcriptSegments,
        boardEvidence = boardEvidence,
    )

    @Test
    fun frameOnlyRequestKeepsBoardEvidenceInTheBody() {
        val body = request().toJson()

        val board = body["boardEvidence"]
        assertTrue(
            "A frame-only request must not send a null boardEvidence field.",
            board is JsonObject,
        )
        assertEquals("frame-1", board!!.jsonObject["id"]?.jsonPrimitive?.content)
        assertEquals(42_000L, board.jsonObject["capturedAtMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(
            "d/dx x^2 = 2x",
            board.jsonObject["visibleText"]?.jsonArray?.single()?.jsonPrimitive?.content,
        )
        assertEquals(
            "Tangent slope sketch",
            board.jsonObject["diagramCaptions"]?.jsonArray?.single()?.jsonPrimitive?.content,
        )
        assertTrue(body["transcriptSegments"]!!.jsonArray.isEmpty())
    }

    @Test
    fun audioOnlyRequestStillSendsExplicitNullBoardEvidence() {
        val body = request(
            transcriptSegments = listOf(
                TranscriptEvidence(
                    id = "segment-1",
                    speakerId = "A",
                    startMs = 0,
                    endMs = 4_000,
                    text = "The power rule lowers the exponent by one.",
                    isPrimarySpeaker = true,
                ),
            ),
            boardEvidence = null,
        ).toJson()

        assertEquals(JsonNull, body["boardEvidence"])
        assertEquals(
            "segment-1",
            body["transcriptSegments"]!!.jsonArray.single().jsonObject["id"]?.jsonPrimitive?.content,
        )
    }
}
