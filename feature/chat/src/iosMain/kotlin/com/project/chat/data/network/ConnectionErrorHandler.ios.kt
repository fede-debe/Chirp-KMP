package com.project.chat.data.network

import com.project.chat.domain.models.ConnectionState
import kotlinx.coroutines.CancellationException
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

/**
 * iOS implementation of the connection error handler, specifically engineered to intercept and map Darwin and Coroutine cancellation errors.
 *
 * ## Strategy / Decisions
 * A critical, hacky workaround is required here due to how Kotlin/Native interacts with Coroutines and iOS network calls.
 * When a native iOS network exception is thrown inside a Kotlin Flow `retry` operator, it surfaces to Kotlin as a `CancellationException`.
 * Because the Coroutine framework intentionally ignores cancellation exceptions, the Flow's retry mechanism completely fails to trigger.
 * To solve this, we intercept these cancellations, parse their messages for Darwin error footprints, and transform them into custom exceptions.
 *
 * ## How It Works
 * 1. `transformException` intercepts incoming throwables. If it's a Kotlin `CancellationException`, it inspects the attached message.
 * 2. It checks the message for string patterns indicating a Darwin HTTP exception or specific `NSURL` error codes (e.g., `-1005` or `-1009`).
 * 3. If a pattern matches, it transforms the cancellation into a custom `IOSNetworkCancellationException` so the Flow `retry` operator won't swallow it.
 * 4. `getConnectionStateForError` extracts the native `NSError`. If the throwable isn't already an `NSError`, it reconstructs one by parsing the error message using companion object string patterns.
 * 5. Finally, maps the extracted error codes (timed out, connection lost) to respective `ConnectionState` variants.
 *
 * ## Alternatives / Why Not
 * Direct exception casting was attempted, but failed because native `NSError`s originating from suspending functions get masked.
 * We cannot rely on standard `is NSError` type checks when the framework wraps them in `CancellationException`. String parsing is brittle but currently the only viable fallback.
 *
 * Technical Details:
 * - Hardcodes `NSURL` error domain code checks: `-1005` (Connection Lost) and `-1009` (Not Connected).
 * - Relies heavily on string matching (`contains`) against exception messages to reconstruct `NSError` instances.
 */
actual class ConnectionErrorHandler {
    actual fun getConnectionStateForError(cause: Throwable): ConnectionState {
        val nsError = extractNsError(cause)

        return if (nsError != null) {
            when (nsError.code) {
                NSURLErrorNotConnectedToInternet,
                NSURLErrorNetworkConnectionLost,
                NSURLErrorTimedOut,
                -> ConnectionState.ERROR_NETWORK
                else -> ConnectionState.ERROR_UNKNOWN
            }
        } else if (cause is IOSNetworkCancellationException) {
            ConnectionState.ERROR_NETWORK
        } else {
            ConnectionState.ERROR_UNKNOWN
        }
    }

    actual fun transformException(exception: Throwable): Throwable {
        if (exception is CancellationException) {
            val cause = exception.cause ?: return exception
            val isDarwinException = cause.message?.contains("DarwinHttpRequestException") == true
            val isConnectionLostException = cause.message?.contains("NSURLErrorDomain Code=-1005") == true
            val isNotConnectedException = cause.message?.contains("NSURLErrorDomain Code=-1009") == true

            if (isDarwinException || isConnectionLostException || isNotConnectedException) {
                return IOSNetworkCancellationException(
                    message = "Network connection lost (extracted from cancellation)",
                    cause = cause,
                )
            }
        }

        return exception
    }

    actual fun isRetriableError(cause: Throwable): Boolean {
        if (cause is IOSNetworkCancellationException) {
            return true
        }

        return when (extractNsError(cause)?.code) {
            NSURLErrorNotConnectedToInternet,
            NSURLErrorNetworkConnectionLost,
            NSURLErrorTimedOut,
            -> true
            else -> false
        }
    }

//    private fun extractNsError(cause: Throwable): NSError? {
//        val throwableCause = cause.cause
//        if (throwableCause is NSError) {
//            return throwableCause
//        }
//
//        if (cause is NSError) {
//            return cause
//        }
//
//        val exceptionNsError = cause.toNSError()
//        val causeNsError = cause.cause?.toNSError()
//
//        return exceptionNsError ?: causeNsError
//    }

    private fun extractNsError(cause: Throwable): NSError? {
        val exceptionNsError = cause.toNSError()
        val causeNsError = cause.cause?.toNSError()
        return exceptionNsError ?: causeNsError
    }

    private fun Throwable.toNSError(): NSError? {
        return message?.let { message ->
            when {
                message.contains(NSURLErrorNotConnectedToInternetPattern) ->
                    return NSError.errorWithDomain(
                        domain = NSURLErrorDomain,
                        code = NSURLErrorNotConnectedToInternet,
                        userInfo = null,
                    )
                message.contains(NSURLErrorNetworkConnectionLostPattern) ->
                    return NSError.errorWithDomain(
                        domain = NSURLErrorDomain,
                        code = NSURLErrorNetworkConnectionLost,
                        userInfo = null,
                    )
                else -> null
            }
        }
    }

    companion object {
        private val NSURLErrorNotConnectedToInternetPattern =
            "Error Domain=$NSURLErrorDomain Code=$NSURLErrorNotConnectedToInternet"
        val NSURLErrorNetworkConnectionLostPattern =
            "Error Domain=$NSURLErrorDomain Code=$NSURLErrorNetworkConnectionLost"
    }
}
