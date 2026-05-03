package com.project.core.data.networking

import com.project.core.domain.util.DataError
import com.project.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse

/**
 * Provides Ktor HTTP client extensions to safely execute network calls, handle low-level exceptions,
 * and automatically parse responses into a domain-specific `Result` wrapper.
 *
 * ## Strategy / Decisions
 * - **Centralized Result Parsing:** Designed to eliminate boilerplate by automatically interpreting HTTP status codes
 *   and mapping them to a clear `DataError.Remote` enum.
 * - **Ktor as the Foundation:** Chosen because it is currently the only viable, production-ready networking library
 *   for Kotlin Multiplatform (KMP) projects.
 * - **Platform-Specific Exception Handling (`expect`/`actual`):** Network errors are inherently platform-dependent.
 *   Android uses OkHttp (throwing Java exceptions like `UnknownHostException`), while iOS uses Darwin (throwing
 *   `DarwinHttpRequestException`). The `expect`/`actual` mechanism bridges this gap to provide unified error states.
 * - **Reified Generics:** Functions parsing JSON bodies use `inline` and `reified T` to preserve generic type
 *   information at runtime, allowing Ktor's `body()` function to know exactly which data class to deserialize into.
 *
 * ## How It Works
 * 1. **URL Construction (`constructRoute`):** Determines if an incoming route string is a full URL or a relative path.
 *    If relative (starts with `/`), it prepends the `BASE_URL` to ensure a valid endpoint address.
 * 2. **Execution Wrapper (`safeCall`):** Takes a suspending execution block and delegates it to `platformSafeCall`.
 *    It passes along a response handler (`responseToResult`).
 * 3. **Platform Delegation (`platformSafeCall`):** Executes the network call. The `actual` implementation on Android
 *    catches Java/OkHttp exceptions, mapping them to domain errors (e.g., `ConnectException` -> `NoInternet`).
 *    The iOS `actual` catches Darwin exceptions and inspects the `NSError` domain and code (e.g.,
 *    `NSURLErrorNotConnectedToInternet`) to achieve the same result.
 * 4. **Response Parsing (`responseToResult`):** Evaluates the raw `HttpResponse`. Status codes in the 2xx range
 *    attempt JSON deserialization. Any serialization failures are caught and mapped to `Serialization`.
 *    Non-2xx codes (401, 408, 409, etc.) are explicitly mapped to their corresponding domain error states
 *    (Unauthorized, RequestTimeout, Conflict).
 *
 * ## Alternatives / Why Not
 * - **Why not a standard `try/catch` in shared code?** Rejected. KMP shared code cannot access or catch
 *   Java-specific (java.net.*) or Apple-specific networking exceptions. A common `try/catch` would crash or fail to
 *   identify critical states like "No Internet."
 *
 * ## Technical Details
 * - **Coroutine Cancellation Safety:** In the Android implementation, catching a generic `Exception` requires a strict
 *   call to `coroutineContext.ensureActive()`. Swallowing a `CancellationException` inadvertently prevents the parent
 *   coroutine scope from closing, leading to memory leaks, zombie processes, and unpredictable app states.
 * - **Dependencies:** Requires Ktor core in `commonMain`, `ktor-client-okhttp` in `androidMain`, and
 *   `ktor-client-darwin` in `iosMain`.
 *
 * @param route The HTTP route string (can be a full URL or relative path).
 * @param execute A suspending lambda that performs the Ktor HTTP request.
 * @return A [Result] encapsulating either the successfully parsed data class of type `T`, or a `DataError.Remote`.
 */

/**
 * Provides utility extension functions (`post`, `get`, `delete`, `put`) on [HttpClient] to streamline network requests.
 * These functions wrap standard Ktor HTTP calls inside a custom `safeCall` to standardize error handling and URL construction.
 *
 * ## Strategy / Decisions
 * - **Centralized Error Handling:** By wrapping all network calls in `safeCall`, we automatically parse HTTP responses into a custom `Result` class. This allows the presentation layer to centrally map `DataError.Remote` cases into clear, user-facing error messages, completely eliminating repetitive try-catch blocks and error handling logic for every request.
 * - **Relative URL Support:** Ktor normally requires explicit URL configuration. This abstraction accepts a relative `route` string and processes it through `constructRoute()` to build the fully qualified Ktor URL, making standard usage much cleaner.
 * - **Type-Safe Payloads:** Uses inline functions with `reified` type parameters to automatically serialize the generic `<Request>` body and deserialize the generic `<Response>` payload.
 * - **Flexible Extensibility:** Exposes a trailing `HttpRequestBuilder` lambda to accommodate "special snowflake" requests that require unique properties (like a single-use authorization header), bypassing the need to create entirely new utility functions for edge cases.
 *
 * ## How It Works
 * 1. The relative `route` is formatted into a full URL string via `constructRoute()`.
 * 2. The function iterates through the `queryParams` map, calling Ktor's `parameter()` to attach each key-value pair to the request.
 * 3. The generic `body` is set using Ktor's `setBody()` (applicable for POST and PUT requests).
 * 4. The optional `builder` block is executed, applying any custom configurations specific to the call instance.
 * 5. The entire Ktor HTTP call (e.g., `client.post { ... }`) is passed directly into `safeCall()`, which executes the request and returns a `Result<Response, DataError.Remote>`.
 *
 * ## Alternatives / Why Not
 * - **Directly using Ktor's built-in `client.post {}`:** Directly calling Ktor was rejected because it requires configuring the full URL, setting the body, and manually handling exceptions or status codes on every single call. This utility abstracts that verbosity away.
 *
 * Technical Details
 * - **`crossinline` Modifier:** The `builder` lambda must be marked as `crossinline` because it is invoked inside the nested Ktor HTTP block context, preventing non-local returns from the caller.
 * - **Method Constraints:** `GET` and `DELETE` requests intentionally omit the generic `<Request>` parameter and the `body` parameter, enforcing standard REST principles where these operations do not contain body payloads.
 *
 * @param route The relative endpoint path string (e.g., "/users").
 * @param queryParams An optional map of key-value pairs to append to the endpoint URL. Defaults to an empty map.
 * @param body The strongly-typed data payload (applicable only to POST and PUT methods).
 * @param builder An optional lambda extending `HttpRequestBuilder` for custom headers or request configurations.
 * @return A `Result` encapsulating either the successful `<Response>` object or a `DataError.Remote` failure.
 */
expect suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError.Remote>,
): Result<T, DataError.Remote>

suspend inline fun <reified Request, reified Response : Any> HttpClient.post(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    body: Request,
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall {
        post {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        }
    }
}

suspend inline fun <reified Response : Any> HttpClient.get(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall {
        get {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            builder()
        }
    }
}

suspend inline fun <reified Response : Any> HttpClient.delete(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall {
        delete {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            builder()
        }
    }
}

suspend inline fun <reified Request, reified Response : Any> HttpClient.put(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    body: Request,
    crossinline builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Remote> {
    return safeCall {
        put {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        }
    }
}

suspend inline fun <reified T> safeCall(
    noinline execute: suspend () -> HttpResponse,
): Result<T, DataError.Remote> {
    return platformSafeCall(
        execute = execute,
    ) { response ->
        responseToResult(response)
    }
}

suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError.Remote> {
    return when (response.status.value) {
        in 200..299 -> {
            try {
                Result.Success(response.body<T>())
            } catch (e: NoTransformationFoundException) {
                Result.Failure(DataError.Remote.SERIALIZATION)
            }
        }
        400 -> Result.Failure(DataError.Remote.BAD_REQUEST)
        401 -> Result.Failure(DataError.Remote.UNAUTHORIZED)
        403 -> Result.Failure(DataError.Remote.FORBIDDEN)
        404 -> Result.Failure(DataError.Remote.NOT_FOUND)
        408 -> Result.Failure(DataError.Remote.REQUEST_TIMEOUT)
        413 -> Result.Failure(DataError.Remote.PAYLOAD_TOO_LARGE)
        429 -> Result.Failure(DataError.Remote.TOO_MANY_REQUESTS)
        500 -> Result.Failure(DataError.Remote.SERVER_ERROR)
        503 -> Result.Failure(DataError.Remote.SERVICE_UNAVAILABLE)
        else -> Result.Failure(DataError.Remote.UNKNOWN)
    }
}

fun constructRoute(route: String): String {
    return when {
        route.contains(UrlConstants.BASE_URL_HTTP) -> route
        route.startsWith("/") -> "${UrlConstants.BASE_URL_HTTP}$route"
        else -> "${UrlConstants.BASE_URL_HTTP}/$route"
    }
}
