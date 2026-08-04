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

    @Test
    fun proxyLevelAuthAndBudgetFailuresAreClassified() {
        val unauthorized = parse(
            401,
            """{"error":{"code":"unauthorized","message":"A valid client token is required.","retryable":false}}""",
        )
        assertEquals(VoxBoxFailureKind.AUTH, unauthorized.kind)
        assertFalse(unauthorized.retryable)

        val unconfigured = parse(
            503,
            """{"error":{"code":"client_auth_not_configured","message":"No token configured.","retryable":false}}""",
        )
        assertEquals(VoxBoxFailureKind.AUTH, unconfigured.kind)

        val budget = parse(
            429,
            """{"error":{"code":"daily_budget_exhausted","message":"Daily limit reached.","retryable":false}}""",
        )
        assertEquals(VoxBoxFailureKind.QUOTA_EXHAUSTED, budget.kind)
        assertFalse(budget.retryable)
    }

    @Test
    fun proxyRateLimitReportsItsDelayFromTheErrorItself() {
        val failure = parse(
            429,
            """{"error":{"code":"rate_limited","message":"Too many requests.","retryable":true,"retryAfterSeconds":9}}""",
        )

        assertEquals(VoxBoxFailureKind.RATE_LIMITED, failure.kind)
        assertTrue(failure.retryable)
        assertEquals(9, failure.retryAfterSeconds)
        assertEquals("Too many requests. (retry after 9s)", failure.describe())
    }
}
