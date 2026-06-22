package com.project.core.presentation.util

import com.project.core.domain.util.DataError
import com.project.core.ui.Res
import com.project.core.ui.error_bad_request
import com.project.core.ui.error_conflict
import com.project.core.ui.error_disk_full
import com.project.core.ui.error_forbidden
import com.project.core.ui.error_no_internet
import com.project.core.ui.error_not_found
import com.project.core.ui.error_payload_too_large
import com.project.core.ui.error_request_timeout
import com.project.core.ui.error_serialization
import com.project.core.ui.error_server
import com.project.core.ui.error_service_unavailable
import com.project.core.ui.error_too_many_requests
import com.project.core.ui.error_unable_to_send_message
import com.project.core.ui.error_unauthorized
import com.project.core.ui.error_unknown

fun DataError.toUiText(): UiText {
    val resource = when (this) {
        DataError.Local.DISK_FULL -> Res.string.error_disk_full
        DataError.Local.NOT_FOUND -> Res.string.error_not_found
        DataError.Local.UNKNOWN -> Res.string.error_unknown
        DataError.Remote.BAD_REQUEST -> Res.string.error_bad_request
        DataError.Remote.REQUEST_TIMEOUT -> Res.string.error_request_timeout
        DataError.Remote.UNAUTHORIZED -> Res.string.error_unauthorized
        DataError.Remote.FORBIDDEN -> Res.string.error_forbidden
        DataError.Remote.NOT_FOUND -> Res.string.error_not_found
        DataError.Remote.CONFLICT -> Res.string.error_conflict
        DataError.Remote.TOO_MANY_REQUESTS -> Res.string.error_too_many_requests
        DataError.Remote.NO_INTERNET -> Res.string.error_no_internet
        DataError.Remote.PAYLOAD_TOO_LARGE -> Res.string.error_payload_too_large
        DataError.Remote.SERVER_ERROR -> Res.string.error_server
        DataError.Remote.SERVICE_UNAVAILABLE -> Res.string.error_service_unavailable
        DataError.Remote.SERIALIZATION -> Res.string.error_serialization
        DataError.Remote.UNKNOWN -> Res.string.error_unknown
        DataError.Connection.NOT_CONNECTED -> Res.string.error_no_internet
        DataError.Connection.MESSAGE_SEND_FAILED -> Res.string.error_unable_to_send_message
    }
    return UiText.Resource(resource)
}
