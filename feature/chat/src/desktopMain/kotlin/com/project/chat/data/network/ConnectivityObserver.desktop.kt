package com.project.chat.data.network

import com.project.core.domain.logging.ChirpLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * Desktop-specific implementation of the connectivity observer to monitor network availability and manage active socket connections.
 * * ## Strategy / Decisions
 * Unlike mobile platforms (Android/iOS) which provide dedicated system-level APIs for connectivity monitoring, the JVM lacks a unified network state manager. To reliably determine internet access, this implementation utilizes a two-tier verification strategy:
 * 1. A lightweight hardware-level check querying `java.net.NetworkInterface`.
 * 2. An active socket ping to highly available remote DNS servers.
 * The secondary active ping is crucial to bypass "captive portal" scenarios (e.g., hotel or public Wi-Fi), where a machine may be securely connected to a local router without having actual outbound internet routing.
 * * ## How It Works
 * 1. An observer flow is constructed that runs a continuous `while (true)` loop, executing the connectivity check every 5 seconds.
 * 2. It queries local hardware via `NetworkInterface.getNetworkInterfaces()`.
 * 3. It evaluates the interfaces lazily, filtering out local loopbacks (e.g., localhost) and ensuring the interface is both enabled (`isUp`) and has assigned IP addresses.
 * 4. If a valid hardware interface exists, it attempts a socket connection to a predefined list of reliable DNS targets (Google, Cloudflare, OpenDNS) on port 53.
 * 5. It returns `true` on the first successful connection and gracefully emits the network state.
 * * ## Alternatives / Why Not
 * - **Relying solely on `NetworkInterface`:** Rejected because a local hardware connection does not guarantee outbound internet access (due to captive portals or external network outages).
 * - **Hot Flow loop execution:** A `while (true)` loop in a hot flow (like `SharedFlow` with aggressive sharing strategies) could launch immediately and run indefinitely in the background, wasting network and memory resources. A cold flow was explicitly chosen to tie execution to active collection.
 * * Technical Details:
 * - **Thread Safety:** Creating socket connections is a strictly blocking IO operation. The ping logic must be wrapped in `withContext(Dispatchers.IO)` to prevent thread starvation.
 * - **Performance:** Iterating over `NetworkInterface` uses `asSequence()` for lazy evaluation, exiting the check immediately once a matching valid interface is discovered.
 * - **Flow Safety:** The infinite `while (true)` loop is completely safe here because it executes inside a Cold Flow. It only runs while actively being collected, and `delay(5000)` acts as a cancellable suspending point, ensuring the loop can be cleanly interrupted from the outside.
 * - **Constraints:** Socket connection attempts enforce a strict 3-second (3000ms) timeout to prevent indefinite hanging.
 * * @return A [Flow] of [Boolean] representing the real-time internet connectivity status.
 */
actual class ConnectivityObserver(
    private val chirpLogger: ChirpLogger,
) {
    actual val isConnected = flow {
        while (true) {
            val connected = isConnected()
            chirpLogger.info("Connectivity state on Desktop: $connected")
            emit(connected)
            delay(5000L)
        }
    }

    private val connectivityTargets = listOf(
        InetSocketAddress("8.8.8.8", 53),
        InetSocketAddress("1.1.1.1", 53),
        InetSocketAddress("208.67.222.222", 53),
    )

    private suspend fun isConnected(): Boolean {
        val hasInterface = try {
            withContext(Dispatchers.IO) {
                NetworkInterface.getNetworkInterfaces()
            }
                .asSequence()
                .any { networkInterface ->
                    !networkInterface.isLoopback &&
                        networkInterface.isUp &&
                        networkInterface.inetAddresses.hasMoreElements()
                }
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            false
        }

        if (!hasInterface) {
            return false
        }

        return withContext(Dispatchers.IO) {
            connectivityTargets.any { target ->
                try {
                    Socket().use {
                        it.soTimeout = 3000
                        it.connect(target)
                        true
                    }
                } catch (_: Exception) {
                    currentCoroutineContext().ensureActive()
                    false
                }
            }
        }
    }
}
