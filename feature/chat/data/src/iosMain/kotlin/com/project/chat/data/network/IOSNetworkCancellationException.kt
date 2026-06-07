package com.project.chat.data.network

/**
 * Custom exception used to unmask native iOS network failures hidden by Coroutine cancellations.
 *
 * ## Strategy / Decisions
 * Created solely to bypass the Kotlin Flow `retry` operator's default behavior of swallowing `CancellationException`.
 * By wrapping the intercepted network-related cancellation into this standard `Exception`, the Coroutine framework
 * treats it as a standard failure, allowing the iOS retry logic to execute properly.
 *
 * ## How It Works
 * 1. Instantiated during `ConnectionErrorHandler.transformException` when an iOS network failure is detected disguised as a coroutine cancellation.
 * 2. Evaluated in `isRetryableError` to explicitly return `true` and trigger a socket reconnect.
 *
 * Technical Details:
 * Inherits from `Exception` rather than `CancellationException` to ensure compatibility with Kotlin Flow error handling blocks.
 *
 * Tags:
 * @param message Human-readable explanation of the intercepted cancellation.
 * @param cause The original `Throwable` (usually a `CancellationException`) that triggered this mapping.
 */
class IOSNetworkCancellationException(
    message: String,
    cause: Throwable?,
) : Exception(message, cause)
