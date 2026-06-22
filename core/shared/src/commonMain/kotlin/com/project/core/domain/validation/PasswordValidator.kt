package com.project.core.domain.validation

/**
 * Evaluates password strings against the application's security requirements to generate a detailed validation state.
 *
 * ## Strategy / Decisions
 * Uses explicit Kotlin collection functions (`length`, `any`) to validate individual requirements.
 * This strategy prioritizes extreme readability and explicit mapping of business requirements over concise but cryptic code.
 *
 * ## How It Works
 * 1. Checks if the password length is greater than or equal to the `minPasswordLength` (9).
 * 2. Iterates through the string using `.any { it.isDigit() }` to verify numerical requirements.
 * 3. Iterates through the string using `.any { it.isUpperCase() }` to verify casing requirements.
 * 4. Bundles these explicit boolean results into a `PasswordValidationState` instance.
 *
 * ## Alternatives / Why Not
 * - **Regular Expressions:** Explicitly rejected for password validation. While regex is used for emails
 *   due to external complexity, using a regex for passwords obfuscates the actual requirements. Explicit
 *   Kotlin property checks allow any developer to understand the exact password rules (min length, digits, casing)
 *   at a single glance.
 *
 * @param password The raw string input from the password text field.
 * @return A [PasswordValidationState] detailing exactly which rules passed or failed.
 */
object PasswordValidator {

    private const val MIN_PASSWORD_LENGTH = 9

    fun validate(password: String): PasswordValidationState {
        return PasswordValidationState(
            hasMinLength = password.length >= MIN_PASSWORD_LENGTH,
            hasDigit = password.any { it.isDigit() },
            hasUppercase = password.any { it.isUpperCase() },
        )
    }
}
