package me.thimmaiah.voxbox.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownNotePreviewTest {
    @Test
    fun reviewSectionMarkersAreNotRenderedAsNoteText() {
        assertTrue(isHiddenMarkdownLine("<!-- voxbox-review:start -->"))
        assertTrue(isHiddenMarkdownLine("<!-- voxbox-review:end -->"))
        assertTrue(isHiddenMarkdownLine("   <!-- voxbox-review:start -->   "))
    }

    @Test
    fun ordinaryNoteContentIsStillRendered() {
        assertFalse(isHiddenMarkdownLine("## Review flags"))
        assertFalse(isHiddenMarkdownLine("- The derivative of x squared is 2x"))
        assertFalse(isHiddenMarkdownLine(""))
        // A partial or inline comment must not silently swallow surrounding content.
        assertFalse(isHiddenMarkdownLine("<!-- unterminated"))
        assertFalse(isHiddenMarkdownLine("text <!-- comment -->"))
    }
}
