package me.thimmaiah.voxbox.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedMarkdownRevisionTest {
    @Test
    fun `matching revision applies exactly one newer revision`() {
        val result = GeneratedMarkdownRevision.decide(
            currentRevision = 3,
            lastPatchId = "patch-3",
            currentMarkdown = "# Existing",
            patchId = "patch-4",
            expectedRevision = 3,
            newMarkdown = "# Existing\n\nNew concept",
        )

        assertEquals(GeneratedMarkdownUpdateResult.Apply(4), result)
    }

    @Test
    fun `retrying the latest identical patch is idempotent`() {
        val result = GeneratedMarkdownRevision.decide(
            currentRevision = 4,
            lastPatchId = "patch-4",
            currentMarkdown = "# Updated",
            patchId = "patch-4",
            expectedRevision = 3,
            newMarkdown = "# Updated",
        )

        assertEquals(GeneratedMarkdownUpdateResult.Duplicate(4), result)
    }

    @Test
    fun `reusing patch id with different content is rejected`() {
        val result = GeneratedMarkdownRevision.decide(
            currentRevision = 4,
            lastPatchId = "patch-4",
            currentMarkdown = "# Updated",
            patchId = "patch-4",
            expectedRevision = 4,
            newMarkdown = "# Different update",
        )

        assertTrue(result is GeneratedMarkdownUpdateResult.Conflict)
        assertEquals(4L, result.revision)
    }

    @Test
    fun `stale expected revision cannot overwrite a newer note`() {
        val result = GeneratedMarkdownRevision.decide(
            currentRevision = 7,
            lastPatchId = "patch-7",
            currentMarkdown = "# Current",
            patchId = "patch-stale",
            expectedRevision = 5,
            newMarkdown = "# Stale",
        )

        assertTrue(result is GeneratedMarkdownUpdateResult.Conflict)
        assertEquals(7L, result.revision)
    }

    @Test
    fun `blank markdown cannot erase generated notes`() {
        val result = GeneratedMarkdownRevision.decide(
            currentRevision = 2,
            lastPatchId = "patch-2",
            currentMarkdown = "# Keep this",
            patchId = "patch-3",
            expectedRevision = 2,
            newMarkdown = "  ",
        )

        assertTrue(result is GeneratedMarkdownUpdateResult.Invalid)
        assertEquals(2L, result.revision)
    }
}
