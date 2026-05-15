package com.project.auth.presentation.registerSuccess

sealed interface RegisterSuccessAction {
    data object OnLoginClick : RegisterSuccessAction
    data object OnResendVerificationEmailClick : RegisterSuccessAction
}
