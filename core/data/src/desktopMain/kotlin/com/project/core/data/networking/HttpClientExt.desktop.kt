@file:Suppress("ktlint:standard:filename")

package com.project.core.data.networking

import com.project.core.domain.util.DataError
import com.project.core.domain.util.Result
import io.ktor.client.statement.HttpResponse

actual suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError.Remote>,
): Result<T, DataError.Remote> {
    return Result.Failure(DataError.Remote.SERVER_ERROR)
}
