package me.thimmaiah.voxbox.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.delay

/**
 * Growing wait between attempts for evidence that must not be dropped.
 *
 * The first two steps match the old quick retries, so a single blip still recovers in about three
 * seconds; after that it backs off to a 30-second ceiling instead of giving up, because a
 * classroom with poor coverage drops out for minutes, not seconds.
 */
internal fun offlineBackoffMs(attempt: Int): Long = when (attempt) {
    0 -> 750L
    1 -> 2_000L
    else -> (2_000L shl (attempt - 1).coerceAtMost(4)).coerceAtMost(30_000L)
}

/** Polls rather than registering a callback: the session already loops, and polling cannot leak. */
class NetworkMonitor(context: Context) {
    private val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    fun isOnline(): Boolean {
        val manager = connectivity ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Returns true once a network is available, false if [keepWaiting] says to give up first. */
    suspend fun awaitOnline(keepWaiting: () -> Boolean): Boolean {
        while (!isOnline()) {
            if (!keepWaiting()) return false
            delay(1_000)
        }
        return true
    }
}
