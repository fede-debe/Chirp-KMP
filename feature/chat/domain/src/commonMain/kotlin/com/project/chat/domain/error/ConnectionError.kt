package com.project.chat.domain.error

import com.project.core.domain.util.Error

enum class ConnectionError : Error {
    NOT_CONNECTED,
    MESSAGE_SEND_FAILED,
}
