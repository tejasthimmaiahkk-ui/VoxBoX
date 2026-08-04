package me.thimmaiah.voxbox.board

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardExtractionCoordinatorTest {
    @Test
    fun `remote failure falls back to bundled OCR with disclosure warning`() = runBlocking {
        val remote = StubExtractionClient {
            throw BoardExtractionException("Local server is offline.")
        }
        val offline = StubExtractionClient {
            BoardExtraction(
                title = "Physics",
                summary = "",
                visibleText = "F = ma",
                concepts = emptyList(),
                confidence = 0.0,
                warnings = listOf("Confidence is unavailable."),
                source = BoardExtractionSource.OFFLINE_OCR,
            )
        }
        val coordinator = BoardExtractionCoordinator(remote, offline)

        val result = coordinator.extract(byteArrayOf(1))

        assertEquals(BoardExtractionSource.OFFLINE_OCR, result.source)
        assertEquals("F = ma", result.visibleText)
        assertTrue(result.warnings.first().contains("offline OCR"))
        assertTrue(result.warnings.contains("Confidence is unavailable."))
    }

    @Test
    fun `successful remote extraction does not invoke fallback`() = runBlocking {
        var offlineInvoked = false
        val expected = BoardExtraction(
            title = "Chemistry",
            summary = "Atomic structure",
            visibleText = "e⁻",
            concepts = listOf("electrons"),
            confidence = 0.9,
            warnings = emptyList(),
            source = BoardExtractionSource.REMOTE_VISION,
        )
        val coordinator = BoardExtractionCoordinator(
            remoteClient = StubExtractionClient { expected },
            offlineClient = StubExtractionClient {
                offlineInvoked = true
                expected.copy(source = BoardExtractionSource.OFFLINE_OCR)
            },
        )

        assertEquals(expected, coordinator.extract(byteArrayOf(1)))
        assertTrue(!offlineInvoked)
    }
}

private class StubExtractionClient(
    private val extraction: suspend () -> BoardExtraction,
) : BoardExtractionClient {
    override suspend fun extract(jpegBytes: ByteArray): BoardExtraction = extraction()
}
