package com.project.auth.presentation.ui.login

sealed interface LoginEvent {
    data object Success : LoginEvent
}
