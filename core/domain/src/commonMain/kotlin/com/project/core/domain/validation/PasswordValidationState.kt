package com.project.core.domain.validation

/**
 * Bundles the granular validation status of a password against specific security requirements.
 *
 * ## Strategy / Decisions
 * This class is placed in `core.domain.validation` rather than `auth.domain`. Password validation
 * is required across multiple features—not just registration, but also in the profile/chat features
 * where users can change their passwords. Placing it in `core` prevents cross-module dependency issues.
 *
 * ## How It Works
 * 1. Acts as a data class holding boolean flags for each specific password requirement.
 * 2. Tracks `hasMinLength` (minimum 9 characters).
 * 3. Tracks `hasDigit` (at least one number).
 * 4. Tracks `hasUppercase` (at least one uppercase letter).
 * 5. Provides an extension getter property (`isValidPassword`) that evaluates to true only if all
 *    underlying boolean requirements are met.
 */
data class PasswordValidationState(
    val hasMinLength: Boolean = false,
    val hasDigit: Boolean = false,
    val hasUppercase: Boolean = false,
) {
    val isValidPassword: Boolean
        get() = hasMinLength && hasDigit && hasUppercase
}
