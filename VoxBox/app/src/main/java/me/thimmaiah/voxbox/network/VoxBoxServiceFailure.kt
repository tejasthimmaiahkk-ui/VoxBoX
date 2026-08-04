package me.thimmaiah.voxbox.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** How the proxy classified an upstream provider failure. */
enum class VoxBoxFailureKind {
    /** The account has no remaining provider quota. Retrying cannot succeed. */
    QUOTA_EXHAUSTED,

    /** A transient per-minute limit. Retrying later can succeed. */
    RATE_LIMITED,

    /** The proxy's own provider credential was rejected. */
    AUTH,

    /** The request itself was rejected; replaying the same bytes cannot succeed. */
    REJECTED,

    /** Anything else, including transport failures and malformed provider output. */
    UNAVAILABLE,
}

/**
 * Structured view of a proxy error response.
 *
 * The proxy preserves upstream `429` and reports whether a retry can ever succeed, so the client
 * can stop burning attempts (and retaining more audio) on a permanently exhausted account.
 */
data class VoxBoxServiceFailure(
    val kind: VoxBoxFailureKind,
    val code: String,
    val message: String,
    val retryable: Boolean,
    val retryAfterSeconds: Int? = null,
    val providerRequestId: String? = null,
) {
    /** A short line for the note warning and the live review panel. */
    fun describe(): String = when {
        retryAfterSeconds != null && retryable -> "$message (retry after ${retryAfterSeconds}s)"
        else -> message
    }
}

/**
 * Parses the shared `{ "error": { code, message, retryable, provider } }` envelope.
 *
 * An unparseable body is treated as retryable only when the status is a server-side failure, so a
 * rejected request is never retried and a gateway hiccup still is.
 */
internal fun parseVoxBoxServiceFailure(
    status: Int,
    body: String,
    json: Json,
    fallbackMessage: String,
): VoxBoxServiceFailure {
    val error = runCatching {
        (json.parseToJsonElement(body) as? JsonObject)?.get("error") as? JsonObject
    }.getOrNull()
    val code = error?.string("code").orEmpty()
    val message = error?.string("message")?.take(300)?.takeIf(String::isNotBlank) ?: fallbackMessage
    val provider = error?.get("provider") as? JsonObject
    val declaredRetryable = (error?.get("retryable") as? JsonPrimitive)?.booleanOrNull
    val kind = when {
        code.endsWith("_quota_exhausted") -> VoxBoxFailureKind.QUOTA_EXHAUSTED
        code.endsWith("_rate_limited") -> VoxBoxFailureKind.RATE_LIMITED
        code.endsWith("_auth_error") || code == "openai_not_configured" -> VoxBoxFailureKind.AUTH
        code.endsWith("_request_rejected") || status == 400 || status == 409 ||
            status == 413 || status == 415 -> VoxBoxFailureKind.REJECTED
        else -> VoxBoxFailureKind.UNAVAILABLE
    }
    val retryable = declaredRetryable ?: when (kind) {
        VoxBoxFailureKind.QUOTA_EXHAUSTED, VoxBoxFailureKind.AUTH, VoxBoxFailureKind.REJECTED -> false
        VoxBoxFailureKind.RATE_LIMITED -> true
        VoxBoxFailureKind.UNAVAILABLE -> status >= 500
    }
    return VoxBoxServiceFailure(
        kind = kind,
        code = code.ifBlank { "http_$status" },
        message = message,
        retryable = retryable,
        retryAfterSeconds = (provider?.get("retryAfterSeconds") as? JsonPrimitive)
            ?.takeUnless(JsonPrimitive::isString)
            ?.intOrNull
            ?.takeIf { it >= 0 },
        providerRequestId = provider?.string("requestId")?.takeIf(String::isNotBlank),
    )
}

/** A transport-level failure the proxy never answered; retrying the same chunk is reasonable. */
internal fun transportFailure(message: String): VoxBoxServiceFailure = VoxBoxServiceFailure(
    kind = VoxBoxFailureKind.UNAVAILABLE,
    code = "transport_error",
    message = message,
    retryable = true,
)

private fun JsonObject.string(name: String): String? =
    (this[name] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull
