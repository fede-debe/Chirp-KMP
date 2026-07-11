package com.project.auth.presentation.social

import androidx.compose.runtime.Composable

/**
 * Remembers a launcher that runs the platform's native Google sign-in flow and reports a
 * [SocialSignInResult] via [onResult].
 *
 * - **Android:** Credential Manager + Google ID (`serverClientId` = web client id, `setNonce`).
 * - **iOS:** OAuth via `ASWebAuthenticationSession` (no GoogleSignIn SDK), reading the client id from
 *   `GoogleService-Info.plist`. Currently mints `aud = iOS client id`, which the backend must allowlist.
 * - **Desktop:** unsupported (the button is hidden); returns [SocialSignInResult.Failure].
 */
@Composable
expect fun rememberGoogleSignInLauncher(
    onResult: (SocialSignInResult) -> Unit,
): SocialSignInLauncher
