package me.thimmaiah.voxbox.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private data class Seg(val text: String)

class TranscriptOverlapTest {

    @Test
    fun trimsTheRepeatedClauseAtASeam() {
        val previous = "Let it shake the old windows, but do not become the storm."
        val incoming = "but do not become the storm. One day, perhaps without noticing, you will reach"

        val trimmed = trimRepeatedPrefix(previous, incoming)

        assertEquals("One day, perhaps without noticing, you will reach", trimmed)
    }

    @Test
    fun ignoresPunctuationAndCaseDisagreementBetweenTheTwoTranscriptions() {
        val previous = "pain says this hurt suffering whispers"
        val incoming = "Pain says, \"This hurt.\" Suffering whispers, therefore I must hurt forever."

        val trimmed = trimRepeatedPrefix(previous, incoming)

        assertEquals("therefore I must hurt forever.", trimmed)
    }

    @Test
    fun aShortAccidentalMatchIsNotTrimmed() {
        // "and so" recurring is coincidence, not an overlap, and trimming it would delete speech.
        val previous = "we will come back to that and so"
        val incoming = "and so the derivative of x squared is two x."

        assertEquals(incoming, trimRepeatedPrefix(previous, incoming, minimumWords = 3))
    }

    @Test
    fun noOverlapLeavesTheTextExactlyAsItWas() {
        val previous = "Pain is one of the oldest languages existence knows."
        val incoming = "Even the planets are wounded by time."

        assertEquals(incoming, trimRepeatedPrefix(previous, incoming))
    }

    @Test
    fun blankInputsAreSafe() {
        assertEquals("hello", trimRepeatedPrefix("", "hello"))
        assertEquals("", trimRepeatedPrefix("hello", ""))
    }

    @Test
    fun aWhollyRepeatedSegmentIsDropped() {
        val previous = "do not become the storm"
        val segments = listOf(Seg("Do not become the storm."), Seg("One day you will reach for the wound."))

        val kept = dropOverlappingSegments(previous, segments, Seg::text) { s, t -> s.copy(text = t) }

        assertEquals(1, kept.size)
        assertEquals("One day you will reach for the wound.", kept[0].text)
    }

    @Test
    fun aPartiallyRepeatedSegmentKeepsOnlyItsNewWords() {
        val previous = "let it howl between the pillars"
        val segments = listOf(Seg("Let it howl between the pillars, let it shake the old windows."))

        val kept = dropOverlappingSegments(previous, segments, Seg::text) { s, t -> s.copy(text = t) }

        assertEquals(1, kept.size)
        assertEquals("let it shake the old windows.", kept[0].text)
    }

    @Test
    fun trimmingStopsAfterTheSeamSoLaterRepetitionSurvives() {
        // A lecturer repeating themselves later in the chunk is real speech, not an artefact.
        val previous = "pain is one of the oldest languages"
        val segments = listOf(
            Seg("Pain is one of the oldest languages existence knows."),
            Seg("Pain is one of the oldest languages, I will say it again."),
        )

        val kept = dropOverlappingSegments(previous, segments, Seg::text) { s, t -> s.copy(text = t) }

        assertEquals(2, kept.size)
        assertTrue(kept[1].text.contains("I will say it again"))
    }

    @Test
    fun noPreviousTailMeansNothingIsTouched() {
        val segments = listOf(Seg("First words of the lecture."))

        val kept = dropOverlappingSegments("", segments, Seg::text) { s, t -> s.copy(text = t) }

        assertEquals(segments, kept)
    }
}
