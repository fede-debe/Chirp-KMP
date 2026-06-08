package com.project.chat.data.network

import com.project.chat.domain.models.ConnectionState
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.websocket.WebSocketException
import io.ktor.network.sockets.SocketTimeoutException
import kotlinx.io.EOFException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Android implementation of the connection error handler utilizing standard Java and OkHttp exception types.
 *
 * ## Strategy / Decisions
 * Android's networking stack is straightforward. Since OkHttp and `java.net` throw distinct,
 * strongly-typed exceptions (`SocketTimeoutException`, `UnknownHostException`), we can rely on standard
 * type-checking to classify errors. We explicitly block retries for certain exceptions (like `UnknownHostException`,
 * `SSLException`, or `BadClientRequest`) because retrying immediately without an underlying network change
 * or code fix will definitively fail again.
 *
 * ## How It Works
 * 1. `getConnectionStateForError` checks the instance type of the throwable. If it matches known network
 * exceptions (EOF, WebSocket Exception, Socket Timeout), it maps to `ConnectionState.NetworkError`.
 * 2. `transformException` acts as a simple pass-through. Coroutine cancellations behave as expected on the JVM,
 * so no exception interception is required.
 * 3. `isRetryableError` evaluates specific types of network failures using a `when` expression. Timeouts
 * and end-of-file streams are approved for retries, while unresolved hosts are rejected.
 *
 * Technical Details:
 * Relies directly on `java.net` and Ktor/OkHttp exception types. Immediate retries are blocked for hard
 * offline states; a separate connection observer handles internet reconnection events.
 */
actual class ConnectionErrorHandler {
    actual fun getConnectionStateForError(cause: Throwable): ConnectionState {
        return when (cause) {
            is ClientRequestException,
            is WebSocketException,
            is SocketException,
            is SocketTimeoutException,
            is UnknownHostException,
            is SSLException,
            is EOFException,
            -> ConnectionState.ERROR_NETWORK
            else -> ConnectionState.ERROR_UNKNOWN
        }
    }

    actual fun transformException(exception: Throwable): Throwable {
        return exception
    }

    actual fun isRetriableError(cause: Throwable): Boolean {
        return when (cause) {
            is SocketTimeoutException,
            is WebSocketException,
            is SocketException,
            is EOFException,
            -> true
            else -> false
        }
    }
}
