package me.thimmaiah.voxbox.session

import java.nio.charset.StandardCharsets

private const val MAX_OUTLINE_CHARS = 12_000
private const val MAX_RECENT_CHARS = 24_000
private const val MAX_EXCERPTS = 6
private const val MAX_EXCERPT_CHARS = 2_000
private const val MAX_EXCERPT_TOTAL_CHARS = 12_000

internal fun buildIncrementalNoteContext(
    title: String,
    completeMarkdown: String,
): IncrementalNoteContext {
    val normalized = completeMarkdown.trim()
    val headings = normalized.lineSequence()
        .map(String::trim)
        .filter { line -> line.matches(Regex("#{1,6}\\s+.+")) }
        .distinct()
        .joinToString("\n")
        .take(MAX_OUTLINE_CHARS)
    val outline = headings.ifBlank { normalized.take(MAX_OUTLINE_CHARS.coerceAtMost(2_000)) }
    return IncrementalNoteContext(
        title = title.trim().take(240),
        outlineMarkdown = outline,
        recentMarkdown = normalized.takeLast(MAX_RECENT_CHARS),
        contentSha256 = sha256(normalized.toByteArray(StandardCharsets.UTF_8)),
    )
}

internal fun selectRelevantSyllabusExcerpts(
    syllabusText: String,
    evidenceText: String,
): List<SyllabusContextExcerpt> {
    val sections = syllabusSections(syllabusText)
    if (sections.isEmpty()) return emptyList()
    val evidenceTokens = searchableTokens(evidenceText)
    val ranked = sections.mapIndexed { index, section ->
        val score = searchableTokens(section.text).count(evidenceTokens::contains)
        RankedSection(index, section, score)
    }
    val candidates = ranked.filter { it.score > 0 }
        .sortedWith(compareByDescending<RankedSection> { it.score }.thenBy { it.index })
        .ifEmpty { ranked.take(3) }

    var remaining = MAX_EXCERPT_TOTAL_CHARS
    return buildList {
        candidates.take(MAX_EXCERPTS).forEach { rankedSection ->
            if (remaining <= 0) return@forEach
            val text = rankedSection.section.text.take(minOf(MAX_EXCERPT_CHARS, remaining)).trim()
            if (text.isBlank()) return@forEach
            add(
                SyllabusContextExcerpt(
                    id = "syllabus-${rankedSection.index + 1}",
                    heading = rankedSection.section.heading.take(240),
                    text = text,
                ),
            )
            remaining -= text.length
        }
    }
}

private data class SyllabusSection(val heading: String, val text: String)
private data class RankedSection(val index: Int, val section: SyllabusSection, val score: Int)

private fun syllabusSections(value: String): List<SyllabusSection> {
    val normalized = value.replace("\r\n", "\n").trim()
    if (normalized.isBlank()) return emptyList()
    val headingStarts = Regex("(?m)^#{1,6}\\s+.+$").findAll(normalized).toList()
    if (headingStarts.isEmpty()) {
        return normalized.chunked(MAX_EXCERPT_CHARS).mapIndexed { index, text ->
            SyllabusSection("Syllabus part ${index + 1}", text.trim())
        }.filter { it.text.isNotBlank() }
    }
    return headingStarts.mapIndexed { index, match ->
        val end = headingStarts.getOrNull(index + 1)?.range?.first ?: normalized.length
        SyllabusSection(
            heading = match.value.trimStart('#').trim(),
            text = normalized.substring(match.range.first, end).trim(),
        )
    }
}

private val stopWords = setOf(
    "about", "after", "again", "also", "and", "are", "because", "been", "before", "being",
    "between", "class", "could", "does", "from", "have", "into", "more", "most", "not", "that",
    "the", "their", "then", "there", "these", "this", "those", "through", "using", "was", "were",
    "what", "when", "where", "which", "while", "with", "would",
)

private fun searchableTokens(value: String): Set<String> = Regex("[\\p{L}\\p{N}]{3,}")
    .findAll(value.lowercase())
    .map(MatchResult::value)
    .filterNot(stopWords::contains)
    .toSet()
