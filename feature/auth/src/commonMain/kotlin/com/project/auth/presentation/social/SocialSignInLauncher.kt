package com.project.auth.presentation.social

/**
 * A handle the login screen calls to start a provider sign-in sheet, mirroring the project's
 * `remember*Launcher` media-picker convention. The screen passes the [hashedNonce] (SHA-256 of the
 * raw nonce the ViewModel generated); the launcher hands that to the provider and reports the outcome
 * through the `onResult` callback supplied at construction.
 */
fun interface SocialSignInLauncher {
    fun launch(hashedNonce: String)
}
