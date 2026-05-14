package com.project.core.data.auth

import com.project.core.data.dto.requests.RegisterRequest
import com.project.core.data.networking.post
import com.project.core.domain.auth.AuthService
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import io.ktor.client.HttpClient

/**
 * Ktor-based implementation of the [AuthService] for executing authentication network requests.
 *
 * ## Strategy / Decisions
 * - **Explicit Naming (`KtorAuthService`):** Specifically named to highlight the underlying technology and strategy. Using a generic `AuthServiceImpl` was rejected because it doesn't describe *what* makes the implementation specific. If we ever swap to another HTTP client, the naming cleanly distinguishes the implementations.
 * - **Domain-Pure Error Handling:** Uses internal utility functions to intercept Ktor-specific exceptions and HTTP error codes, mapping them directly to clean, domain-level `DataError.Remote` objects so the domain layer never knows about Ktor.
 *
 * ## How It Works
 * 1. Takes a Ktor `HttpClient` as a constructor dependency.
 * 2. Overrides the `register` function.
 * 3. Wraps the provided credentials into a `RegisterRequest` Data Transfer Object (DTO).
 * 4. Uses a custom `.post` utility function to send a request to the relative route `/auth/register`.
 * 5. Attaches the serialized `RegisterRequest` as the mandatory HTTP body.
 *
 * ## Alternatives / Why Not
 * - **Generic `Impl` Naming:** We strictly avoided naming this `AuthServiceImpl`. "Impl" adds zero contextual value. Naming should always reveal the specific library or architectural pattern (e.g., `OfflineFirstChatRepository`) driving the class.
 *
 * ## Technical Details
 * - **Serialization:** Relies on KotlinX Serialization (`@Serializable` on `RegisterRequest`) to parse the DTO into a JSON payload.
 * - **Routing:** Makes calls to a relative API endpoint (`/auth/register`) rather than a hardcoded absolute URL.
 * - **Safety:** Under the hood, it uses a custom `safeCall` wrapper that executes the request, prevents crashes from network exceptions, and standardizes the error reporting stream.
 *
 * @param httpClient The Ktor client used to execute the HTTP requests.
 * @return A Result containing either an EmptyResult on success or a mapped DataError.Remote.
 */
class KtorAuthService(
    private val httpClient: HttpClient,
) : AuthService {

    override suspend fun register(
        email: String,
        username: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/auth/register",
            body = RegisterRequest(
                email = email,
                username = username,
                password = password,
            ),
        )
    }
}
