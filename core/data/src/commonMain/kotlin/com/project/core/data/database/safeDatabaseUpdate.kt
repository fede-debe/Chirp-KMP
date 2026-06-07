@file:Suppress("ktlint:standard:filename", "filename")

package com.project.core.data.database

import androidx.sqlite.SQLiteException
import com.project.core.domain.util.DataError
import com.project.core.domain.util.Result

suspend inline fun <T> safeDatabaseUpdate(update: suspend () -> T): Result<T, DataError.Local> {
    return try {
        Result.Success(update())
    } catch (_: SQLiteException) {
        Result.Failure(DataError.Local.DISK_FULL)
    }
}
