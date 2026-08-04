package me.thimmaiah.voxbox.board

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagramCropperTest {
    @Test
    fun normalizedBoundsRoundOutwardAndStayInsideImage() {
        val bounds = normalizedCropBounds(
            imageWidth = 1_000,
            imageHeight = 500,
            region = DiagramRegion(left = 0.123, top = 0.202, width = 0.456, height = 0.501, caption = "Graph"),
        )

        assertEquals(123, bounds.left)
        assertEquals(101, bounds.top)
        assertEquals(456, bounds.width)
        assertEquals(251, bounds.height)
    }

    @Test
    fun edgeCropNeverExceedsBitmap() {
        val bounds = normalizedCropBounds(
            imageWidth = 320,
            imageHeight = 240,
            region = DiagramRegion(left = 0.9, top = 0.8, width = 0.1, height = 0.2, caption = "Edge"),
        )

        assertEquals(288, bounds.left)
        assertEquals(192, bounds.top)
        assertEquals(32, bounds.width)
        assertEquals(48, bounds.height)
    }
}
