package com.project.chirp.navigation

/**
 * A shared singleton responsible for receiving native iOS deep link events and passing them to the Compose Multiplatform UI.
 *
 * ## Strategy / Decisions
 * Because iOS native code (Swift) can receive a deep link before the Compose Multiplatform UI is fully initialized, a caching strategy is required.
 * This prevents cold-start deep links from being dropped into the void.
 *
 * ## How It Works
 * 1. Native iOS calls `onNewUri(uri)` when the app opens via a link.
 * 2. If the Compose UI hasn't attached a `listener` yet, the URI is saved to a `cached` variable.
 * 3. When the Compose UI is ready, it assigns a lambda to the `listener` property.
 * 4. The custom setter for `listener` immediately checks if a `cached` URI exists. If it does, it invokes the newly attached listener with the cached URI and nullifies the cache.
 *
 * ## Alternatives / Why Not
 * Direct synchronous callbacks from Swift to Compose were rejected because the app lifecycle on a cold start guarantees the Swift application delegate will intercept the URL before the Compose `NavHost` is mounted and ready to navigate.
 *
 * ## Technical Details
 * - Threading: Assumes UI thread interactions.
 * - State: Singleton object to maintain cache state across the native/shared boundary.
 *
 * @param uri The absolute string representation of the deep link URL received from the native OS.
 */

object ExternalUriHandler {

    private var cached: String? = null

    var listener: ((uri: String) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                cached?.let {
                    value.invoke(it)
                }
                cached = null
            }
        }

    fun onNewUri(uri: String) {
        cached = uri
        listener?.let {
            it.invoke(uri)
            cached = null
        }
    }
}
