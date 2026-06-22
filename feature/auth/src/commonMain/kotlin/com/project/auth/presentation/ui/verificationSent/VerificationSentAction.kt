package com.project.auth.presentation.ui.verificationSent

sealed interface VerificationSentAction {
    data object OnLoginClick : VerificationSentAction
    data object OnResendVerificationEmailClick : VerificationSentAction
}
