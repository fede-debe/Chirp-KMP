package com.project.auth.presentation.ui.login

sealed interface LoginEvent {
    data object Success : LoginEvent
    data class EmailNotVerified(val email: String) : LoginEvent

    /** Ask the screen to start the native Google sheet with this hashed nonce. */
    data class LaunchGoogleSignIn(val hashedNonce: String) : LoginEvent

    /** Ask the screen to start the native Apple sheet with this hashed nonce. */
    data class LaunchAppleSignIn(val hashedNonce: String) : LoginEvent
}
