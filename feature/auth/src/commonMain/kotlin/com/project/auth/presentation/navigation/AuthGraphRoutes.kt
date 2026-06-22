package com.project.auth.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Defines the type-safe routes available within the Authentication feature navigation graph.
 *
 * ## Strategy / Decisions
 * Uses a sealed interface coupled with Kotlinx Serialization to define navigation routes. This approach was chosen to enable type-safe navigation and argument passing (e.g., passing an email string natively to `RegisterSuccess`), avoiding the fragility of traditional string-based deep link routes.
 *
 * ## How It Works
 * 1. Declares a base sealed interface `AuthGraphRoutes` to enumerate all possible destinations within the auth flow.
 * 2. Defines the overall graph route as a `data object`.
 * 3. Uses parameterless `data object` classes for static screens (Login, Register, Forgot Password).
 * 4. Uses a `data class` for the `RegisterSuccess` screen, strictly typing the required `email` argument.
 *
 * ## Alternatives / Why Not
 * - **Navigation Three:** This is an upcoming, highly promising navigation library. However, it was rejected for this project because it is currently in an early alpha stage and not yet available/stable for Compose Multiplatform. Standard Compose Navigation is the most viable and stable approach at this time.
 *
 * Technical Details:
 * Requires the `@Serializable` annotation on all objects/classes. Under the hood, Kotlinx Serialization generates the adapters necessary to serialize these types into navigation arguments.
 */
sealed interface AuthGraphRoutes {
    @Serializable
    data object Graph : AuthGraphRoutes

    @Serializable
    data object Login : AuthGraphRoutes

    @Serializable
    data object Register : AuthGraphRoutes

    @Serializable
    data class RegisterSuccess(val email: String) : AuthGraphRoutes

    @Serializable
    data class VerificationSent(val email: String) : AuthGraphRoutes

    @Serializable
    data object ForgotPassword : AuthGraphRoutes

    @Serializable
    data class ResetPassword(val token: String) : AuthGraphRoutes

    @Serializable
    data class EmailVerification(val token: String) : AuthGraphRoutes
}
