package com.project.chat.data.lifecycle

import kotlinx.coroutines.flow.Flow

/**
 * Exposes the current foreground/background state of the application as a reactive flow.
 *
 * ## Strategy / Decisions
 * WebSockets provide the bidirectional connection needed for real-time messaging, but unlike standard HTTP
 * fire-and-forget requests, they require actively maintaining a persistent connection. Keeping this connection
 * alive while the app is in the background can lead to OS-level connection exceptions and resource drain.
 * Therefore, we proactively monitor the app's lifecycle to manually disconnect when minimized and automatically
 * reconnect when brought back to the foreground.
 *
 * ## Alternatives / Why Not
 * - **HTTP Polling:** Rejected because consistently asking the server for updates creates massive network
 * overhead (due to HTTP headers) and severe latency, making it unsuitable for real-time chat.
 * - **Passive Disconnection:** Rejected. Waiting for the OS (especially iOS) to throw a connection exception
 * when backgrounded creates unpredictable states. Proactive, manual management is safer.
 *
 * ## How It Works
 * 1. Declares an `expect` class requiring platform-specific implementations.
 * 2. Exposes an `isInForeground` Flow that emits `true` when the app is active and `false` when it is not.
 * 3. The emitted state acts as the single source of truth to conditionally toggle the WebSocket connection.
 */
expect class AppLifecycleObserver {
    val isInForeground: Flow<Boolean>
}
