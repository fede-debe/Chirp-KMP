package com.project.auth.presentation.ui.login

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.auth.domain.EmailValidator
import com.project.auth.presentation.Res
import com.project.auth.presentation.error_invalid_credentials
import com.project.core.domain.auth.AuthService
import com.project.core.domain.auth.SessionStorage
import com.project.core.domain.util.DataError
import com.project.core.domain.util.onFailure
import com.project.core.domain.util.onSuccess
import com.project.core.presentation.util.UiText
import com.project.core.presentation.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authService: AuthService,
    private val sessionStorage: SessionStorage,
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val eventChannel = Channel<LoginEvent>()
    val events = eventChannel.receiveAsFlow()

    private val isEmailValidFlow = snapshotFlow { state.value.emailTextFieldState.text.toString() }
        .map { email ->
            val cleanEmail = email.trim()
            EmailValidator.validate(cleanEmail)
        }
        .distinctUntilChanged()

    private val isPasswordNotBlankFlow =
        snapshotFlow { state.value.passwordTextFieldState.text.toString() }
            .map { it.isNotBlank() }
            .distinctUntilChanged()

    private val _state = MutableStateFlow(LoginState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeTextStates()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = LoginState(),
        )

    private val isRegisteringFlow = state
        .map { it.isLoggingIn }
        .distinctUntilChanged()

    fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.OnLoginClick -> login()
            LoginAction.OnTogglePasswordVisibility -> {
                _state.update {
                    it.copy(
                        isPasswordVisible = !it.isPasswordVisible,
                    )
                }
            }

            else -> Unit
        }
    }

    private fun observeTextStates() {
        combine(
            isEmailValidFlow,
            isPasswordNotBlankFlow,
            isRegisteringFlow,
        ) { isEmailValid, isPasswordNotBlank, isRegistering ->
            _state.update {
                it.copy(
                    canLogin = !isRegistering && isEmailValid && isPasswordNotBlank,
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun login() {
        if (!state.value.canLogin) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoggingIn = true,
                )
            }

            val email = state.value.emailTextFieldState.text.toString().trim()
            val password = state.value.passwordTextFieldState.text.toString()

            authService
                .login(
                    email = email,
                    password = password,
                )
                .onSuccess { authInfo ->
                    sessionStorage.set(authInfo)

                    _state.update {
                        it.copy(
                            isLoggingIn = false,
                        )
                    }
                    eventChannel.send(LoginEvent.Success)
                }
                .onFailure { error ->
                    // The backend only returns 403 on login when the account's email isn't verified
                    // (and, when not rate-limited, it has already resent the verification email). Route
                    // the user to the confirmation screen instead of showing an inline error.
                    if (error == DataError.Remote.FORBIDDEN) {
                        _state.update {
                            it.copy(
                                isLoggingIn = false,
                            )
                        }
                        eventChannel.send(LoginEvent.EmailNotVerified(email))
                    } else {
                        val errorMessage = when (error) {
                            DataError.Remote.UNAUTHORIZED -> UiText.Resource(Res.string.error_invalid_credentials)
                            else -> error.toUiText()
                        }

                        _state.update {
                            it.copy(
                                error = errorMessage,
                                isLoggingIn = false,
                            )
                        }
                    }
                }
        }
    }
}
