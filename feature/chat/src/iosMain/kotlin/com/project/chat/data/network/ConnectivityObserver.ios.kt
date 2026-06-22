package com.project.chat.data.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfiable
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

/**
 * Observes the network connectivity state on iOS devices using native C-Interop APIs and emits updates.
 *
 * ## Strategy / Decisions
 * The iOS implementation relies on C-Interop to access Apple's low-level `NWPathMonitor` API. Like the
 * Android implementation, it wraps this callback-driven system inside a `callbackFlow` to provide a unified
 * reactive stream to the shared Kotlin codebase.
 *
 * ## How It Works
 * 1. Creates a network path monitor using `nw_path_monitor_create()`.
 * 2. Instantiates a dedicated dispatch queue (`dispatch_queue_create`) labeled for debugging (e.g., `NW_PATH_MONITOR_LABEL`) to handle network event dispatches.
 * 3. Registers an update handler via `nw_path_monitor_set_update_handler`.
 * 4. Whenever a network path update occurs, extracts the status using `nw_path_get_status`.
 * 5. Evaluates the enum-like C-status: emits `true` if the status is `nw_path_status_satisfied` or `nw_path_status_satisfiable`. Otherwise, emits `false`.
 * 6. Assigns the queue to the monitor (`nw_path_monitor_set_queue`) and starts monitoring (`nw_path_monitor_start`).
 * 7. Cleans up by calling `nw_path_monitor_cancel` inside the flow's `awaitClose` block.
 *
 * ## Alternatives / Why Not
 * Higher-level Swift libraries or standard generated UI Kit bindings could not be used here because iOS
 * network monitoring relies natively on C-level functions that require custom definition (`.def`) files to bridge.
 *
 * ## Technical Details
 * - **Naming Conventions:** The Kotlin API functions mirror the exact snake_case naming of the C functions (e.g., `nw_path_monitor_create`).
 * - **Status `satisfiable`:** Even though it is poorly documented in Apple's SDK, technical testing shows that `nw_path_status_satisfiable` means the network is technically available and should be treated as connected.
 * - **Device Testing:** Simulators may exhibit unreliable network toggles; debugging true states (like Airplane mode vs. Wi-Fi disabling) requires a physical device.
 */
actual class ConnectivityObserver {
    actual val isConnected: Flow<Boolean> = callbackFlow {
        val pathMonitor = nw_path_monitor_create()

        val queue = dispatch_queue_create(
            NW_PATH_MONITOR_LABEL,
            null,
        )

        nw_path_monitor_set_update_handler(pathMonitor) { path ->
            if (path != null) {
                val status = nw_path_get_status(path)

                val isConnected = when (status) {
                    nw_path_status_satisfied -> true
                    nw_path_status_satisfiable -> true
                    else -> false
                }

                trySend(isConnected)
            }
        }

        nw_path_monitor_set_queue(pathMonitor, queue)
        nw_path_monitor_start(pathMonitor)

        awaitClose {
            nw_path_monitor_cancel(pathMonitor)
        }
    }

    companion object {
        private const val NW_PATH_MONITOR_LABEL = "com.project.chat.data.network.ConnectivityObserver"
    }
}
