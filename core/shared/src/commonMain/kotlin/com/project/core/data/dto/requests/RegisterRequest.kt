package com.project.core.data.dto.requests

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object (DTO) representing the JSON body required for the registration endpoint.
 *
 * ## Strategy / Decisions
 * - **DTO Separation:** Placed in a specific `dto/request` package inside the data layer. This ensures that the network JSON structure is isolated from domain models, allowing the API contract to change without breaking the app's internal domain logic.
 *
 * ## Technical Details
 * - Requires `@Serializable` from `kotlinx.serialization` to automatically parse this Kotlin Data Class into the JSON format expected by the Ktor HTTP client.
 * - Parameter names (e.g., `username`) must strictly match the expected JSON keys on the backend API.
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
)
