package com.project.chat.data.notification

import com.project.chat.data.dto.request.RegisterDeviceTokenRequest
import com.project.chat.domain.notification.DeviceTokenService
import com.project.core.data.networking.delete
import com.project.core.data.networking.post
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import io.ktor.client.HttpClient

/**
 * Ktor-based implementation of the [DeviceTokenService] for network communication.
 *
 * ## Strategy / Decisions
 * Utilizes the existing Ktor HTTP client to communicate with the backend's notification endpoints,
 * keeping network requests standardized across the common multiplatform code.
 *
 * ## How It Works
 * 1. `registerToken` makes an HTTP POST request to `/notification/register` using a `RegisterDeviceTokenRequest` DTO.
 * 2. `unregisterToken` makes an HTTP DELETE request to `/notification/{token}`, passing the token as a path parameter.
 *
 * ## Technical Details
 * Requires the `RegisterDeviceTokenRequest` payload to encapsulate the token and capitalized platform string.
 */
class KtorDeviceTokenService(
    private val httpClient: HttpClient,
) : DeviceTokenService {

    override suspend fun registerToken(
        token: String,
        platform: String,
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/notification/register",
            body = RegisterDeviceTokenRequest(
                token = token,
                platform = platform,
            ),
        )
    }

    override suspend fun unregisterToken(token: String): EmptyResult<DataError.Remote> {
        return httpClient.delete(
            route = "/notification/$token",
        )
    }
}
