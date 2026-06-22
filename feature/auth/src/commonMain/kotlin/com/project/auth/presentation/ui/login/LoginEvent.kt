package com.project.auth.presentation.ui.login

sealed interface LoginEvent {
    data object Success : LoginEvent
    data class EmailNotVerified(val email: String) : LoginEvent
}
