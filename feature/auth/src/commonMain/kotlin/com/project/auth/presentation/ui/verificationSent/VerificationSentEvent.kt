package com.project.auth.presentation.ui.verificationSent

sealed interface VerificationSentEvent {
    data object ResendVerificationEmailSuccess : VerificationSentEvent
}
