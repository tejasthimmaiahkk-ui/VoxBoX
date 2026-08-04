package me.thimmaiah.voxbox.network

import java.net.URL
import me.thimmaiah.voxbox.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoxBoxApiConfigTest {
    @Test
    fun debugEndpointKeepsAdbReverseLocalhostWorkflow() {
        assertTrue(BuildConfig.DEBUG)
        assertEquals(
            "http://127.0.0.1:8787/v1/notes/refine",
            voxBoxApiEndpoint("/v1/notes/refine"),
        )
    }

    @Test
    fun validatesHttpEndpointInDebugBuild() {
        val url: URL = validatedVoxBoxUrl("http://127.0.0.1:8787/v1/board/extract")

        assertEquals("http", url.protocol)
        assertEquals("127.0.0.1", url.host)
    }
}
