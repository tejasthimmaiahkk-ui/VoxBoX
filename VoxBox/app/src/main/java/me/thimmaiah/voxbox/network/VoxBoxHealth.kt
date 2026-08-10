package me.thimmaiah.voxbox.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection

data class VbHealthModels(
    val vision: String?,
    val notes: String?,
    val transcription: String?,
)

data class VbHealthBudget(val used: Int, val limit: Int)

data class VbHealthProbe(
    val latencyMs: Long,
    val mode: String?,
    val models: VbHealthModels?,
    val budget: VbHealthBudget?,
)

private val healthJson = Json { ignoreUnknownKeys = true }

/**
 * Reads `/health` so the connection screen can state what is actually running rather than what
 * the app was built expecting.
 *
 * That distinction has bitten this project once already: a fix was committed but never deployed,
 * and the app kept talking to a server running older code with a different note model. The model
 * names below come from the server's own response for exactly that reason.
 *
 * Returns null on any failure — the caller renders "unreachable", which is the only thing a
 * failed health check can honestly claim.
 */
suspend fun probeVoxBoxHealth(): VbHealthProbe? = runCatching {
    val url = validatedVoxBoxUrl(voxBoxApiEndpoint("/health"))
    val started = System.currentTimeMillis()
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15_000
        readTimeout = 15_000
        applyVoxBoxClientAuth()
    }
    val body = try {
        if (connection.responseCode !in 200..299) return@runCatching null
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
    val latency = System.currentTimeMillis() - started
    val root = healthJson.parseToJsonElement(body).jsonObject
    val models = root["models"]?.jsonObject
    val budget = root["budget"]?.jsonObject
    VbHealthProbe(
        latencyMs = latency,
        mode = root["mode"]?.jsonPrimitive?.content,
        models = models?.let {
            VbHealthModels(
                vision = it["vision"]?.jsonPrimitive?.content,
                notes = it["notes"]?.jsonPrimitive?.content,
                transcription = it["transcription"]?.jsonPrimitive?.content,
            )
        },
        budget = budget?.let {
            VbHealthBudget(
                used = it["used"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                limit = it["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            )
        },
    )
}.getOrNull()
