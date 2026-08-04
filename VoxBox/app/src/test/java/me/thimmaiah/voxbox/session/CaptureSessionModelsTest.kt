package me.thimmaiah.voxbox.session

import org.junit.Assert.assertThrows
import org.junit.Test

class CaptureSessionModelsTest {
    @Test
    fun `video settings reject unsafe frame interval`() {
        assertThrows(IllegalArgumentException::class.java) {
            CaptureSessionSettings(
                noteId = "note-1",
                mode = CaptureMode.VIDEO,
                notePolicy = CaptureNotePolicy.RUNNABLE,
                frameIntervalMs = 500,
            )
        }
    }

    @Test
    fun `transcript rejects backwards timestamps`() {
        assertThrows(IllegalArgumentException::class.java) {
            NewTranscriptSegment(
                text = "Newton's second law",
                startMs = 2_000,
                endMs = 1_000,
            )
        }
    }

    @Test
    fun `asset rejects paths outside app storage`() {
        assertThrows(IllegalArgumentException::class.java) {
            NewNoteAsset(
                noteId = "note-1",
                evidenceId = "frame-1",
                kind = NoteAssetKind.DIAGRAM,
                localRelativePath = "../shared/diagram.png",
                caption = "Free-body diagram",
            )
        }
    }
}
