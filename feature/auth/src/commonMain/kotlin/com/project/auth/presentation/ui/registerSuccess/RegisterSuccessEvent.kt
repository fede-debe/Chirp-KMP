package com.project.auth.presentation.ui.registerSuccess

sealed interface RegisterSuccessEvent {
    data object ResendVerificationEmailSuccess : RegisterSuccessEvent
}
