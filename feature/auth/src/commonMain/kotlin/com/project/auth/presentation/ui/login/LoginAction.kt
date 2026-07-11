package com.project.auth.presentation.ui.login

import com.project.auth.presentation.social.SocialProvider
import com.project.auth.presentation.social.SocialSignInResult

sealed interface LoginAction {
    data object OnTogglePasswordVisibility : LoginAction
    data object OnForgotPasswordClick : LoginAction
    data object OnLoginClick : LoginAction
    data object OnSignUpClick : LoginAction
    data object OnGoogleSignInClick : LoginAction
    data object OnAppleSignInClick : LoginAction

    /** Reported by the screen once a provider sign-in sheet resolves (success / cancel / failure). */
    data class OnSocialSignInResult(
        val provider: SocialProvider,
        val result: SocialSignInResult,
    ) : LoginAction
}
