package me.thimmaiah.voxbox.reader

/**
 * A small Markdown model, covering exactly what the note pipeline emits.
 *
 * Deliberately not a full CommonMark implementation. The notes are written by a prompt that asks
 * for headings, lists, emphasis, block quotes and `$…$` maths, so parsing that subset well beats
 * parsing everything badly — and it stays a few hundred readable lines rather than a dependency.
 */
sealed interface MdBlock {
    data class Heading(val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Paragraph(val spans: List<MdSpan>) : MdBlock
    data class Bullet(val depth: Int, val spans: List<MdSpan>) : MdBlock
    data class Numbered(val depth: Int, val number: Int, val spans: List<MdSpan>) : MdBlock
    data class Quote(val callout: String?, val spans: List<MdSpan>) : MdBlock
    /** Fenced code, or a `$$…$$` display equation. Rendered monospaced and horizontally scrolled. */
    data class Code(val text: String, val isMath: Boolean) : MdBlock
    /** Header row plus body rows. Tables are how a comparison should read, not as bullets. */
    data class Table(val header: List<List<MdSpan>>, val rows: List<List<List<MdSpan>>>) : MdBlock
    data object Divider : MdBlock
}

/** Inline runs within a block. */
data class MdSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val math: Boolean = false,
    val highlight: Boolean = false,
)

private val bulletPrefix = Regex("^(\\s*)[-*+]\\s+(.*)$")
private val numberedPrefix = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
private val headingPrefix = Regex("^(#{1,6})\\s+(.*)$")
private val calloutPrefix = Regex("^\\[!(\\w+)]\\s*(.*)$")

/**
 * Splits Markdown into blocks.
 *
 * Fenced code and `$$` display maths are consumed whole before anything else, because their
 * contents must survive untouched — a wrapped equation is a wrong equation.
 */
fun parseMarkdownBlocks(markdown: String): List<MdBlock> {
    val lines = markdown.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<MdBlock>()
    val paragraph = StringBuilder()

    fun flushParagraph() {
        val text = paragraph.toString().trim()
        paragraph.setLength(0)
        if (text.isNotEmpty()) blocks += MdBlock.Paragraph(parseInline(text))
    }

    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()

        // A table is a header row, a separator of dashes and pipes, then body rows.
        if (trimmed.startsWith("|") && index + 1 < lines.size && isTableSeparator(lines[index + 1])) {
            flushParagraph()
            val header = tableCells(trimmed)
            val rows = mutableListOf<List<List<MdSpan>>>()
            index += 2
            while (index < lines.size && lines[index].trim().startsWith("|")) {
                rows += tableCells(lines[index].trim())
                index += 1
            }
            blocks += MdBlock.Table(header, rows)
            continue
        }

        if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
            flushParagraph()
            val fence = trimmed.take(3)
            val body = StringBuilder()
            index += 1
            while (index < lines.size && !lines[index].trim().startsWith(fence)) {
                body.appendLine(lines[index])
                index += 1
            }
            index += 1
            blocks += MdBlock.Code(body.toString().trimEnd(), isMath = false)
            continue
        }

        if (trimmed == "$$") {
            flushParagraph()
            val body = StringBuilder()
            index += 1
            while (index < lines.size && lines[index].trim() != "$$") {
                body.appendLine(lines[index])
                index += 1
            }
            index += 1
            blocks += MdBlock.Code(body.toString().trimEnd(), isMath = true)
            continue
        }

        // A single-line $$ … $$ display equation.
        if (trimmed.startsWith("$$") && trimmed.endsWith("$$") && trimmed.length > 4) {
            flushParagraph()
            blocks += MdBlock.Code(trimmed.removeSurrounding("$$").trim(), isMath = true)
            index += 1
            continue
        }

        if (trimmed.isEmpty()) {
            flushParagraph()
            index += 1
            continue
        }

        if (trimmed.startsWith("---") && trimmed.all { it == '-' } && trimmed.length >= 3) {
            flushParagraph()
            blocks += MdBlock.Divider
            index += 1
            continue
        }

        val heading = headingPrefix.matchEntire(trimmed)
        if (heading != null) {
            flushParagraph()
            blocks += MdBlock.Heading(
                level = heading.groupValues[1].length,
                spans = parseInline(heading.groupValues[2]),
            )
            index += 1
            continue
        }

        if (trimmed.startsWith(">")) {
            flushParagraph()
            val content = trimmed.removePrefix(">").trim()
            val callout = calloutPrefix.matchEntire(content)
            if (callout != null) {
                val rest = StringBuilder(callout.groupValues[2])
                index += 1
                while (index < lines.size && lines[index].trim().startsWith(">")) {
                    rest.append(' ').append(lines[index].trim().removePrefix(">").trim())
                    index += 1
                }
                blocks += MdBlock.Quote(callout.groupValues[1], parseInline(rest.toString().trim()))
            } else {
                val rest = StringBuilder(content)
                index += 1
                while (index < lines.size && lines[index].trim().startsWith(">")) {
                    rest.append(' ').append(lines[index].trim().removePrefix(">").trim())
                    index += 1
                }
                blocks += MdBlock.Quote(null, parseInline(rest.toString().trim()))
            }
            continue
        }

        val bullet = bulletPrefix.matchEntire(line)
        if (bullet != null) {
            flushParagraph()
            blocks += MdBlock.Bullet(
                depth = bullet.groupValues[1].length / 2,
                spans = parseInline(bullet.groupValues[2]),
            )
            index += 1
            continue
        }

        val numbered = numberedPrefix.matchEntire(line)
        if (numbered != null) {
            flushParagraph()
            blocks += MdBlock.Numbered(
                depth = numbered.groupValues[1].length / 2,
                number = numbered.groupValues[2].toIntOrNull() ?: 1,
                spans = parseInline(numbered.groupValues[3]),
            )
            index += 1
            continue
        }

        if (paragraph.isNotEmpty()) paragraph.append(' ')
        paragraph.append(trimmed)
        index += 1
    }
    flushParagraph()
    return blocks
}

/**
 * Splits one line into styled runs.
 *
 * Order matters: `**` must be tested before `*`, or bold is read as two empty italics. Inline
 * code and `$…$` maths are taken verbatim, so an asterisk inside an equation is not emphasis.
 */
fun parseInline(text: String): List<MdSpan> {
    val spans = mutableListOf<MdSpan>()
    val buffer = StringBuilder()
    var index = 0

    fun flush() {
        if (buffer.isNotEmpty()) {
            spans += MdSpan(buffer.toString())
            buffer.setLength(0)
        }
    }

    fun take(marker: String, span: (String) -> MdSpan): Boolean {
        if (!text.startsWith(marker, index)) return false
        val close = text.indexOf(marker, index + marker.length)
        if (close < 0) return false
        val inner = text.substring(index + marker.length, close)
        if (inner.isEmpty()) return false
        flush()
        spans += span(inner)
        index = close + marker.length
        return true
    }

    while (index < text.length) {
        val consumed = take("`") { MdSpan(it, code = true) } ||
            take("$") { MdSpan(it, math = true) } ||
            take("==") { MdSpan(it, highlight = true) } ||
            take("**") { MdSpan(it, bold = true) } ||
            take("__") { MdSpan(it, bold = true) } ||
            take("*") { MdSpan(it, italic = true) } ||
            take("_") { MdSpan(it, italic = true) }
        if (!consumed) {
            buffer.append(text[index])
            index += 1
        }
    }
    flush()
    return spans
}

private val tableSeparator = Regex("""^\|?[\s:|-]*-[\s:|-]*\|?$""")

private fun isTableSeparator(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.contains('-') && trimmed.contains('|') && tableSeparator.matches(trimmed)
}

private fun tableCells(line: String): List<List<MdSpan>> =
    line.trim().trim('|').split('|').map { cell -> parseInline(cell.trim()) }
