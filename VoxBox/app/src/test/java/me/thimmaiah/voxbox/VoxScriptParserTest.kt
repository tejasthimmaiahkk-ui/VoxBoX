package me.thimmaiah.voxbox

import me.thimmaiah.voxbox.voxscript.VoxScriptParser
import me.thimmaiah.voxbox.voxscript.VoxScriptResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoxScriptParserTest {
    private val parser = VoxScriptParser()

    @Test
    fun `plain speech remains dictation`() {
        val result = parser.parse("Plants convert light into chemical energy")

        assertTrue(result is VoxScriptResult.PlainDictation)
    }

    @Test
    fun `heading command extracts heading text`() {
        val result = parser.parse("Vox heading Photosynthesis")

        assertTrue(result is VoxScriptResult.Heading)
        assertEquals("photosynthesis", (result as VoxScriptResult.Heading).text)
    }

    @Test
    fun `bullet point command extracts item`() {
        val result = parser.parse("Note bullet point Light intensity")

        assertTrue(result is VoxScriptResult.BulletPoint)
        assertEquals("light intensity", (result as VoxScriptResult.BulletPoint).text)
    }

    @Test
    fun `pie chart command extracts percentage color and label`() {
        val result = parser.parse("Tejas pie chart 25 percent yellow label wheat")

        assertTrue(result is VoxScriptResult.PieChart)
        result as VoxScriptResult.PieChart
        assertEquals(25, result.percentage)
        assertEquals("yellow", result.color)
        assertEquals("wheat", result.label)
    }

    @Test
    fun `invalid pie percentage is rejected`() {
        val result = parser.parse("Vox pie chart 125 percent yellow label wheat")

        assertTrue(result is VoxScriptResult.InvalidCommand)
        assertEquals(
            "Pie-chart percentage must be between 0 and 100.",
            (result as VoxScriptResult.InvalidCommand).reason,
        )
    }

    @Test
    fun `pie chart without label is rejected`() {
        val result = parser.parse("Vox pie chart 25 percent yellow")

        assertTrue(result is VoxScriptResult.InvalidCommand)
        assertEquals(
            "A pie chart needs a label or tag.",
            (result as VoxScriptResult.InvalidCommand).reason,
        )
    }
}
