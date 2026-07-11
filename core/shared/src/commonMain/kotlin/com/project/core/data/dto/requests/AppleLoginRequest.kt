package com.project.core.data.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class AppleLoginRequest(
    val identityToken: String,
    val rawNonce: String,
    // Apple only returns the user's name on the very first authorization; omitted on later sign-ins.
    val fullName: String? = null,
)
