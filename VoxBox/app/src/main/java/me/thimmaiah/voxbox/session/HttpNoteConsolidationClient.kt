package me.thimmaiah.voxbox.session

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import me.thimmaiah.voxbox.network.VoxBoxServiceFailure
import me.thimmaiah.voxbox.network.applyVoxBoxClientAuth
import me.thimmaiah.voxbox.network.parseVoxBoxServiceFailure
import me.thimmaiah.voxbox.network.transportFailure
import me.thimmaiah.voxbox.network.validatedVoxBoxUrl
import me.thimmaiah.voxbox.network.voxBoxApiEndpoint

internal const val MAX_CONSOLIDATE_MARKDOWN_CHARS = 60_000
private const val MAX_CONSOLIDATE_RESPONSE_BYTES = 1024 * 1024

data class NoteConsolidation(
    val markdown: String,
    val mergedTopics: List<String>,
)

class NoteConsolidationException(
    message: String,
    cause: Throwable? = null,
    val failure: VoxBoxServiceFailure? = null,
) : Exception(message, cause)

/**
 * The Stop-time pass that merges topics the lecturer explained more than once.
 *
 * Live deltas can only append, so repetition piles up as duplicate headings. This one call
 * rewrites the session's note once; the proxy rejects a result that loses a diagram link or most
 * of the note, and the caller keeps the pre-consolidation Markdown on disk.
 */
class HttpNoteConsolidationClient(
    endpoint: String = voxBoxApiEndpoint("/v1/notes/consolidate"),
    private val connectTimeoutMillis: Int = 8_000,
    private val readTimeoutMillis: Int = 110_000,
) {
    private val endpointUrl = validatedVoxBoxUrl(endpoint)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun consolidate(
        sessionId: String,
        requestId: String,
        noteMarkdown: String,
        subjectHint: String,
    ): NoteConsolidation = withContext(Dispatchers.IO) {
        require(noteMarkdown.length <= MAX_CONSOLIDATE_MARKDOWN_CHARS) { "The note is too long to consolidate." }
        val body = buildJsonObject {
            put("sessionId", sessionId)
            put("requestId", requestId)
            put("noteMarkdown", noteMarkdown)
            put("subjectHint", subjectHint.take(240))
        }.toString().toByteArray(StandardCharsets.UTF_8)
        val connection = try {
            endpointUrl.openConnection() as HttpURLConnection
        } catch (error: IOException) {
            throw NoteConsolidationException("The note service is unavailable.", error, transportFailure("The note service is unavailable."))
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
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val failure = parseVoxBoxServiceFailure(
                    status = status,
                    body = connection.errorStream?.use(::readLimited).orEmpty(),
                    json = json,
                    fallbackMessage = "The note service returned HTTP $status.",
                )
                throw NoteConsolidationException(failure.describe(), failure = failure)
            }
            val root = json.parseToJsonElement(connection.inputStream.use(::readLimited)).jsonObject
            val markdown = (root["markdown"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                ?: throw NoteConsolidationException("The consolidated note was empty.")
            val topics = (root["mergedTopics"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }
            NoteConsolidation(markdown, topics)
        } catch (error: CancellationException) {
            throw error
        } catch (error: NoteConsolidationException) {
            throw error
        } catch (error: IOException) {
            throw NoteConsolidationException("The note service is unavailable.", error, transportFailure("The note service is unavailable."))
        } finally {
            connection.disconnect()
        }
    }
}

private fun readLimited(input: InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8_192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (output.size() + count > MAX_CONSOLIDATE_RESPONSE_BYTES) throw IOException("The note response is too large.")
        output.write(buffer, 0, count)
    }
    return output.toString(StandardCharsets.UTF_8.name())
}
