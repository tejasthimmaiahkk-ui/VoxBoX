package me.thimmaiah.voxbox.network

import java.net.URI
import java.net.URL
import me.thimmaiah.voxbox.BuildConfig

private const val UNCONFIGURED_RELEASE_HOST = "invalid.voxbox.local"

internal fun voxBoxApiEndpoint(path: String): String {
    require(path.startsWith('/') && !path.startsWith("//")) {
        "The VoxBox API path must start with exactly one '/'."
    }
    val baseUrl = BuildConfig.VOXBOX_API_BASE_URL.trim().trimEnd('/')
    check(baseUrl.isNotBlank()) { "VOXBOX_API_BASE_URL is not configured." }
    return "$baseUrl$path"
}

/**
 * Validates the final endpoint before a client opens a connection. Debug builds deliberately allow
 * HTTP for the local `adb reverse` workflow; release builds only accept HTTPS.
 */
internal fun validatedVoxBoxUrl(endpoint: String): URL {
    val normalized = endpoint.trim()
    val uri = try {
        URI(normalized)
    } catch (error: Exception) {
        throw IllegalStateException("The VoxBox API endpoint is not a valid URL.", error)
    }
    check(uri.isAbsolute && !uri.host.isNullOrBlank() && uri.scheme in setOf("http", "https")) {
        "The VoxBox API endpoint must be an absolute HTTP(S) URL."
    }
    check(uri.userInfo == null && uri.fragment == null) {
        "The VoxBox API endpoint must not contain credentials or a fragment."
    }
    if (!BuildConfig.DEBUG) {
        check(uri.scheme == "https") { "Release builds require an HTTPS VoxBox API endpoint." }
        check(uri.host != UNCONFIGURED_RELEASE_HOST) {
            "The release VoxBox API endpoint is not configured. Set VOXBOX_API_BASE_URL when building."
        }
    }
    return try {
        uri.toURL()
    } catch (error: Exception) {
        throw IllegalStateException("The VoxBox API endpoint cannot be opened.", error)
    }
}
