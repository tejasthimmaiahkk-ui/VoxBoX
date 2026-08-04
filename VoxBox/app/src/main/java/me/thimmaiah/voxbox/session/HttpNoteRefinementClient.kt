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
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import me.thimmaiah.voxbox.network.validatedVoxBoxUrl
import me.thimmaiah.voxbox.network.voxBoxApiEndpoint

private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
private const val MAX_NOTE_OUTLINE_CHARS = 12_000
private const val MAX_RECENT_MARKDOWN_CHARS = 24_000
private const val MAX_SYLLABUS_EXCERPTS = 8
private const val MAX_SYLLABUS_EXCERPT_CHARS = 2_000
private const val MAX_FORWARDED_SYLLABUS_CHARS = 12_000
private val SHA256_PATTERN = Regex("[0-9a-f]{64}")

class NoteRefinementException(message: String, cause: Throwable? = null) : Exception(message, cause)

class HttpNoteRefinementClient(
    endpoint: String = voxBoxApiEndpoint("/v1/notes/refine"),
    private val connectTimeoutMillis: Int = 8_000,
    private val readTimeoutMillis: Int = 60_000,
    private val connectionFactory: (URL) -> HttpURLConnection = { url -> url.openConnection() as HttpURLConnection },
) : NoteRefinementClient, Closeable {
    private val endpointUrl = validatedVoxBoxUrl(endpoint)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun refine(request: NoteRefinementRequest): NoteRefinement = withContext(Dispatchers.IO) {
        require(request.requestId.isNotBlank() && request.sessionId.isNotBlank())
        require(request.baseRevision >= 0)
        require(request.transcriptSegments.isNotEmpty() || request.boardEvidence != null)
        request.requireValidIncrementalContract()
        val body = request.toJson().toString().toByteArray(StandardCharsets.UTF_8)
        val connection = try {
            connectionFactory(endpointUrl)
        } catch (error: IOException) {
            throw NoteRefinementException("The note service is unavailable.", error)
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
            connection.setRequestProperty("Cache-Control", "no-store")
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val errorBody = connection.errorStream?.use(::readUtf8Limited).orEmpty()
                val detail = runCatching {
                    json.parseToJsonElement(errorBody).jsonObject["error"]
                        ?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                throw NoteRefinementException(detail?.take(300) ?: "The note service returned HTTP $status.")
            }
            val result = parseNoteRefinementResponse(connection.inputStream.use(::readUtf8Limited), json)
            if (result.requestId != request.requestId || result.sessionId != request.sessionId ||
                result.baseRevision != request.baseRevision || result.nextRevision != request.baseRevision + 1
            ) {
                throw NoteRefinementException("The note response does not match this request revision.")
            }
            when (request.responseMode) {
                NoteRefinementResponseMode.FULL -> if (result.updateMode != NoteRefinementUpdateMode.FULL) {
                    throw NoteRefinementException("The note service returned a delta for a full-note request.")
                }
                NoteRefinementResponseMode.DELTA -> {
                    val expectedHash = requireNotNull(request.noteContext).contentSha256
                    if (
                        result.updateMode != NoteRefinementUpdateMode.DELTA ||
                        result.baseContentSha256 != expectedHash
                    ) {
                        throw NoteRefinementException("The note delta does not match the current note content.")
                    }
                }
            }
            result
        } catch (error: CancellationException) {
            throw error
        } catch (error: NoteRefinementException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw NoteRefinementException("The note service timed out.", error)
        } catch (error: IOException) {
            throw NoteRefinementException("The note service is unavailable.", error)
        } finally {
            connection.disconnect()
        }
    }

    override fun close() = Unit
}

private fun NoteRefinementRequest.requireValidIncrementalContract() {
    require(syllabusExcerpts.size <= MAX_SYLLABUS_EXCERPTS) {
        "At most $MAX_SYLLABUS_EXCERPTS syllabus excerpts may be sent."
    }
    require(syllabusExcerpts.sumOf { it.text.length } <= MAX_FORWARDED_SYLLABUS_CHARS) {
        "Syllabus excerpts may contain at most $MAX_FORWARDED_SYLLABUS_CHARS text characters in total."
    }
    syllabusExcerpts.forEach { excerpt ->
        require(excerpt.id.isNotBlank() && excerpt.id.length <= 128) {
            "Each syllabus excerpt requires an ID of at most 128 characters."
        }
        require(excerpt.heading.length <= 240) {
            "Each syllabus excerpt heading must be at most 240 characters."
        }
        require(excerpt.text.isNotBlank() && excerpt.text.length <= MAX_SYLLABUS_EXCERPT_CHARS) {
            "Each syllabus excerpt requires 1 to $MAX_SYLLABUS_EXCERPT_CHARS text characters."
        }
    }
    if (responseMode == NoteRefinementResponseMode.DELTA) {
        require(existingMarkdown.isBlank()) {
            "Delta refinement must not upload the complete existing note."
        }
        val context = requireNotNull(noteContext) { "Delta refinement requires bounded note context." }
        require(context.title.length <= 240)
        require(context.outlineMarkdown.length <= MAX_NOTE_OUTLINE_CHARS)
        require(context.recentMarkdown.length <= MAX_RECENT_MARKDOWN_CHARS)
        require(SHA256_PATTERN.matches(context.contentSha256)) {
            "Delta refinement requires a lowercase SHA-256 content hash."
        }
    }
}

private fun NoteRefinementRequest.toJson(): JsonObject = buildJsonObject {
    put("requestId", requestId)
    put("sessionId", sessionId)
    put("baseRevision", baseRevision)
    put("mode", mode.name.lowercase())
    put("notePolicy", notePolicy.name.lowercase())
    put("primarySpeakerId", primarySpeakerId.orEmpty())
    put("syllabusContext", syllabusContext)
    put("existingMarkdown", existingMarkdown)
    if (syllabusExcerpts.isNotEmpty()) {
        put("syllabusExcerpts", buildJsonArray {
            syllabusExcerpts.forEach { excerpt ->
                add(buildJsonObject {
                    put("id", excerpt.id)
                    put("heading", excerpt.heading)
                    put("text", excerpt.text)
                })
            }
        })
    }
    if (responseMode == NoteRefinementResponseMode.DELTA) {
        put("responseMode", "delta")
        val context = requireNotNull(noteContext)
        put("noteContext", buildJsonObject {
            put("title", context.title)
            put("outlineMarkdown", context.outlineMarkdown)
            put("recentMarkdown", context.recentMarkdown)
            put("contentSha256", context.contentSha256)
        })
    }
    put("transcriptSegments", buildJsonArray {
        transcriptSegments.forEach { segment ->
            add(buildJsonObject {
                put("id", segment.id)
                put("speakerId", segment.speakerId.orEmpty())
                put("startMs", segment.startMs)
                put("endMs", segment.endMs)
                put("text", segment.text)
                put("isPrimarySpeaker", segment.isPrimarySpeaker)
            })
        }
    })
    boardEvidence?.let { board ->
        put("boardEvidence", buildJsonObject {
            put("id", board.id)
            put("capturedAtMs", board.capturedAtMs)
            put("summary", board.summary)
            put("visibleText", board.visibleText.toJsonArray())
            put("concepts", board.concepts.toJsonArray())
            put("equations", board.equations.toJsonArray())
            put("diagramCaptions", board.diagramCaptions.toJsonArray())
        })
    } ?: put("boardEvidence", kotlinx.serialization.json.JsonNull)
}

private fun List<String>.toJsonArray(): JsonArray = buildJsonArray { forEach { add(JsonPrimitive(it)) } }

internal fun parseNoteRefinementResponse(
    responseBody: String,
    json: Json = Json,
): NoteRefinement {
    val root = try {
        json.parseToJsonElement(responseBody).jsonObject
    } catch (error: Exception) {
        throw NoteRefinementException("The note service returned invalid JSON.", error)
    }
    val corrections = root.requiredObjectList("corrections").mapIndexed { index, item ->
        val severity = item.requiredString("severity")
        if (severity !in setOf("info", "warning")) {
            throw NoteRefinementException("corrections[$index].severity is invalid.")
        }
        SuggestedCorrection(
            captured = item.requiredString("captured"),
            suggested = item.requiredString("suggested"),
            reason = item.requiredString("reason"),
            severity = severity,
            evidenceIds = item.requiredStringList("evidenceIds"),
        )
    }
    val source = when (root.requiredString("source")) {
        "openai" -> NoteRefinementSource.OPENAI
        "mock" -> NoteRefinementSource.MOCK
        else -> throw NoteRefinementException("The note response has an unknown source.")
    }
    val updateMode = when ((root["updateMode"] as? JsonPrimitive)?.contentOrNull) {
        null, "full" -> NoteRefinementUpdateMode.FULL
        "delta" -> NoteRefinementUpdateMode.DELTA
        else -> throw NoteRefinementException("The note response has an unknown update mode.")
    }
    val markdown: String
    val markdownDelta: String
    val baseContentSha256: String
    when (updateMode) {
        NoteRefinementUpdateMode.FULL -> {
            markdown = root.requiredString("markdown")
            if (markdown.isBlank()) throw NoteRefinementException("The full note response is empty.")
            markdownDelta = ""
            baseContentSha256 = ""
        }
        NoteRefinementUpdateMode.DELTA -> {
            markdown = ""
            markdownDelta = root.requiredString("markdownDelta")
            baseContentSha256 = root.requiredString("baseContentSha256")
            if (markdownDelta.isBlank()) throw NoteRefinementException("The note delta is empty.")
            if (!SHA256_PATTERN.matches(baseContentSha256)) {
                throw NoteRefinementException("The note delta has an invalid base content hash.")
            }
        }
    }
    return NoteRefinement(
        requestId = root.requiredString("requestId"),
        sessionId = root.requiredString("sessionId"),
        baseRevision = root.requiredLong("baseRevision"),
        nextRevision = root.requiredLong("nextRevision"),
        title = root.requiredString("title"),
        markdown = markdown,
        corrections = corrections,
        consumedEvidenceIds = root.requiredStringList("consumedEvidenceIds"),
        warnings = root.requiredStringList("warnings"),
        source = source,
        updateMode = updateMode,
        baseContentSha256 = baseContentSha256,
        markdownDelta = markdownDelta,
    )
}

private fun JsonObject.requiredString(name: String): String {
    val value = this[name] as? JsonPrimitive
    if (value == null || !value.isString) throw NoteRefinementException("The response field '$name' must be a string.")
    return value.content
}

private fun JsonObject.requiredLong(name: String): Long =
    (this[name] as? JsonPrimitive)?.takeUnless(JsonPrimitive::isString)?.longOrNull
        ?: throw NoteRefinementException("The response field '$name' must be an integer.")

private fun JsonObject.requiredStringList(name: String): List<String> {
    val array = this[name] as? JsonArray
        ?: throw NoteRefinementException("The response field '$name' must be a list.")
    return array.mapIndexed { index, value ->
        val primitive = value as? JsonPrimitive
        if (primitive == null || !primitive.isString) {
            throw NoteRefinementException("The response field '$name[$index]' must be a string.")
        }
        primitive.content
    }
}

private fun JsonObject.requiredObjectList(name: String): List<JsonObject> {
    val array = this[name] as? JsonArray
        ?: throw NoteRefinementException("The response field '$name' must be a list.")
    return array.mapIndexed { index, value ->
        value as? JsonObject
            ?: throw NoteRefinementException("The response field '$name[$index]' must be an object.")
    }
}

private fun readUtf8Limited(input: InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > MAX_RESPONSE_BYTES) throw NoteRefinementException("The note response was too large.")
        output.write(buffer, 0, count)
    }
    return output.toString(StandardCharsets.UTF_8.name())
}
