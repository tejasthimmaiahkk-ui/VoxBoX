package me.thimmaiah.voxbox.audio

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import me.thimmaiah.voxbox.network.VoxBoxServiceFailure
import me.thimmaiah.voxbox.network.applyVoxBoxClientAuth
import me.thimmaiah.voxbox.network.parseVoxBoxServiceFailure
import me.thimmaiah.voxbox.network.transportFailure
import me.thimmaiah.voxbox.network.validatedVoxBoxUrl
import me.thimmaiah.voxbox.network.voxBoxApiEndpoint

private const val MAX_AUDIO_BYTES = 10 * 1024 * 1024
private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024

class AudioTranscriptionException(
    message: String,
    cause: Throwable? = null,
    /** Present when the proxy answered with a classified failure. */
    val failure: VoxBoxServiceFailure? = null,
) : Exception(message, cause) {
    /** False only when the proxy stated that retrying this exact chunk cannot succeed. */
    val retryable: Boolean
        get() = failure?.retryable ?: true
}

class HttpAudioTranscriptionClient(
    endpoint: String = voxBoxApiEndpoint("/v1/audio/transcribe"),
    private val connectTimeoutMillis: Int = 8_000,
    private val readTimeoutMillis: Int = 65_000,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) : AudioTranscriptionClient, Closeable {
    private val endpointUrl = validatedVoxBoxUrl(endpoint)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun transcribe(
        sessionId: String,
        chunkId: String,
        offsetMs: Long,
        wavBytes: ByteArray,
        language: String,
    ): AudioTranscription = withContext(Dispatchers.IO) {
        requireWav(wavBytes)
        val body = buildJsonObject {
            put("sessionId", sessionId)
            put("chunkId", chunkId)
            put("offsetMs", offsetMs)
            put("mimeType", "audio/wav")
            put("audioBase64", Base64.getEncoder().encodeToString(wavBytes))
            put("language", language)
        }.toString().toByteArray(StandardCharsets.UTF_8)
        val connection = try {
            connectionFactory(endpointUrl)
        } catch (error: IOException) {
            throw AudioTranscriptionException(
                "The transcription service is unavailable.",
                error,
                transportFailure("The transcription service is unavailable."),
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
                    fallbackMessage = "The transcription service returned HTTP $status.",
                )
                throw AudioTranscriptionException(failure.describe(), failure = failure)
            }
            parseAudioTranscriptionResponse(connection.inputStream.use(::readUtf8Limited), json)
        } catch (error: CancellationException) {
            throw error
        } catch (error: AudioTranscriptionException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw AudioTranscriptionException(
                "The transcription service timed out.",
                error,
                transportFailure("The transcription service timed out."),
            )
        } catch (error: IOException) {
            throw AudioTranscriptionException(
                "The transcription service is unavailable.",
                error,
                transportFailure("The transcription service is unavailable."),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun requireWav(bytes: ByteArray) {
        if (bytes.size > MAX_AUDIO_BYTES) throw AudioTranscriptionException("The audio chunk exceeds 10 MiB.")
        val valid = bytes.size >= 44 &&
            bytes.copyOfRange(0, 4).toString(StandardCharsets.US_ASCII) == "RIFF" &&
            bytes.copyOfRange(8, 12).toString(StandardCharsets.US_ASCII) == "WAVE"
        if (!valid) throw AudioTranscriptionException("The audio chunk is not a valid WAV file.")
    }

    override fun close() = Unit
}

internal fun parseAudioTranscriptionResponse(
    responseBody: String,
    json: Json = Json,
): AudioTranscription {
    val root = try {
        json.parseToJsonElement(responseBody).jsonObject
    } catch (error: Exception) {
        throw AudioTranscriptionException("The transcription service returned invalid JSON.", error)
    }
    val source = when (root.requiredString("source")) {
        "mock" -> TranscriptionSource.MOCK
        "openai" -> TranscriptionSource.OPENAI
        else -> throw AudioTranscriptionException("The transcription response has an unknown source.")
    }
    val segments = (root["segments"] as? JsonArray)
        ?.mapIndexed { index, value ->
            val item = value as? JsonObject
                ?: throw AudioTranscriptionException("segments[$index] must be an object.")
            val startMs = item.requiredLong("startMs", index)
            val endMs = item.requiredLong("endMs", index)
            if (endMs < startMs) throw AudioTranscriptionException("segments[$index] has invalid timestamps.")
            TranscribedSegment(
                id = item.requiredString("id"),
                speakerId = item.requiredString("speakerId"),
                startMs = startMs,
                endMs = endMs,
                text = item.requiredString("text").trim(),
            )
        }
        ?: throw AudioTranscriptionException("The transcription response must contain segments.")
    return AudioTranscription(
        sessionId = root.requiredString("sessionId"),
        chunkId = root.requiredString("chunkId"),
        text = root.requiredString("text"),
        durationMs = root.requiredLong("durationMs"),
        segments = segments,
        source = source,
    )
}

private fun JsonObject.requiredString(name: String): String {
    val value = this[name] as? JsonPrimitive
    if (value == null || !value.isString) throw AudioTranscriptionException("The response field '$name' must be a string.")
    return value.content
}

private fun JsonObject.requiredLong(name: String, segmentIndex: Int? = null): Long {
    val value = (this[name] as? JsonPrimitive)?.takeUnless(JsonPrimitive::isString)?.longOrNull
    return value ?: throw AudioTranscriptionException(
        if (segmentIndex == null) "The response field '$name' must be an integer."
        else "The response field 'segments[$segmentIndex].$name' must be an integer.",
    )
}

private fun readUtf8Limited(input: InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > MAX_RESPONSE_BYTES) throw AudioTranscriptionException("The transcription response was too large.")
        output.write(buffer, 0, count)
    }
    return output.toString(StandardCharsets.UTF_8.name())
}
