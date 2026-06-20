package com.project.chat.data.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Android-specific implementation of the application lifecycle observer.
 *
 * ## Strategy / Decisions
 * Uses `ProcessLifecycleOwner` rather than an Activity-level lifecycle observer. This ensures that the state
 * reflects the entire application process, preventing false "background" emissions during configuration
 * changes (like screen rotations).
 *
 * ## How It Works
 * 1. Retrieves the initial app state upon collection by checking if the process state is at least `STARTED`.
 * 2. Bridges traditional Android callback APIs into a reactive stream using `callbackFlow`.
 * 3. Registers a `LifecycleEventObserver` to listen for global `ON_START` and `ON_STOP` events.
 * 4. Uses `trySend` to emit `true` (ON_START) or `false` (ON_STOP), because the observer callback is not a
 * suspending context.
 * 5. Uses `awaitClose` to automatically unregister the observer and prevent memory leaks when the flow collector
 * (e.g., ViewModel CoroutineScope) is cancelled.
 *
 * Technical Details
 * - **Thread Safety:** The flow relies on UI-related lifecycle components, so it is constrained to
 * `Dispatchers.Main` using `.flowOn(Dispatchers.Main)`.
 * - **Dependencies:** Requires the `androidx.lifecycle:lifecycle-process` dependency to access
 * `ProcessLifecycleOwner`.
 */
actual class AppLifecycleObserver {
    actual val isInForeground: Flow<Boolean> = callbackFlow {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle

        val isAtLeastStarted = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        send(isAtLeastStarted)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> trySend(true)
                Lifecycle.Event.ON_STOP -> trySend(false)
                else -> Unit
            }
        }

        lifecycle.addObserver(observer)

        awaitClose {
            lifecycle.removeObserver(observer)
        }
    }.flowOn(Dispatchers.Main)
}
