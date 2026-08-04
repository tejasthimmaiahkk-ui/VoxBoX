package me.thimmaiah.voxbox.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SyllabusImporterTest {
    @Test
    fun sha256IsStableAndLowercase() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256("abc".toByteArray()),
        )
    }
}
