package com.project.core.domain.util

/**
 * A sealed interface representing all expected errors that can occur during data operations.
 *
 * ## Strategy / Decisions
 * Errors are grouped into distinct enum classes (`Remote` and `Local`) rather than throwing
 * generic exceptions. This guarantees exhaustive, type-safe error checking downstream (e.g.,
 * in ViewModels), ensuring the UI layer only has to handle a predictable, predefined set of
 * domain errors.
 *
 * ## How It Works
 * Implements the `Error` marker interface. It exposes specific enums for remote API failures
 * (like `REQUEST_TIMEOUT`, `UNAUTHORIZED`, `PAYLOAD_TOO_LARGE`) and local database/storage
 * failures (like `DISK_FULL`). When an API call fails, the exact enum case is returned inside
 * a `Result.Failure`.
 *
 * @see Error
 */
sealed interface DataError : Error {
    enum class Remote : DataError {
        BAD_REQUEST,
        REQUEST_TIMEOUT,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        PAYLOAD_TOO_LARGE,
        SERVER_ERROR,
        SERVICE_UNAVAILABLE,
        SERIALIZATION,
        UNKNOWN,
    }

    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN,
    }
}
