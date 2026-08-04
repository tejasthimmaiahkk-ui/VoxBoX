package me.thimmaiah.voxbox.session

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RetainedAudioRecoveryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun aRetainedFileNameCarriesItsChunkOffsetAndDuration() {
        val chunkId = "0f0b1c2d-3e4f-5061-7283-94a5b6c7d8e9"

        val name = retainedAudioFileName(chunkId, offsetMs = 40_000, durationMs = 20_000)
        val parsed = parseRetainedAudioName(name)

        assertEquals("$chunkId-o40000-d20000.wav", name)
        assertEquals(chunkId, parsed.chunkToken)
        assertEquals(40_000L, parsed.offsetMs)
        assertEquals(20_000L, parsed.durationMs)
    }

    @Test
    fun aFileWrittenBeforeTheNamingChangeStillListsWithADerivedDuration() {
        val parsed = parseRetainedAudioName("legacy-chunk-id.wav")
        assertEquals("legacy-chunk-id", parsed.chunkToken)
        assertNull(parsed.offsetMs)
        assertNull(parsed.durationMs)

        // 44-byte header plus 20 seconds of 16 kHz mono PCM16 audio.
        val wav = temporaryFolder.newFile("legacy-chunk-id.wav")
        wav.writeBytes(ByteArray(44 + 20 * 32_000))
        assertEquals(20_000L, wavDurationMs(wav))
        assertEquals(0L, wavDurationMs(File(temporaryFolder.root, "missing.wav")))
    }

    @Test
    fun recoveredAudioIsAppendedAsLabelledEvidenceInsteadOfRewritingTheNote() {
        val existing = "# Calculus\n\n- The power rule lowers the exponent."

        val recovered = appendRecoveredEvidence(
            existing = existing,
            offsetMs = 60_000,
            transcript = listOf(
                TranscriptEvidence(
                    id = "segment-1",
                    speakerId = "A",
                    startMs = 61_000,
                    endMs = 64_000,
                    text = "The chain rule multiplies the outer and inner derivatives.",
                    isPrimarySpeaker = false,
                ),
            ),
        )

        assertTrue("The original note must be preserved.", recovered.startsWith(existing))
        assertTrue(recovered.contains("## Recovered audio · 01:00"))
        assertTrue(recovered.contains("Transcribed later from a retained recording."))
        assertTrue(recovered.contains("- **01:01 · A:** The chain rule multiplies the outer and inner derivatives."))
    }
}
