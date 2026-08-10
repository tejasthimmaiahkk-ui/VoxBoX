package me.thimmaiah.voxbox.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DiagramLinkTest {

    @Test
    fun findsEveryImageLinkWithItsCaption() {
        val markdown = """
            Some prose.

            ![Tangent graph](note-assets/n1/a.jpg)

            More prose.

            ![](note-assets/n1/b.png)
        """.trimIndent()

        val links = findDiagramLinks(markdown)

        assertEquals(2, links.size)
        assertEquals("Tangent graph", links[0].alt)
        assertEquals("note-assets/n1/a.jpg", links[0].path)
        assertEquals("", links[1].alt)
    }

    @Test
    fun proseSurvivesWithoutTheLinkSyntax() {
        val markdown = "Before.\n\n![Graph](note-assets/a.jpg)\n\nAfter."

        val prose = withoutDiagramLinks(markdown)

        assertTrue(prose.contains("Before."))
        assertTrue(prose.contains("After."))
        assertTrue(!prose.contains("note-assets"))
        assertTrue(!prose.contains("!["))
    }

    // Note text is model output, and a path is the one part of it that could reach outside the
    // app's own storage. These are the cases that must never resolve.

    @Test
    fun aPathEscapingPrivateStorageIsRejected() {
        val root = File("/data/data/app/files")

        assertNull(resolveAsset(root, "../../../etc/passwd"))
        assertNull(resolveAsset(root, "note-assets/../../secrets.txt"))
        assertNull(resolveAsset(root, "/etc/passwd"))
        assertNull(resolveAsset(root, "content://media/external/1"))
        assertNull(resolveAsset(root, "   "))
    }

    @Test
    fun anOrdinaryRelativeAssetPathResolvesUnderPrivateStorage() {
        val root = File("/data/data/app/files")

        val resolved = resolveAsset(root, "./note-assets/n1/a.jpg")

        assertTrue(resolved!!.path.replace('\\', '/').endsWith("files/note-assets/n1/a.jpg"))
    }
}
