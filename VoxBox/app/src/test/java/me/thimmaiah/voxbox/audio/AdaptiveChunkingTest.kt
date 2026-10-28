package me.thimmaiah.voxbox.audio

import kotlin.math.PI
import kotlin.math.sin
import me.thimmaiah.voxbox.network.offlineBackoffMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveChunkingTest {
    private val rate = 16_000

    /** PCM16 of [ms] milliseconds: a 220 Hz tone at [amplitude], or silence at 0. */
    private fun pcm(ms: Int, amplitude: Double): ByteArray {
        val samples = rate * ms / 1_000
        val out = ByteArray(samples * 2)
        for (i in 0 until samples) {
            val v = (amplitude * sin(2 * PI * 220 * i / rate)).toInt().toShort().toInt()
            out[2 * i] = (v and 0xFF).toByte()
            out[2 * i + 1] = (v shr 8).toByte()
        }
        return out
    }

    /** Speech-like: 220 ms syllables with 80 ms dips, so the floor sees the gaps real speech has. */
    private fun speech(ms: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var left = ms
        while (left > 0) {
            out.write(pcm(minOf(220, left), 6_000.0)); left -= 220
            if (left > 0) { out.write(pcm(minOf(80, left), 200.0)); left -= 80 }
        }
        return out.toByteArray()
    }

    private fun accumulator() = PcmChunkAccumulator(
        targetBytes = rate * 2 * 10,
        minimumUsefulBytes = rate,
        sampleRate = rate,
        softTargetBytes = rate * 2 * 4,
        pauseBytes = rate * 2 * 360 / 1_000 / 2 * 2,
    )

    @Test
    fun cutsAtThePauseAfterTheSoftTargetInsteadOfTheHardCeiling() {
        val acc = accumulator()
        val chunks = acc.append(speech(5_000)) + acc.append(pcm(600, 0.0)) + acc.append(speech(3_000))
        assertEquals(1, chunks.size)
        // 5 s of speech plus the 360 ms pause that triggered the cut, well before the 10 s ceiling.
        assertTrue(chunks[0].durationMs in 5_300L..5_500L)
        assertTrue(chunks[0].voicedMs >= 3_000L)
    }

    @Test
    fun aLecturerWhoNeverPausesStillGetsAChunkAtTheCeiling() {
        val acc = accumulator()
        val chunks = acc.append(speech(11_000))
        assertEquals(1, chunks.size)
        assertEquals(10_000L, chunks[0].durationMs)
    }

    @Test
    fun silenceAndSteadyHumAreNotCountedAsSpeech() {
        val quiet = accumulator()
        quiet.append(pcm(3_000, 40.0))
        assertTrue(requireNotNull(quiet.finish()).voicedMs < 100L)

        // Constant fan hum raises the floor; only the louder voice on top of it counts.
        val hum = accumulator()
        hum.append(pcm(6_000, 1_500.0))
        hum.append(pcm(1_000, 9_000.0))
        val chunk = requireNotNull(hum.finish())
        assertTrue("voiced=${chunk.voicedMs}", chunk.voicedMs in 800L..2_500L)
    }

    @Test
    fun offlineBackoffGrowsToACeilingAndNeverGivesUp() {
        assertEquals(750L, offlineBackoffMs(0))
        assertEquals(2_000L, offlineBackoffMs(1))
        assertEquals(4_000L, offlineBackoffMs(2))
        assertEquals(30_000L, offlineBackoffMs(50))
    }
}
