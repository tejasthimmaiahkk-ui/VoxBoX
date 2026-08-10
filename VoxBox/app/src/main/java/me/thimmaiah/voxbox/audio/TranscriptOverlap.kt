package me.thimmaiah.voxbox.audio

/**
 * Removes the text a chunk repeats from the one before it.
 *
 * Chunks overlap by a couple of seconds so no word is only ever seen as a fragment at a clip
 * edge. The cost is that the seam is transcribed twice, and without this the note would stutter:
 * "…do not become the storm. …become the storm. One day…".
 *
 * Matching is on words rather than characters because the two transcriptions of the same audio
 * rarely agree on punctuation or capitalisation, and almost never split segments the same way.
 */

private val wordPattern = Regex("[\\p{L}\\p{N}']+")

internal fun transcriptWords(text: String): List<String> =
    wordPattern.findAll(text.lowercase()).map { it.value }.toList()

/**
 * Trims from [incoming] the longest run of words that already ends [previous].
 *
 * Requires [minimumWords] agreement before trimming anything: a one- or two-word match happens by
 * chance all the time ("the", "and so"), and trimming on that would delete real speech. Returns
 * [incoming] unchanged when no long-enough overlap is found, which is the safe direction — a
 * duplicated clause is a blemish, a deleted one is lost evidence.
 */
fun trimRepeatedPrefix(previous: String, incoming: String, minimumWords: Int = 3): String {
    if (previous.isBlank() || incoming.isBlank()) return incoming
    val previousWords = transcriptWords(previous)
    val incomingWords = transcriptWords(incoming)
    if (previousWords.isEmpty() || incomingWords.isEmpty()) return incoming

    // Longest suffix of `previous` that is also a prefix of `incoming`.
    val maxOverlap = minOf(previousWords.size, incomingWords.size)
    var overlap = 0
    for (length in maxOverlap downTo minimumWords) {
        val tail = previousWords.subList(previousWords.size - length, previousWords.size)
        val head = incomingWords.subList(0, length)
        if (tail == head) {
            overlap = length
            break
        }
    }
    if (overlap == 0) return incoming

    // Walk the original text and drop the matched words, keeping the rest verbatim.
    var seen = 0
    var cut = 0
    for (match in wordPattern.findAll(incoming.lowercase())) {
        seen += 1
        if (seen == overlap) {
            cut = match.range.last + 1
            break
        }
    }
    return incoming.substring(cut).trimStart(' ', ',', '.', ';', ':', '-', '—')
}

/**
 * Applies [trimRepeatedPrefix] across a chunk's segments, dropping any that are wholly repeated.
 *
 * Returns the segments to keep. A segment trimmed to nothing is removed rather than stored empty,
 * so the transcript does not gain a blank line at every seam.
 */
fun <T> dropOverlappingSegments(
    previousTail: String,
    segments: List<T>,
    text: (T) -> String,
    withText: (T, String) -> T,
): List<T> {
    if (previousTail.isBlank() || segments.isEmpty()) return segments
    val kept = mutableListOf<T>()
    var carry = previousTail
    var stillTrimming = true
    segments.forEach { segment ->
        val original = text(segment)
        if (!stillTrimming) {
            kept += segment
            return@forEach
        }
        val trimmed = trimRepeatedPrefix(carry, original)
        when {
            trimmed.isBlank() -> {
                // Wholly repeated: the previous chunk already recorded these words.
                carry = original
            }
            trimmed == original -> {
                // Nothing matched, so the overlap is behind us for the rest of this chunk.
                stillTrimming = false
                kept += segment
            }
            else -> {
                stillTrimming = false
                kept += withText(segment, trimmed)
            }
        }
    }
    return kept
}
