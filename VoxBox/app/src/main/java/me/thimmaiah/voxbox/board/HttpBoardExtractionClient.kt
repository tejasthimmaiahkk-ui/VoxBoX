package me.thimmaiah.voxbox.board

import java.io.IOException
import java.io.InputStream
import java.io.ByteArrayOutputStream
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.thimmaiah.voxbox.network.validatedVoxBoxUrl
import me.thimmaiah.voxbox.network.voxBoxApiEndpoint

private const val MAX_JPEG_BYTES = 8 * 1024 * 1024
private const val MAX_RESPONSE_BYTES = 1024 * 1024

class BoardExtractionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class HttpBoardExtractionClient(
    endpoint: String = voxBoxApiEndpoint("/v1/board/extract"),
    private val connectTimeoutMillis: Int = 8_000,
    private val readTimeoutMillis: Int = 50_000,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) : BoardExtractionClient {
    private val endpointUrl = validatedVoxBoxUrl(endpoint)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    override suspend fun extract(jpegBytes: ByteArray): BoardExtraction = withContext(Dispatchers.IO) {
        requireValidJpeg(jpegBytes)
        val requestBody = buildJsonObject {
            put("imageBase64", Base64.getEncoder().encodeToString(jpegBytes))
            put("mimeType", "image/jpeg")
        }.toString().toByteArray(StandardCharsets.UTF_8)

        val connection = try {
            connectionFactory(endpointUrl)
        } catch (error: IOException) {
            throw BoardExtractionException("The board extraction service is unavailable.", error)
        }

        try {
            configure(connection, requestBody.size)
            connection.outputStream.use { output -> output.write(requestBody) }
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                val errorBody = connection.errorStream?.use(::readUtf8Limited).orEmpty()
                val detail = parseServerError(errorBody)
                throw BoardExtractionException(
                    detail ?: "The board extraction service returned HTTP $statusCode.",
                )
            }
            val responseBody = connection.inputStream.use(::readUtf8Limited)
            parseBoardExtractionResponse(responseBody, json)
        } catch (error: CancellationException) {
            throw error
        } catch (error: BoardExtractionException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw BoardExtractionException("The board extraction service timed out.", error)
        } catch (error: IOException) {
            throw BoardExtractionException("The board extraction service is unavailable.", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun requireValidJpeg(jpegBytes: ByteArray) {
        if (jpegBytes.isEmpty()) {
            throw BoardExtractionException("The captured frame was empty.")
        }
        if (jpegBytes.size > MAX_JPEG_BYTES) {
            throw BoardExtractionException("The captured frame exceeds the 8 MiB service limit.")
        }
        val hasJpegHeader =
            jpegBytes.size >= 2 &&
                jpegBytes[0] == 0xFF.toByte() &&
                jpegBytes[1] == 0xD8.toByte()
        if (!hasJpegHeader) {
            throw BoardExtractionException("The captured frame is not a valid JPEG.")
        }
    }

    private fun configure(connection: HttpURLConnection, contentLength: Int) {
        connection.requestMethod = "POST"
        connection.connectTimeout = connectTimeoutMillis
        connection.readTimeout = readTimeoutMillis
        connection.doOutput = true
        connection.useCaches = false
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Cache-Control", "no-store")
        connection.setFixedLengthStreamingMode(contentLength)
    }

    private fun parseServerError(body: String): String? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val error = root["error"] as? JsonObject ?: return@runCatching null
        error.requiredString("message").take(300)
    }.getOrNull()
}

internal fun parseBoardExtractionResponse(
    responseBody: String,
    json: Json = Json,
): BoardExtraction {
    val root = try {
        json.parseToJsonElement(responseBody).jsonObject
    } catch (error: Exception) {
        throw BoardExtractionException("The board extraction service returned invalid JSON.", error)
    }

    val title = root.requiredString("title").trim()
    val summary = root.requiredString("summary").trim()
    val visibleText = root.requiredStringList("visibleText")
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("\n")
    val concepts = root.requiredStringList("concepts").normalizeList()
    val equations = root.optionalStringList("equations").normalizeList()
    val diagramRegions = root.optionalDiagramRegions()
    val warnings = root.requiredStringList("warnings").normalizeList()
    val confidencePrimitive = root["confidence"] as? JsonPrimitive
        ?: throw BoardExtractionException("The response field 'confidence' must be a number.")
    val confidence = confidencePrimitive
        .takeUnless(JsonPrimitive::isString)
        ?.doubleOrNull
        ?.takeIf { it.isFinite() && it in 0.0..1.0 }
        ?: throw BoardExtractionException("The response field 'confidence' must be between 0 and 1.")

    val source = when (root["source"]?.jsonPrimitive?.contentOrNull) {
        "mock" -> BoardExtractionSource.MOCK_PROXY
        else -> BoardExtractionSource.REMOTE_VISION
    }
    return BoardExtraction(
        title = title,
        summary = summary,
        visibleText = visibleText,
        concepts = concepts,
        confidence = confidence,
        warnings = warnings,
        source = source,
        equations = equations,
        diagramRegions = diagramRegions,
    )
}

private fun JsonObject.requiredString(name: String): String {
    val value = this[name] as? JsonPrimitive
    if (value == null || !value.isString) {
        throw BoardExtractionException("The response field '$name' must be a string.")
    }
    return value.content
}

private fun JsonObject.requiredStringList(name: String): List<String> {
    val values = this[name] as? JsonArray
        ?: throw BoardExtractionException("The response field '$name' must be a list.")
    return values.mapIndexed { index, value ->
        val primitive = value as? JsonPrimitive
        if (primitive == null || !primitive.isString) {
            throw BoardExtractionException("The response field '$name[$index]' must be a string.")
        }
        primitive.content
    }
}

private fun JsonObject.optionalStringList(name: String): List<String> {
    if (this[name] == null) return emptyList()
    return requiredStringList(name)
}

private fun JsonObject.optionalDiagramRegions(): List<DiagramRegion> {
    val values = this["diagramRegions"] ?: return emptyList()
    val array = values as? JsonArray
        ?: throw BoardExtractionException("The response field 'diagramRegions' must be a list.")
    return array.mapIndexed { index, value ->
        val region = value as? JsonObject
            ?: throw BoardExtractionException("The response field 'diagramRegions[$index]' must be an object.")
        val parsed = DiagramRegion(
            left = region.requiredNormalizedNumber("left", index),
            top = region.requiredNormalizedNumber("top", index),
            width = region.requiredNormalizedNumber("width", index),
            height = region.requiredNormalizedNumber("height", index),
            caption = region.requiredString("caption").trim(),
        )
        if (!parsed.isValid()) {
            throw BoardExtractionException("The response field 'diagramRegions[$index]' exceeds the frame bounds.")
        }
        parsed
    }
}

private fun JsonObject.requiredNormalizedNumber(name: String, regionIndex: Int): Double {
    val primitive = this[name] as? JsonPrimitive
        ?: throw BoardExtractionException("The response field 'diagramRegions[$regionIndex].$name' must be a number.")
    return primitive.takeUnless(JsonPrimitive::isString)?.doubleOrNull
        ?.takeIf { it.isFinite() && it in 0.0..1.0 }
        ?: throw BoardExtractionException(
            "The response field 'diagramRegions[$regionIndex].$name' must be between 0 and 1.",
        )
}

private fun List<String>.normalizeList(): List<String> = map(String::trim)
    .filter(String::isNotBlank)
    .distinctBy(String::lowercase)

private fun readUtf8Limited(input: InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > MAX_RESPONSE_BYTES) {
            throw BoardExtractionException("The board extraction response was too large.")
        }
        output.write(buffer, 0, count)
    }
    return output.toString(StandardCharsets.UTF_8.name())
}
