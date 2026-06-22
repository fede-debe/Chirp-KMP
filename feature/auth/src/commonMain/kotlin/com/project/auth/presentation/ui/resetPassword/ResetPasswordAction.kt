package com.project.auth.presentation.ui.resetPassword

sealed interface ResetPasswordAction {
    data object OnSubmitClick : ResetPasswordAction
    data object OnTogglePasswordVisibilityClick : ResetPasswordAction
}
