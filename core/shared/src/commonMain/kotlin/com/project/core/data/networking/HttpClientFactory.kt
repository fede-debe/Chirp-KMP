package com.project.core.data.networking

import com.project.core.data.dto.AuthInfoSerializable
import com.project.core.data.dto.requests.RefreshRequest
import com.project.core.data.mappers.toDomain
import com.project.core.domain.auth.SessionStorage
import com.project.core.domain.logging.ChirpLogger
import com.project.core.domain.util.onFailure
import com.project.core.domain.util.onSuccess
import com.project.core.shared.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull
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

/**
 * Configures the HTTP client's Authentication plugin to manage JWT Bearer tokens and handles the background token refresh lifecycle.
 *
 * ## Strategy / Decisions
 * - **JWT Token Architecture:** Employs a dual-token system using short-lived access tokens (15 minutes) for API authorization and long-lived refresh tokens (30 days) to securely maintain sessions.
 * - **Token Rotation:** The refresh endpoint is designed to return both a new access token AND a new refresh token. This security mechanism rotates older refresh tokens out of circulation so they don't remain valid indefinitely.
 * - **Seamless User Experience:** The token refresh flow is handled automatically by the Ktor plugin. As long as a user makes at least one API call within a 30-day window, their session remains alive seamlessly in the background.
 *
 * ## How It Works
 * 1. **Plugin Configuration:** Installs Ktor's `Auth` plugin and configures `bearer` authentication.
 * 2. **Token Loading (`loadTokens`):** Retrieves the locally saved `AuthInfo` from `SessionStorage` (local preferences) and maps it to Ktor's `BearerTokens` object to attach to outgoing requests.
 * 3. **Refresh Trigger (`refreshTokens`):** Invoked automatically by Ktor when an endpoint responds with a `401 Unauthorized` (indicating an expired or invalid access token).
 * 4. **Endpoint Exclusion:** Checks the original request's URL path. If it contains `auth`, the refresh flow is skipped. This prevents an infinite loop where the client needs a token to hit the login/register endpoints.
 * 5. **Token Verification:** Validates the existence of a local refresh token. If it is null or blank, the `SessionStorage` is cleared (logging the user out) and the process aborts.
 * 6. **Refresh Request:** Executes a POST request to `/auth/refresh` using the internal client, passing the current refresh token mapped precisely to the `RefreshRequest` DTO format expected by the backend JSON.
 * 7. **Infinite Loop Prevention:** Crucially marks the request with `markAsRefreshTokenRequest()`. This injects Ktor's internal `AuthCircuitBreaker` attribute, ensuring that if the refresh token itself yields a 401, Ktor stops retrying instead of entering an infinite failure loop.
 * 8. **Session Update:** * - On Success: Overwrites the local `SessionStorage` with the newly rotated token pair and returns them, allowing Ktor to instantly retry the original failed request.
 * - On Failure: If the refresh fails (e.g., 30 days of inactivity), `SessionStorage` is set to null, which clears the session and triggers a redirect to the login screen.
 *
 * ## Alternatives / Why Not
 * - **Client-side Token Mutation:** Rejected. While the client technically can alter the JSON stored in preferences, the server validates the JWT signature and will deny the request.
 * - **Unprotected API endpoints:** Rejected. Endpoints must be protected to ensure users can only access their own private data (like chats).
 *
 * ## Technical Details
 * - **Expiration Constraints:** Access Token (15 mins), Refresh Token (30 days).
 * - **State Management:** Relies heavily on reactive `SessionStorage` for state; setting it to `null` serves as the global trigger for user logout.
 */
class HttpClientFactory(
    private val chirpLogger: ChirpLogger,
    private val sessionStorage: SessionStorage,
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
                // HEADERS (not ALL): logs request/response metadata and headers but omits
                // bodies. This keeps binary uploads (e.g. JPEG attachments) out of the log
                // and avoids leaking the refresh token, which travels in the /auth/refresh
                // request body.
                level = LogLevel.HEADERS
                // Redact credentials so the access-token JWT and the shared API key are
                // never printed verbatim to Logcat (shown as "***" instead).
                sanitizeHeader { header ->
                    header.equals("Authorization", ignoreCase = true) ||
                        header.equals("x-api-key", ignoreCase = true)
                }
            }
            install(WebSockets) {
                pingIntervalMillis = 20_000L
            }
            defaultRequest {
                header("x-api-key", BuildKonfig.API_KEY)
                contentType(ContentType.Application.Json)
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        sessionStorage
                            .observeAuthInfo()
                            .firstOrNull()
                            ?.let {
                                BearerTokens(
                                    accessToken = it.accessToken,
                                    refreshToken = it.refreshToken,
                                )
                            }
                    }
                    refreshTokens {
                        if (response.request.url.encodedPath.contains("auth/")) {
                            return@refreshTokens null
                        }

                        val authInfo = sessionStorage.observeAuthInfo().firstOrNull()
                        if (authInfo?.refreshToken.isNullOrBlank()) {
                            sessionStorage.set(null)
                            return@refreshTokens null
                        }

                        var bearerTokens: BearerTokens? = null
                        client.post<RefreshRequest, AuthInfoSerializable>(
                            route = "/auth/refresh",
                            body = RefreshRequest(
                                refreshToken = authInfo.refreshToken,
                            ),
                            builder = {
                                markAsRefreshTokenRequest()
                            },
                        ).onSuccess { newAuthInfo ->
                            sessionStorage.set(newAuthInfo.toDomain())
                            bearerTokens = BearerTokens(
                                accessToken = newAuthInfo.accessToken,
                                refreshToken = newAuthInfo.refreshToken,
                            )
                        }.onFailure { error ->
                            sessionStorage.set(null)
                        }

                        bearerTokens
                    }
                }
            }
        }
    }
}
