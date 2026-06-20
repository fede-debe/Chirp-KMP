package com.project.auth.presentation.ui.forgotPassword

sealed interface ForgotPasswordAction {
    data object OnSubmitClick : ForgotPasswordAction
}
