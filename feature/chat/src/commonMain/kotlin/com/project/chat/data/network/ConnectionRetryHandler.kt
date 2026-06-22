package com.project.chat.data.network

import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * Determines whether a connection retry should be initiated and applies an exponential backoff delay between attempts.
 * * ## Strategy / Decisions
 * - **Exponential Backoff:** Implements an exponentially increasing delay between failed attempts rather than a constant retry strategy. This prevents spamming the server and handles persistent network issues more gracefully by giving the system more time to recover after repeated failures.
 * - **Concrete Class over Abstraction:** Kept as a standard concrete class in the data layer rather than putting it behind an interface in the domain layer. The instructor strongly warns against "flooding your codebase with abstractions just for the sake of flooding it." Since the presentation layer doesn't need to trigger retries and no alternative implementations are planned, a direct class avoids unnecessary architectural boilerplate.
 * - **Max Delay Ceiling:** Caps the maximum backoff delay at 30 seconds (`30,000` ms). Exponential growth scales too aggressively, and without a ceiling, the application might eventually wait hours before attempting to reconnect.
 * * ## How It Works
 * 1. The `shouldRetry` function forwards the incoming error to the `ConnectionErrorHandler` to verify if the exception is actually retryable.
 * 2. When a retry is approved, `applyRetryDelay` is invoked. It calculates the required wait time using an exponential formula: $2^{\text{attempt}} \times 2000$ milliseconds.
 * 3. The `createBackoffDelay` function compares this calculated time against the `maxDelay` ceiling and returns the smaller of the two values.
 * 4. The system suspends the coroutine for that specific duration using Kotlin Coroutines' `delay()`.
 * 5. A manual `resetDelay()` function allows external callers (e.g., when a socket is entirely recreated) to toggle the `shouldSkipBackoff` flag. If true, the next retry attempt instantly proceeds without a delay, and the flag resets to false for subsequent attempts.
 * * ## Alternatives / Why Not
 * - **Constant Retries (e.g., fixed 10s wait):** Rejected because repeated failures often indicate a deeper issue. Waiting the exact same amount of time over and over is less efficient than giving the system progressively more breathing room.
 * - **Domain Layer Interface:** Rejected. While a domain abstraction would allow passing a mock in isolation tests, abstractions should strictly be reserved for genuine inversion of control or when multiple implementations are definitively required.
 * * ## Technical Details
 * - Resides entirely in the `commonMain` module without requiring `expect` / `actual` declarations.
 * - The mathematical power calculation uses `pow()`, which requires casting the `attempt` parameter from a `Long` to an `Int` to compile properly.
 * * @param connectionErrorHandler Dependency used to evaluate if a specific `Throwable` warrants a retry.
 */
class ConnectionRetryHandler(
    private val connectionErrorHandler: ConnectionErrorHandler,
) {
    private var shouldSkipBackoff = false

    fun shouldRetry(cause: Throwable, attempt: Long): Boolean {
        return connectionErrorHandler.isRetriableError(cause)
    }

    suspend fun applyRetryDelay(attempt: Long) {
        if (!shouldSkipBackoff) {
            val delay = createBackoffDelay(attempt)
            delay(delay)
        } else {
            shouldSkipBackoff = false
        }
    }

    fun resetDelay() {
        shouldSkipBackoff = true
    }

    private fun createBackoffDelay(attempt: Long): Long {
        val delayTime = (2f.pow(attempt.toInt()) * 2000L).toLong()
        val maxDelay = 30_000L
        return minOf(delayTime, maxDelay)
    }
}
