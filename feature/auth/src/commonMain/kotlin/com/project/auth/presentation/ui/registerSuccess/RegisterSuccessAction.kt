package com.project.auth.presentation.ui.registerSuccess

sealed interface RegisterSuccessAction {
    data object OnLoginClick : RegisterSuccessAction
    data object OnResendVerificationEmailClick : RegisterSuccessAction
}
