package com.project.auth.presentation.social

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Social sign-in isn't offered on desktop (`canUseAppleSignIn` stays false there). This actual exists
 * only to satisfy the common `expect`; if ever invoked it fails cleanly.
 */
@Composable
actual fun rememberAppleSignInLauncher(
    onResult: (SocialSignInResult) -> Unit,
): SocialSignInLauncher {
    return remember(onResult) {
        SocialSignInLauncher {
            onResult(SocialSignInResult.Failure("Sign in with Apple is not available on desktop"))
        }
    }
}
