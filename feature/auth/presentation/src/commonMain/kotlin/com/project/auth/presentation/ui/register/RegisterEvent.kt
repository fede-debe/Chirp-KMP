package com.project.auth.presentation.ui.register

sealed interface RegisterEvent {
    data class Success(val email: String) : RegisterEvent
}
