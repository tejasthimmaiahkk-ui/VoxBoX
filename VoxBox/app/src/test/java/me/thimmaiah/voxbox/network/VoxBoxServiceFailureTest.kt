package me.thimmaiah.voxbox.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoxBoxServiceFailureTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(status: Int, body: String) = parseVoxBoxServiceFailure(
        status = status,
        body = body,
        json = json,
        fallbackMessage = "fallback",
    )

    @Test
    fun exhaustedQuotaIsPermanentAndKeepsItsProviderRequestId() {
        val failure = parse(
            429,
            """
            {"error":{
              "code":"transcription_quota_exhausted",
              "message":"This account has no remaining quota.",
              "retryable":false,
              "provider":{"status":429,"type":"insufficient_quota","requestId":"req_1","retryAfterSeconds":null}
            }}
            """.trimIndent(),
        )

        assertEquals(VoxBoxFailureKind.QUOTA_EXHAUSTED, failure.kind)
        assertFalse(failure.retryable)
        assertEquals("req_1", failure.providerRequestId)
        assertNull(failure.retryAfterSeconds)
        assertEquals("This account has no remaining quota.", failure.describe())
    }

    @Test
    fun transientRateLimitStaysRetryableAndSurfacesItsDelay() {
        val failure = parse(
            429,
            """
            {"error":{
              "code":"transcription_rate_limited",
              "message":"Rate limited.",
              "retryable":true,
              "provider":{"status":429,"retryAfterSeconds":12}
            }}
            """.trimIndent(),
        )

        assertEquals(VoxBoxFailureKind.RATE_LIMITED, failure.kind)
        assertTrue(failure.retryable)
        assertEquals(12, failure.retryAfterSeconds)
        assertEquals("Rate limited. (retry after 12s)", failure.describe())
    }

    @Test
    fun rejectedCredentialAndRejectedRequestAreNeverRetried() {
        val auth = parse(
            502,
            """{"error":{"code":"note_auth_error","message":"Bad key.","retryable":false}}""",
        )
        assertEquals(VoxBoxFailureKind.AUTH, auth.kind)
        assertFalse(auth.retryable)

        val rejected = parse(400, """{"error":{"code":"invalid_request","message":"Bad body."}}""")
        assertEquals(VoxBoxFailureKind.REJECTED, rejected.kind)
        assertFalse(rejected.retryable)
    }

    @Test
    fun anUnparseableBodyRetriesOnlyForServerSideStatuses() {
        val gateway = parse(502, "<html>proxy died</html>")
        assertEquals(VoxBoxFailureKind.UNAVAILABLE, gateway.kind)
        assertTrue(gateway.retryable)
        assertEquals("fallback", gateway.message)
        assertEquals("http_502", gateway.code)

        val clientSide = parse(413, "")
        assertEquals(VoxBoxFailureKind.REJECTED, clientSide.kind)
        assertFalse(clientSide.retryable)
    }

    @Test
    fun aTransportFailureIsRetryable() {
        val failure = transportFailure("The transcription service is unavailable.")

        assertEquals(VoxBoxFailureKind.UNAVAILABLE, failure.kind)
        assertTrue(failure.retryable)
        assertEquals("transport_error", failure.code)
    }
}
