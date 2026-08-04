package me.thimmaiah.voxbox.board

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpBoardExtractionClientTest {
    @Test
    fun `valid response joins visible text and normalizes review fields`() {
        val result = parseBoardExtractionResponse(
            """
                {
                  "title": "  Calculus  ",
                  "summary": "  Derivative rules  ",
                  "visibleText": [" d/dx x² = 2x ", "", "Power rule"],
                  "concepts": [" Derivatives ", "derivatives", "Power rule"],
                  "confidence": 0.92,
                  "warnings": [" Check exponent ", "check exponent"]
                }
            """.trimIndent(),
        )

        assertEquals("Calculus", result.title)
        assertEquals("Derivative rules", result.summary)
        assertEquals("d/dx x² = 2x\nPower rule", result.visibleText)
        assertEquals(listOf("Derivatives", "Power rule"), result.concepts)
        assertEquals(0.92, result.confidence, 0.0)
        assertEquals(listOf("Check exponent"), result.warnings)
        assertEquals(BoardExtractionSource.REMOTE_VISION, result.source)
    }

    @Test
    fun `mock source is explicit and confidence is not presented as live vision`() {
        val result = parseBoardExtractionResponse(
            """
                {
                  "title": "Mock board capture",
                  "summary": "Deterministic response",
                  "visibleText": ["Image not analyzed"],
                  "concepts": [],
                  "confidence": 0,
                  "warnings": ["Mock mode is enabled."],
                  "source": "mock"
                }
            """.trimIndent(),
        )

        assertEquals(BoardExtractionSource.MOCK_PROXY, result.source)
        assertEquals(0.0, result.confidence, 0.0)
    }

    @Test
    fun `invalid confidence and malformed fields are rejected`() {
        val invalidConfidence =
            """{"title":"","summary":"","visibleText":[],"concepts":[],"confidence":1.2,"warnings":[]}"""
        val invalidVisibleText =
            """{"title":"","summary":"","visibleText":"text","concepts":[],"confidence":0.5,"warnings":[]}"""

        assertThrows(BoardExtractionException::class.java) {
            parseBoardExtractionResponse(invalidConfidence)
        }
        assertThrows(BoardExtractionException::class.java) {
            parseBoardExtractionResponse(invalidVisibleText)
        }
    }

    @Test
    fun `client posts jpeg base64 with bounded timeouts`() = runBlocking {
        val response =
            """{"title":"Board","summary":"","visibleText":["Line 1","Line 2"],"concepts":[],"confidence":0.8,"warnings":[]}"""
        val connection = FakeHttpURLConnection(URL("http://127.0.0.1/test"), 200, response)
        val client = HttpBoardExtractionClient(
            endpoint = "http://127.0.0.1:8787/v1/board/extract",
            connectTimeoutMillis = 321,
            readTimeoutMillis = 654,
            connectionFactory = { connection },
        )

        val result = client.extract(validJpeg())
        val requestJson = Json.parseToJsonElement(connection.requestBody()).jsonObject

        assertEquals("POST", connection.requestMethod)
        assertEquals(321, connection.connectTimeout)
        assertEquals(654, connection.readTimeout)
        assertEquals("image/jpeg", requestJson.getValue("mimeType").jsonPrimitive.content)
        assertEquals("/9gBAv/Z", requestJson.getValue("imageBase64").jsonPrimitive.content)
        assertEquals("Line 1\nLine 2", result.visibleText)
        assertTrue(connection.wasDisconnected)
    }

    @Test
    fun `server error message is surfaced without attempting response parsing`() {
        val connection = FakeHttpURLConnection(
            url = URL("http://127.0.0.1/test"),
            status = 503,
            response = """{"error":{"code":"openai_not_configured","message":"Server-side vision is not configured."}}""",
        )
        val client = HttpBoardExtractionClient(connectionFactory = { connection })

        val error = assertThrows(BoardExtractionException::class.java) {
            runBlocking { client.extract(validJpeg()) }
        }

        assertEquals("Server-side vision is not configured.", error.message)
        assertTrue(connection.wasDisconnected)
    }

    private fun validJpeg(): ByteArray =
        byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02, 0xFF.toByte(), 0xD9.toByte())
}

private class FakeHttpURLConnection(
    url: URL,
    private val status: Int,
    private val response: String,
) : HttpURLConnection(url) {
    private val posted = ByteArrayOutputStream()
    var wasDisconnected: Boolean = false
        private set

    override fun getOutputStream(): ByteArrayOutputStream = posted

    override fun getResponseCode(): Int = status

    override fun getInputStream(): InputStream {
        if (status !in 200..299) throw IllegalStateException("Use errorStream for failed responses.")
        return response.byteInputStream(StandardCharsets.UTF_8)
    }

    override fun getErrorStream(): InputStream? =
        response.takeIf { status !in 200..299 }?.byteInputStream(StandardCharsets.UTF_8)

    override fun disconnect() {
        wasDisconnected = true
    }

    override fun usingProxy(): Boolean = false

    override fun connect() = Unit

    fun requestBody(): String = posted.toByteArray().toString(StandardCharsets.UTF_8)
}
