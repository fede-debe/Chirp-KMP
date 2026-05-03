package com.project.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Safely observes one-time events (like navigation or snackbar messages) from a Kotlin Flow
 * within a Jetpack Compose UI, ensuring they are consumed exactly once.
 *
 * ## Strategy / Decisions
 * In Compose, UI updates are usually driven by persistent state (e.g., StateFlow). However,
 * using persistent state for one-time actions is problematic. For example, if navigation is
 * triggered by `isLoggedIn == true`, that state remains `true`. When a user navigates back to
 * that screen, or if the device is rotated (causing an Android configuration change), the state
 * observer would immediately re-trigger the navigation. To avoid this, we use Kotlin Channels
 * to emit one-time events and observe them in a lifecycle-aware manner so they trigger only once.
 *
 * ## How It Works
 * 1. Retrieves the current `LocalLifecycleOwner`.
 * 2. Launches a `LaunchedEffect` that restarts only if the provided keys change.
 * 3. Uses `repeatOnLifecycle(Lifecycle.State.STARTED)` to ensure the flow is only collected
 *    when the UI is actually visible and active, automatically cancelling collection when the
 *    lifecycle drops below the `STARTED` state (e.g., `ON_STOP`).
 * 4. Switches the coroutine context to `Dispatchers.Main.immediate` to collect the flow.
 * 5. Passes the collected events to the provided `onEvent` lambda.
 *
 * ## Alternatives / Why Not
 * - **`collectAsStateWithLifecycle()`**: While this handles lifecycle properly, it is designed
 *   for *state*, meaning it caches the latest emitted value. If the activity is recreated
 *   (e.g., screen rotation), the cached event would be re-delivered to the UI, causing the
 *   one-time action (like showing an error snackbar) to happen again.
 *
 * ## Technical Details
 * - **Thread Safety / Preventing Missed Events**: The context switch to `Dispatchers.Main.immediate`
 *   is critical on Android. `repeatOnLifecycle` cancels the coroutine when the activity goes into
 *   the `STOPPED` state. There is a very brief window where the activity transitions from `STOPPED`
 *   to `DESTROYED`. If the ViewModel sends an event through the Channel during this exact gap, it
 *   could be lost forever. `Main.immediate` ensures the collection happens synchronously on the main
 *   thread, eliminating this race condition.
 * - **Configuration Changes**: This function is particularly crucial for Android targets in
 *   multi-platform development, as it inherently protects against the side effects of Android's
 *   Activity recreation cycles.
 * - **Keys**: The optional keys ensure that if any external variables used inside the `onEvent`
 *   lambda change, the `LaunchedEffect` is properly restarted to capture the new references.
 *
 * @param flow The [Flow] of type [T] containing the one-time events sent from the ViewModel.
 * @param key1 Optional parameter to trigger the re-composition of the effect if a dependency changes.
 * @param key2 Optional second parameter for effect recreation.
 * @param onEvent The action to perform when an event of type [T] is received.
 */
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, key1, key2) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect(onEvent)
            }
        }
    }
}
