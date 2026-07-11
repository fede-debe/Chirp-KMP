package com.project.auth.presentation.ui.verificationSent

import com.project.core.presentation.util.UiText

data class VerificationSentState(
    val email: String = "",
    val isResendingVerificationEmail: Boolean = false,
    val resendVerificationError: UiText? = null,
)
