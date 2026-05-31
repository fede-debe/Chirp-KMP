package com.project.core.data.mappers

import com.project.core.data.dto.AuthInfoSerializable
import com.project.core.data.dto.UserSerializable
import com.project.core.domain.auth.AuthInfo
import com.project.core.domain.auth.User

fun AuthInfoSerializable.toDomain(): AuthInfo {
    return AuthInfo(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = user.toDomain(),
    )
}

fun UserSerializable.toDomain(): User {
    return User(
        id = id,
        email = email,
        username = username,
        hasVerifiedEmail = hasEmailVerified,
        profilePictureUrl = profilePictureUrl,
    )
}

fun User.toSerializable(): UserSerializable {
    return UserSerializable(
        id = id,
        email = email,
        username = username,
        hasEmailVerified = hasVerifiedEmail,
        profilePictureUrl = profilePictureUrl,
    )
}

fun AuthInfo.toSerializable(): AuthInfoSerializable {
    return AuthInfoSerializable(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = user.toSerializable(),
    )
}
