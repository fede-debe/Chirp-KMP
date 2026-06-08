package com.project.core.domain.util
/**
 * A custom exception wrapper utilized to pass domain-specific data errors through standard exception channels.
 *
 * ## Strategy / Decisions
 * - **Type-Safe Error Handling:** By wrapping our domain `DataError` inside an Exception, we can propagate
 * custom API or networking errors through standard `try/catch` blocks used by the paginator.
 *
 * ## How It Works
 * 1. Accepts a `DataError` enum/sealed class as a property.
 * 2. When caught by the View Model, the error can be extracted and mapped safely to a localized `UiText`
 * instead of relying on standard String exception messages.
 *
 * @param error The underlying domain-specific DataError.
 */
class DataErrorException(
    val error: DataError,
) : Exception()
