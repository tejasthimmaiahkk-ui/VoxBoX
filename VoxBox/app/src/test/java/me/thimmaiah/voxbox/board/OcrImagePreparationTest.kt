package me.thimmaiah.voxbox.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrImagePreparationTest {
    @Test
    fun `small images retain full resolution`() {
        assertEquals(1, calculateOcrSampleSize(width = 1920, height = 1080))
    }

    @Test
    fun `large images are bounded with a power of two sample`() {
        val sample = calculateOcrSampleSize(width = 12_000, height = 9_000)

        assertEquals(8, sample)
        assertTrue(12_000 / sample <= 2_560)
        assertEquals(0, sample and (sample - 1))
    }

    @Test
    fun `invalid dimensions use safe default`() {
        assertEquals(1, calculateOcrSampleSize(width = 0, height = 4_000))
        assertEquals(1, calculateOcrSampleSize(width = 4_000, height = -1))
    }
}
