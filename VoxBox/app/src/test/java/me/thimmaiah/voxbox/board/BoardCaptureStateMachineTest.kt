package me.thimmaiah.voxbox.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardCaptureStateMachineTest {
    @Test
    fun `capture is gated by permission and rear camera readiness`() {
        val machine = BoardCaptureStateMachine()

        assertFalse(machine.beginCapture())
        machine.onPermissionChanged(true)
        assertFalse(machine.beginCapture())
        machine.onCameraReady()

        assertTrue(machine.beginCapture())
        assertEquals(BoardCaptureStage.CAPTURING, machine.state.stage)
        assertFalse(machine.beginCapture())
    }

    @Test
    fun `repeated permission callback does not regress a ready camera status`() {
        val machine = BoardCaptureStateMachine()
        machine.onPermissionChanged(true)
        machine.onCameraReady()

        machine.onPermissionChanged(true)

        assertTrue(machine.state.canCapture)
        assertEquals(
            "Aim at the board or projector, then capture one frame",
            machine.state.status,
        )
    }

    @Test
    fun `extraction must enter editable review before one shot save`() {
        val machine = BoardCaptureStateMachine()
        val extracted = BoardExtraction(
            title = "Original title",
            summary = "Original summary",
            visibleText = "A = πr²",
            concepts = listOf("area"),
            confidence = 0.75,
            warnings = listOf("Check the exponent."),
            source = BoardExtractionSource.REMOTE_VISION,
        )

        assertNull(machine.consumeReviewedExtraction())
        machine.showReview(extracted)
        machine.updateDraft { draft ->
            draft.copy(
                title = "  Circle area  ",
                conceptsText = "geometry\narea\nGeometry",
            )
        }

        val saved = machine.consumeReviewedExtraction()

        assertEquals("Circle area", saved?.title)
        assertEquals(listOf("geometry", "area"), saved?.concepts)
        assertEquals(BoardCaptureStage.SAVED, machine.state.stage)
        assertNull(machine.consumeReviewedExtraction())
    }

    @Test
    fun `retake discards draft and returns to live preview`() {
        val machine = BoardCaptureStateMachine()
        machine.onPermissionChanged(true)
        machine.showReview(
            BoardExtraction(
                title = "Board",
                summary = "",
                visibleText = "",
                concepts = emptyList(),
                confidence = 0.0,
                warnings = emptyList(),
                source = BoardExtractionSource.OFFLINE_OCR,
            ),
        )

        machine.retake()

        assertEquals(BoardCaptureStage.LIVE_PREVIEW, machine.state.stage)
        assertNull(machine.state.draft)
        assertTrue(machine.state.permissionGranted)
    }
}
