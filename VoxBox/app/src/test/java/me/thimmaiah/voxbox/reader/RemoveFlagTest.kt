package me.thimmaiah.voxbox.reader

import me.thimmaiah.voxbox.notes.removeFlagWithSuggestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Resolving a flag edits the note, so these guard the one property that matters: the captured
 * line survives whatever the student decides.
 */
class RemoveFlagTest {

    private val note = """
        # Physics

        Energy is measured in newtons.

        <!-- voxbox-review:start -->
        ## Review flags
        ### Suggested corrections
        - **Captured:** Energy is measured in newtons.
          - **Suggested:** Energy is measured in joules.
          - **Reason:** Newtons measure force.
        - **Captured:** Acceleration is measured in metres per second.
          - **Suggested:** Acceleration is measured in metres per second squared.
          - **Reason:** Units of acceleration.
        <!-- voxbox-review:end -->
    """.trimIndent()

    @Test
    fun removesOnlyTheResolvedCorrection() {
        val result = removeFlagWithSuggestion(note, "Energy is measured in joules.")

        assertTrue(!result.contains("measured in joules"))
        // The other flag is still awaiting a decision and must remain.
        assertTrue(result.contains("metres per second squared"))
    }

    @Test
    fun theCapturedLineInTheNoteBodyIsNeverTouched() {
        val result = removeFlagWithSuggestion(note, "Energy is measured in joules.")

        // The prose still says what the lecturer said, even though the AI disagreed with it.
        assertTrue(result.contains("Energy is measured in newtons."))
    }

    @Test
    fun anUnknownSuggestionChangesNothingMeaningful() {
        val result = removeFlagWithSuggestion(note, "Something nobody suggested")

        assertTrue(result.contains("measured in joules"))
        assertTrue(result.contains("metres per second squared"))
    }

    @Test
    fun aBlankSuggestionIsRejectedRatherThanMatchingEverything() {
        assertEquals(note, removeFlagWithSuggestion(note, "   "))
    }

    @Test
    fun resolvingBothLeavesNoOrphanedCorrectionLines() {
        val once = removeFlagWithSuggestion(note, "Energy is measured in joules.")
        val twice = removeFlagWithSuggestion(once, "Acceleration is measured in metres per second squared.")

        assertTrue(!twice.contains("**Captured:**"))
        assertTrue(!twice.contains("**Suggested:**"))
        assertTrue(twice.contains("Energy is measured in newtons."))
    }
}
