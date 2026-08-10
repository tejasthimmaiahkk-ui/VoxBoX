package me.thimmaiah.voxbox.reader

/** One disagreement between the model and the captured evidence. */
data class ReviewFlag(
    val captured: String,
    val suggested: String,
    val reason: String,
    val severity: String,
    val evidenceIds: List<String>,
)

/** A note split into the part you read and the part you have to decide about. */
data class ParsedNote(
    val body: String,
    val warnings: List<String>,
    val flags: List<ReviewFlag>,
)

private const val REVIEW_START = "<!-- voxbox-review:start -->"
private const val REVIEW_END = "<!-- voxbox-review:end -->"

/**
 * Pulls the review section out of a note so the reader can render it as decisions rather than prose.
 *
 * The capture pipeline writes flags into the note between HTML-comment markers, which keeps a note
 * a single portable Markdown document — it exports and opens in Obsidian with no special support.
 * The cost is that a plain renderer shows them as ordinary bullet text, which is exactly wrong:
 * a suggestion the AI made must not look like something the lecturer said. Parsing them back out
 * is what lets the reader show the captured line and the suggestion side by side, with neither
 * action rewriting the original.
 */
fun parseNoteForReview(markdown: String): ParsedNote {
    val start = markdown.indexOf(REVIEW_START)
    if (start < 0) return ParsedNote(markdown.trim(), emptyList(), emptyList())
    val end = markdown.indexOf(REVIEW_END, start + REVIEW_START.length)
    val sectionEnd = if (end < 0) markdown.length else end
    val body = (
        markdown.substring(0, start).trimEnd() +
            if (end < 0) "" else "\n\n" + markdown.substring(end + REVIEW_END.length).trimStart()
        ).trim()
    val section = markdown.substring(start + REVIEW_START.length, sectionEnd)

    val warnings = mutableListOf<String>()
    val flags = mutableListOf<ReviewFlag>()
    var inWarnings = false
    var captured: String? = null
    var suggested = ""
    var reason = ""
    var severity = ""
    var evidence = emptyList<String>()

    fun flush() {
        val capturedLine = captured ?: return
        flags += ReviewFlag(capturedLine, suggested, reason, severity, evidence)
        captured = null
        suggested = ""
        reason = ""
        severity = ""
        evidence = emptyList()
    }

    section.lineSequence().map(String::trim).forEach { line ->
        when {
            line.startsWith("### Warnings") -> { flush(); inWarnings = true }
            line.startsWith("### Suggested corrections") -> { flush(); inWarnings = false }
            line.startsWith("## ") -> Unit
            line.startsWith("- **Captured:**") -> {
                flush()
                captured = line.removePrefix("- **Captured:**").trim()
            }
            line.startsWith("- **Suggested:**") -> suggested = line.removePrefix("- **Suggested:**").trim()
            line.startsWith("- **Reason:**") -> reason = line.removePrefix("- **Reason:**").trim()
            line.startsWith("- **Severity:**") -> severity = line.removePrefix("- **Severity:**").trim()
            line.startsWith("- **Evidence:**") -> evidence = line.removePrefix("- **Evidence:**")
                .split(',').map(String::trim).filter(String::isNotBlank)
            inWarnings && line.startsWith("- ") -> warnings += line.removePrefix("- ").trim()
            else -> Unit
        }
    }
    flush()
    return ParsedNote(body, warnings, flags)
}
