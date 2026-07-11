package com.project.auth.presentation.ui.login

import androidx.compose.foundation.text.input.TextFieldState
import com.project.auth.presentation.social.SocialProvider
import com.project.core.presentation.util.UiText

data class LoginState(
    val emailTextFieldState: TextFieldState = TextFieldState(),
    val passwordTextFieldState: TextFieldState = TextFieldState(),
    val isPasswordVisible: Boolean = false,
    val canLogin: Boolean = false,
    val isLoggingIn: Boolean = false,
    val error: UiText? = null,
    // Which providers to show — resolved from the platform on init (Google: Android + iOS; Apple: iOS).
    val canUseGoogleSignIn: Boolean = false,
    val canUseAppleSignIn: Boolean = false,
    // Non-null while a provider flow (or its backend exchange) is in progress; drives button loading/disable.
    val socialSignInLoading: SocialProvider? = null,
)
