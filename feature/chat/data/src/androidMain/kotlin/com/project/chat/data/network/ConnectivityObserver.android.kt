package com.project.chat.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Observes the network connectivity state on Android devices and emits updates as a stream.
 *
 * ## Strategy / Decisions
 * The Android implementation uses a `callbackFlow` to bridge the callback-based `ConnectivityManager.NetworkCallback`
 * API into a reactive Kotlin Flow. This allows the rest of the application (like websocket connectors or UI state)
 * to consume connection states reactively without managing callback lifecycles.
 *
 * ## How It Works
 * 1. Initializes a `callbackFlow` and immediately checks/emits the initial connection state.
 * 2. Constructs a `ConnectivityManager.NetworkCallback` object.
 * 3. Overrides network state functions:
 * - `onAvailable`: Emits `true`.
 * - `onLost`: Emits `false`.
 * - `onUnavailable`: Emits `false` (e.g., Airplane mode turned on).
 * - `onCapabilitiesChanged`: Re-evaluates capabilities, specifically checking for `NET_CAPABILITY_VALIDATED`, and emits the boolean result.
 * 4. Registers the callback using `registerDefaultNetworkCallback`.
 * 5. Uses `awaitClose` to automatically unregister the callback (`unregisterNetworkCallback`) when flow collection stops, preventing memory leaks.
 *
 * ## Alternatives / Why Not
 * Polling the network state at intervals was not used, as the OS-level callbacks provide instant,
 * push-based updates that are far more battery-efficient and accurate for real-time chat operations.
 *
 * ## Technical Details
 * - Uses `trySend` inside the callbacks since they execute asynchronously outside of the coroutine's direct control.
 * - Relies on `NET_CAPABILITY_VALIDATED` to ensure the network actually has working internet access, not just a local connection.
 *
 * @see android.net.ConnectivityManager.NetworkCallback
 */
actual class ConnectivityObserver(
    private val context: Context,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!

    actual val isConnected: Flow<Boolean> = callbackFlow {
        val initiallyConnected = connectivityManager.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED,
            )
        } ?: false

        send(initiallyConnected)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(false)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val connected = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                )
                trySend(connected)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
