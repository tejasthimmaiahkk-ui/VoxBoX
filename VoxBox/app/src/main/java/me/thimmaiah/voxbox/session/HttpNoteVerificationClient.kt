package me.thimmaiah.voxbox.session

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import me.thimmaiah.voxbox.network.VoxBoxServiceFailure
import me.thimmaiah.voxbox.network.applyVoxBoxClientAuth
import me.thimmaiah.voxbox.network.parseVoxBoxServiceFailure
import me.thimmaiah.voxbox.network.transportFailure
import me.thimmaiah.voxbox.network.validatedVoxBoxUrl
import me.thimmaiah.voxbox.network.voxBoxApiEndpoint

private const val MAX_RESPONSE_BYTES = 512 * 1024
internal const val MAX_VERIFY_MARKDOWN_CHARS = 24_000

class NoteVerificationException(
    message: String,
    cause: Throwable? = null,
    val failure: VoxBoxServiceFailure? = null,
) : Exception(message, cause)

class HttpNoteVerificationClient(
    endpoint: String = voxBoxApiEndpoint("/v1/notes/verify"),
    private val connectTimeoutMillis: Int = 8_000,
    private val readTimeoutMillis: Int = 60_000,
    private val connectionFactory: (URL) -> HttpURLConnection = { url -> url.openConnection() as HttpURLConnection },
) : NoteVerificationClient, Closeable {
    private val endpointUrl = validatedVoxBoxUrl(endpoint)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun verify(
        sessionId: String,
        requestId: String,
        noteMarkdown: String,
        subjectHint: String,
    ): NoteVerification = withContext(Dispatchers.IO) {
        require(sessionId.isNotBlank() && requestId.isNotBlank())
        require(noteMarkdown.isNotBlank()) { "There is nothing to verify." }
        val body = buildJsonObject {
            put("sessionId", sessionId)
            put("requestId", requestId)
            // The proxy bounds this too; trimming here avoids a guaranteed rejection round trip.
            put("noteMarkdown", noteMarkdown.takeLast(MAX_VERIFY_MARKDOWN_CHARS))
            put("subjectHint", subjectHint.take(240))
        }.toString().toByteArray(StandardCharsets.UTF_8)

        val connection = try {
            connectionFactory(endpointUrl)
        } catch (error: IOException) {
            throw NoteVerificationException(
                "The verification service is unavailable.",
                error,
                transportFailure("The verification service is unavailable."),
            )
        }
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = readTimeoutMillis
            connection.doOutput = true
            connection.useCaches = false
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.applyVoxBoxClientAuth()
            connection.setRequestProperty("Cache-Control", "no-store")
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val errorBody = connection.errorStream?.use(::readUtf8Limited).orEmpty()
                val failure = parseVoxBoxServiceFailure(
                    status = status,
                    body = errorBody,
                    json = json,
                    fallbackMessage = "The verification service returned HTTP $status.",
                )
                throw NoteVerificationException(failure.describe(), failure = failure)
            }
            parseNoteVerificationResponse(connection.inputStream.use(::readUtf8Limited), json)
        } catch (error: CancellationException) {
            throw error
        } catch (error: NoteVerificationException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw NoteVerificationException(
                "The verification service timed out.",
                error,
                transportFailure("The verification service timed out."),
            )
        } catch (error: IOException) {
            throw NoteVerificationException(
                "The verification service is unavailable.",
                error,
                transportFailure("The verification service is unavailable."),
            )
        } finally {
            connection.disconnect()
        }
    }

    override fun close() = Unit
}

internal fun parseNoteVerificationResponse(
    responseBody: String,
    json: Json = Json,
): NoteVerification {
    val root = try {
        json.parseToJsonElement(responseBody).jsonObject
    } catch (error: Exception) {
        throw NoteVerificationException("The verification service returned invalid JSON.", error)
    }
    val source = when (root.string("source")) {
        "mock" -> NoteRefinementSource.MOCK
        "openrouter", "openai" -> NoteRefinementSource.PROVIDER
        else -> throw NoteVerificationException("The verification response has an unknown source.")
    }
    val findings = (root["findings"] as? JsonArray)
        ?.mapIndexed { index, element ->
            val item = element as? JsonObject
                ?: throw NoteVerificationException("findings[$index] must be an object.")
            val severity = item.string("severity")
            if (severity !in setOf("info", "warning")) {
                throw NoteVerificationException("findings[$index].severity is invalid.")
            }
            val kind = when (item.string("kind")) {
                "formula" -> VerificationFindingKind.FORMULA
                "concept" -> VerificationFindingKind.CONCEPT
                "units" -> VerificationFindingKind.UNITS
                "terminology" -> VerificationFindingKind.TERMINOLOGY
                "other" -> VerificationFindingKind.OTHER
                else -> throw NoteVerificationException("findings[$index].kind is invalid.")
            }
            val confidence = (item["confidence"] as? JsonPrimitive)
                ?.takeUnless(JsonPrimitive::isString)
                ?.doubleOrNull
                ?: throw NoteVerificationException("findings[$index].confidence must be a number.")
            if (confidence !in 0.0..1.0) {
                throw NoteVerificationException("findings[$index].confidence must be between 0 and 1.")
            }
            VerificationFinding(
                claim = item.requiredText("findings[$index].claim", "claim"),
                issue = item.requiredText("findings[$index].issue", "issue"),
                suggestion = item.requiredText("findings[$index].suggestion", "suggestion"),
                kind = kind,
                severity = severity!!,
                confidence = confidence,
            )
        }
        ?: throw NoteVerificationException("The verification response must contain findings.")

    return NoteVerification(
        sessionId = root.requiredText("sessionId", "sessionId"),
        requestId = root.requiredText("requestId", "requestId"),
        findings = findings,
        checkedFormulas = root.stringList("checkedFormulas"),
        checkedConcepts = root.stringList("checkedConcepts"),
        warnings = root.stringList("warnings"),
        source = source,
    )
}

private fun JsonObject.string(name: String): String? =
    (this[name] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

private fun JsonObject.requiredText(label: String, name: String): String =
    string(name) ?: throw NoteVerificationException("The response field '$label' must be a string.")

private fun JsonObject.stringList(name: String): List<String> =
    (this[name] as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content }
        .orEmpty()

private fun readUtf8Limited(input: InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > MAX_RESPONSE_BYTES) throw NoteVerificationException("The verification response was too large.")
        output.write(buffer, 0, count)
    }
    return output.toString(StandardCharsets.UTF_8.name())
}
