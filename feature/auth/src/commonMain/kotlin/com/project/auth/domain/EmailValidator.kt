package com.project.auth.domain

/**
 * Validates user email addresses using a predefined regular expression.
 *
 * ## Strategy / Decisions
 * This validation logic resides in the domain layer as it constitutes core business logic.
 * An object is used since no state or dependencies need to be injected. A regular expression
 * was chosen as the most pragmatic approach to cover the broad range of typical email formats.
 *
 * ## How It Works
 * 1. Defines a private constant regular expression pattern.
 * 2. The `validate` function converts the pattern string to a Regex object.
 * 3. It calls `matches()` against the provided email string to return a boolean result.
 *
 * ## Alternatives / Why Not
 * - **Android SDK Patterns (`android.util.Patterns.EMAIL_ADDRESS`):** Rejected. Since this is a
 *   Kotlin Multiplatform (KMP) project, Android-specific SDKs cannot be used in the shared domain layer.
 * - **Platform-Specific Implementations (`expect`/`actual`):** Rejected. Writing and maintaining
 *   separate email validation logic for Android and iOS was deemed unnecessary overhead compared to a shared regex.
 *
 * Technical Details:
 * The regex approach is acknowledged to be complex and will not catch every single extreme edge case
 * (e.g., complex addresses with 5+ subdomains). However, it is optimized to handle the vast majority
 * of standard email addresses effectively.
 *
 * @param email The raw string input from the email text field.
 * @return `true` if the email matches the standard pattern, `false` otherwise.
 */
object EmailValidator {

    private const val EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"

    fun validate(email: String): Boolean {
        return EMAIL_PATTERN.toRegex().matches(email)
    }
}
