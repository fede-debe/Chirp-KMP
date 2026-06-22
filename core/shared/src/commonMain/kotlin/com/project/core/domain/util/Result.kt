package com.project.core.domain.util

/**
 * A generic wrapper for encapsulating successful outcomes or predefined failure states across all app layers.
 *
 * ## Strategy / Decisions
 * Uses a sealed interface with covariant generic types (`out D`, `out E`) to represent the outcome of
 * operations (API calls, password validation, etc.) without relying on traditional try/catch exception
 * handling. This design enforces strict type safety and exhaustiveness when checking outcomes.
 * Placed in `core/domain` so it is accessible to all feature layers without circular dependencies.
 *
 * ## How It Works
 * - Defines two variants: a `Success` data class holding a generic data payload (`D`), and a `Failure`
 *   data class holding a strictly bounded generic error (`E` of type `Error`).
 * - Uses Kotlin's `Nothing` type for the unused parameter in each variant (e.g., `Success` has no error,
 *   so `E` is `Nothing`).
 * - Exposes inline utility functions (`map`, `onSuccess`, `onFailure`) that utilize lambda functions
 *   to provide a chainable, functional approach to handling results without verbose if/else checking.
 * - Provides an `EmptyResult` type alias and `asEmptyResult()` function to easily strip payloads when
 *   only the success/failure status matters (e.g., returning `Unit`).
 *
 * ## Alternatives / Why Not
 * External libraries like `Either` provide similar functional error-handling mechanisms. However, writing
 * a custom `Result` wrapper was chosen to avoid adding external third-party dependencies purely for
 * error handling, keeping the architecture lightweight, transparent, and entirely self-contained.
 *
 * ## Technical Details
 * - `out D` and `out E` modifiers are utilized to allow covariant subtyping (passing subtypes into the wrapper).
 * - `E` is strictly bounded by the `Error` marker interface.
 *
 * @param D The type of the valid data returned on success.
 * @param E The type of the domain-specific error returned on failure.
 * @see Error
 */
sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Failure<out E : Error>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : Error, R> Result<T, E>.map(map: (T) -> R): Result<R, E> {
    return when (this) {
        is Result.Failure -> Result.Failure(error)
        is Result.Success -> Result.Success(map(this.data))
    }
}

inline fun <T, E : Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    return when (this) {
        is Result.Failure -> this
        is Result.Success -> {
            action(this.data)
            this
        }
    }
}

inline fun <T, E : Error> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    return when (this) {
        is Result.Failure -> {
            action(error)
            this
        }
        is Result.Success -> this
    }
}

fun <T, E : Error> Result<T, E>.asEmptyResult(): EmptyResult<E> {
    return map { }
}

typealias EmptyResult<E> = Result<Unit, E>
