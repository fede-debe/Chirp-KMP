package com.project.chat.data.mappers

import com.project.chat.data.dto.response.ProfilePictureUploadUrlsResponse
import com.project.chat.domain.models.ProfilePictureUploadUrls

fun ProfilePictureUploadUrlsResponse.toDomain(): ProfilePictureUploadUrls {
    return ProfilePictureUploadUrls(
        uploadUrl = uploadUrl,
        publicUrl = publicUrl,
        headers = headers,
    )
}
