package com.project.auth.presentation.social

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Sign in with Apple is iOS-only, so the Apple button is never shown on Android (`canUseAppleSignIn`
 * stays false). This actual exists only to satisfy the `expect`; if ever invoked it fails cleanly.
 */
@Composable
actual fun rememberAppleSignInLauncher(
    onResult: (SocialSignInResult) -> Unit,
): SocialSignInLauncher {
    return remember(onResult) {
        SocialSignInLauncher {
            onResult(SocialSignInResult.Failure("Sign in with Apple is not available on Android"))
        }
    }
}
