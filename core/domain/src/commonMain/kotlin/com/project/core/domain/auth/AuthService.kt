package com.project.core.domain.auth

import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult

/**
 * Defines the core authentication operations required by the application, such as account registration.
 *
 * ## Strategy / Decisions
 * - **Centralized in Core Module:** Placed in `core/domain` instead of feature-specific modules (like `auth` or `chat`). Authentication operations are cross-cutting (e.g., login/register in Auth, logout/change password in Profile/Chat), so a unified service prevents duplicating implementations across features.
 * - **Service vs. Repository:** Named `AuthService` rather than `AuthRepository`. Repositories typically manage data persistence, retrieval, and pagination (e.g., getting a list of messages from a local DB and remote API). This class simply provides actionable network operations (register, login, logout) without holding or persisting state.
 * - **Domain Abstraction:** By placing this interface in the domain layer, the rest of the application remains completely decoupled from specific network libraries (like Ktor).
 *
 * ## How It Works
 * 1. Exposes a `register` function accepting raw user credentials.
 * 2. Returns a custom `Result` wrapper of type `EmptyResult` to safely surface success, or domain-specific `DataError.Remote` errors without throwing exceptions.
 *
 * ## Alternatives / Why Not
 * - **Feature-Specific Services:** We could have built an `AuthFeatureService` for login/registration and a separate `ChatAuthService` for logout/password changes. This was rejected to avoid fragmented authentication logic and stick to a single, cohesive service within the core module.
 *
 * @see KtorAuthService
 */
interface AuthService {
    suspend fun register(
        email: String,
        username: String,
        password: String,
    ): EmptyResult<DataError.Remote>
}
