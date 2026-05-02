package com.project.core.data.networking

import com.project.core.domain.util.DataError
import com.project.core.domain.util.Result
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
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
expect suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError.Remote>,
): Result<T, DataError.Remote>

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
