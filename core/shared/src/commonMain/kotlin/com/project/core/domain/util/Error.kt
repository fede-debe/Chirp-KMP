package com.project.core.domain.util

/**
 * A marker interface used to explicitly define and group system errors.
 *
 * ## Strategy / Decisions
 * By establishing a dedicated marker interface with no body, we can strictly bound the error
 * type (`E`) in our `Result` wrappers. This guarantees that only valid, explicitly designated
 * error types can be passed into failure states, preventing arbitrary or untracked exceptions
 * from polluting the domain layer.
 *
 * ## How It Works
 * Any specific set of errors (like `DataError` or `PasswordValidationError`) must implement
 * this interface. It serves purely as a type-safety contract.
 */
interface Error
