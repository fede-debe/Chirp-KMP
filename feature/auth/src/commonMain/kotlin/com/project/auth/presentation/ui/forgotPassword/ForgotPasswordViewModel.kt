package com.project.auth.presentation.ui.forgotPassword

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.auth.domain.EmailValidator
import com.project.core.domain.auth.AuthService
import com.project.core.domain.util.onFailure
import com.project.core.domain.util.onSuccess
import com.project.core.presentation.util.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Remote implementation of the authentication service for handling user password recovery.
 *
 * ## Strategy / Decisions
 * Implements a `forgotPassword` POST endpoint to bridge the local app state with the backend remote
 * server. It relies on a typed `EmailRequest` body to safely encapsulate the raw email string.
 *
 * ## How It Works
 * 1. Takes the user's email address as an input.
 * 2. Issues an HTTP POST request to the `/auth/forgot-password` route.
 * 3. Wraps the email in an `EmailRequest` JSON body.
 * 4. Awaits the response and maps it to a `Result<Unit, DataError.Remote>`.
 *
 * ## Technical Details
 * - Executes as a suspend function, requiring a coroutine context.
 * - Expects an empty body (`Unit`) on success, delegating error mapping to the network utility.
 *
 * @param email The target email address to send the recovery link to.
 * @return A Result containing Unit on success, or a mapped DataError.Remote on failure.
 */
class ForgotPasswordViewModel(private val authService: AuthService) : ViewModel() {

    private var hasLoadedInitialData = false

    private val isEmailValidFlow = snapshotFlow { state.value.emailTextFieldState.text.toString() }
        .map { email -> EmailValidator.validate(email) }
        .distinctUntilChanged()

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeValidationState()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ForgotPasswordState(),
        )

    fun onAction(action: ForgotPasswordAction) {
        when (action) {
            is ForgotPasswordAction.OnSubmitClick -> submitForgotPasswordRequest()
        }
    }

    private fun observeValidationState() {
        isEmailValidFlow.onEach { isEmailValid ->
            _state.update {
                it.copy(
                    canSubmit = isEmailValid,
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun submitForgotPasswordRequest() {
        if (state.value.isLoading || !state.value.canSubmit) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isEmailSentSuccessfully = false,
                    errorText = null,
                )
            }

            val email = state.value.emailTextFieldState.text.toString()
            authService
                .forgotPassword(email)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isEmailSentSuccessfully = true,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            errorText = error.toUiText(),
                            isLoading = false,
                        )
                    }
                }
        }
    }
}
