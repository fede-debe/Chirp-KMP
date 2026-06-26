package com.project.auth.presentation.social

import androidx.compose.runtime.Composable

/**
 * Remembers a launcher that runs the native "Sign in with Apple" flow and reports a
 * [SocialSignInResult] via [onResult].
 *
 * - **iOS:** `ASAuthorizationController` with the Apple ID provider (scopes: fullName, email;
 *   `request.nonce` = hashed nonce). Returns the identity token and — first sign-in only — the name.
 * - **Android / Desktop:** unsupported (the button is iOS-only); returns [SocialSignInResult.Failure].
 */
@Composable
expect fun rememberAppleSignInLauncher(
    onResult: (SocialSignInResult) -> Unit,
): SocialSignInLauncher
