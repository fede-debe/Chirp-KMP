package com.project.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserSerializable(
    val id: String,
    val email: String,
    val username: String,
    val hasEmailVerified: Boolean = false,
    val profilePictureUrl: String? = null,
)
