package com.project.auth.presentation.register

sealed interface RegisterEvent {
    data class Success(val email: String) : RegisterEvent
}
