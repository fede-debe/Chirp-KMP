package com.project.chat.presentation.util

import com.project.chat.domain.models.ConnectionState
import com.project.chat.presentation.Res
import com.project.chat.presentation.network_error
import com.project.chat.presentation.offline
import com.project.chat.presentation.online
import com.project.chat.presentation.reconnecting
import com.project.chat.presentation.unknown_error
import com.project.core.presentation.util.UiText

fun ConnectionState.toUiText(): UiText {
    val resource = when (this) {
        ConnectionState.DISCONNECTED -> Res.string.offline
        ConnectionState.CONNECTING -> Res.string.reconnecting
        ConnectionState.CONNECTED -> Res.string.online
        ConnectionState.ERROR_NETWORK -> Res.string.network_error
        ConnectionState.ERROR_UNKNOWN -> Res.string.unknown_error
    }
    return UiText.Resource(resource)
}
