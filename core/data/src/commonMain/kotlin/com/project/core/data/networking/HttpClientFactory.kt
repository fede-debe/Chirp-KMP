package com.project.core.data.networking

import com.project.core.data.BuildKonfig
import com.project.core.domain.logging.ChirpLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Primary factory class responsible for creating, instantiating, and configuring the Ktor HTTP client.
 * This class ensures that the core HTTP client behavior remains consistent across all platforms in the Kotlin Multiplatform project.
 *
 * ## Strategy / Decisions
 * - **Factory Pattern:** Instantiating the HTTP client requires complex configuration (plugins, JSON parsing, logging, timeouts). Encapsulating this in a factory is preferred over a simple constructor call.
 * - **Platform-Agnostic Core with Platform-Specific Engines:** The `create` function requires an `HttpClientEngine`. This delegates the *only* platform-specific networking detail (e.g., OkHttp for Android, Darwin for iOS) to the caller, keeping this factory completely cross-platform.
 * - **Resilient JSON Parsing:** Kotlinx Serialization is configured with `ignoreUnknownKeys = true`. This prevents parsing failures if the API returns unexpected JSON fields that do not exist in our data classes.
 * - **Centralized Authentication & Headers:** `DefaultRequest` is utilized to automatically append the `X-API-Key` (injected via BuildConfig) and `Content-Type: application/json` headers to *every* request, preventing duplicated setup across individual API calls.
 *
 * ## How It Works
 * 1. Takes the platform-specific `HttpClientEngine` as an argument to construct the base Ktor client.
 * 2. Installs the `ContentNegotiation` plugin, configuring it with Kotlinx Serialization for JSON payload conversion.
 * 3. Installs the `HttpTimeout` plugin, setting a strict 20-second (20,000ms) limit for socket connections and outgoing HTTP requests.
 * 4. Installs the `DefaultRequest` plugin to automatically attach default headers (API Key and Content-Type) to all outbound requests.
 * 5. Installs the `Logging` plugin. It wraps the custom `ChirpLogger` abstraction to intercept Ktor's internal logs and outputs them as debug-level logs. `LogLevel.ALL` is used to capture maximum detail during development.
 * 6. Installs the `WebSockets` plugin and configures a 20-second ping interval to maintain real-time bidirectional connections (ping-pong mechanism) with the server.
 *
 * ## Alternatives / Why Not
 * - **Using standard `println()` for logging:** Rejected because basic print statements lack efficiency, tag application, and timestamps. A dedicated logging library (Touchlab Kermit) accessed via a domain-level abstraction was chosen instead for comprehensive and structured logging.
 *
 * @param engine The platform-specific Ktor HTTP client engine (e.g., OkHttp, Darwin) passed in from the respective platform modules.
 * @return A fully configured Ktor `HttpClient` instance ready for application-wide network requests and WebSockets.
 */
class HttpClientFactory(
    private val chirpLogger: ChirpLogger,
) {

    fun create(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    json = Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 20_000L
                requestTimeoutMillis = 20_000L
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        chirpLogger.debug(message)
                    }
                }
                level = LogLevel.ALL
            }
            install(WebSockets) {
                pingIntervalMillis = 20_000L
            }
            defaultRequest {
                header("x-api-key", BuildKonfig.API_KEY)
                contentType(ContentType.Application.Json)
            }
        }
    }
}
