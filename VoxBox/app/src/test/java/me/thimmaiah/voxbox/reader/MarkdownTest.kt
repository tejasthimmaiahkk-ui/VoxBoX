package me.thimmaiah.voxbox.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every case here is taken from a note the app actually produced during a lecture, where it
 * rendered as raw `##` and `**` on screen.
 */
class MarkdownTest {

    @Test
    fun headingsAreBlocksNotLiteralHashes() {
        val blocks = parseMarkdownBlocks("## Captured evidence (needs review)")

        assertEquals(1, blocks.size)
        val heading = blocks[0] as MdBlock.Heading
        assertEquals(2, heading.level)
        assertEquals("Captured evidence (needs review)", heading.spans.joinToString("") { it.text })
    }

    @Test
    fun aBulletWithBoldTimestampSplitsIntoStyledRuns() {
        val blocks = parseMarkdownBlocks("- **00:09:** Ah, there you are.")

        val bullet = blocks[0] as MdBlock.Bullet
        assertEquals("00:09:", bullet.spans[0].text)
        assertTrue(bullet.spans[0].bold)
        assertEquals(" Ah, there you are.", bullet.spans[1].text)
        assertTrue(!bullet.spans[1].bold)
    }

    @Test
    fun boldIsNotReadAsTwoEmptyItalics() {
        val spans = parseInline("**Distinction:** Pain says this.")

        assertTrue(spans[0].bold)
        assertEquals("Distinction:", spans[0].text)
    }

    @Test
    fun nestedBulletsKeepTheirDepth() {
        val blocks = parseMarkdownBlocks("- top\n  - nested\n    - deeper")

        assertEquals(0, (blocks[0] as MdBlock.Bullet).depth)
        assertEquals(1, (blocks[1] as MdBlock.Bullet).depth)
        assertEquals(2, (blocks[2] as MdBlock.Bullet).depth)
    }

    @Test
    fun numberedListsKeepTheirNumbers() {
        val blocks = parseMarkdownBlocks("1. first\n2. second")

        assertEquals(1, (blocks[0] as MdBlock.Numbered).number)
        assertEquals(2, (blocks[1] as MdBlock.Numbered).number)
    }

    @Test
    fun displayMathIsOneVerbatimBlock() {
        val blocks = parseMarkdownBlocks("Before\n\n$$\nx^2 - 9 = (x-3)(x+3)\n$$\n\nAfter")

        val code = blocks[1] as MdBlock.Code
        assertTrue(code.isMath)
        assertEquals("x^2 - 9 = (x-3)(x+3)", code.text)
    }

    @Test
    fun aSingleLineDisplayEquationIsAlsoOneBlock() {
        val blocks = parseMarkdownBlocks("$$ a^2 + b^2 = c^2 $$")

        val code = blocks[0] as MdBlock.Code
        assertTrue(code.isMath)
        assertEquals("a^2 + b^2 = c^2", code.text)
    }

    @Test
    fun asterisksInsideMathAreNotEmphasis() {
        val spans = parseInline("The area is \$a * b\$ exactly.")

        val math = spans.first { it.math }
        assertEquals("a * b", math.text)
        assertTrue(spans.none { it.italic })
    }

    @Test
    fun fencedCodeSurvivesUntouched() {
        val blocks = parseMarkdownBlocks("```\n**not bold**\n```")

        val code = blocks[0] as MdBlock.Code
        assertTrue(!code.isMath)
        assertEquals("**not bold**", code.text)
    }

    @Test
    fun aCalloutQuoteKeepsItsKindAndText() {
        val blocks = parseMarkdownBlocks("> [!note] Suggested correction (not spoken)\n> Captured: x")

        val quote = blocks[0] as MdBlock.Quote
        assertEquals("note", quote.callout)
        assertTrue(quote.spans.joinToString("") { it.text }.contains("Captured: x"))
    }

    @Test
    fun consecutiveLinesJoinIntoOneParagraph() {
        val blocks = parseMarkdownBlocks("Pain is one of\nthe oldest languages.\n\nSecond paragraph.")

        assertEquals(2, blocks.size)
        assertEquals(
            "Pain is one of the oldest languages.",
            (blocks[0] as MdBlock.Paragraph).spans.joinToString("") { it.text },
        )
    }

    @Test
    fun aRealNoteFromTheFieldParsesIntoStructureNotRawText() {
        val note = """
            ## Pain vs. Suffering

            - Pain is an immediate reaction to harm.
            - **Distinction:** Pain says, "This hurts."

            ## Captured evidence (needs review)
            - **02:32:** Good morning class.
        """.trimIndent()

        val blocks = parseMarkdownBlocks(note)

        assertEquals(2, blocks.count { it is MdBlock.Heading })
        assertEquals(3, blocks.count { it is MdBlock.Bullet })
        // Nothing may survive as a literal hash or asterisk run.
        val rendered = blocks.joinToString("") { block ->
            when (block) {
                is MdBlock.Heading -> block.spans.joinToString("") { it.text }
                is MdBlock.Bullet -> block.spans.joinToString("") { it.text }
                is MdBlock.Paragraph -> block.spans.joinToString("") { it.text }
                else -> ""
            }
        }
        assertTrue(!rendered.contains("##"))
        assertTrue(!rendered.contains("**"))
    }
}
