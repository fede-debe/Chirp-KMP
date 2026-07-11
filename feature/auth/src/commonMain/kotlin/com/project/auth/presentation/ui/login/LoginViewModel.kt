package com.project.auth.presentation.ui.login

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.auth.domain.EmailValidator
import com.project.auth.presentation.Res
import com.project.auth.presentation.error_invalid_credentials
import com.project.auth.presentation.error_social_sign_in_failed
import com.project.auth.presentation.social.SocialProvider
import com.project.auth.presentation.social.SocialSignInResult
import com.project.core.data.util.NonceFactory
import com.project.core.data.util.PlatformUtils
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
    private val nonceFactory: NonceFactory,
) : ViewModel() {

    private var hasLoadedInitialData = false

    // Raw nonce for the in-flight social attempt. We hand the provider the *hashed* nonce and keep the
    // raw one here so we can send it to the backend once the provider returns its token. Single-use.
    private var pendingRawNonce: String? = null

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
                initSocialAvailability()
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

            LoginAction.OnGoogleSignInClick -> startSocialSignIn(SocialProvider.GOOGLE)
            LoginAction.OnAppleSignInClick -> startSocialSignIn(SocialProvider.APPLE)
            is LoginAction.OnSocialSignInResult -> onSocialSignInResult(action.provider, action.result)

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

    /** Show Google on Android + iOS; Apple on iOS only (per Apple's guidelines). Neither on desktop. */
    private fun initSocialAvailability() {
        val osName = PlatformUtils.getOSName()
        val isAndroid = osName == "ANDROID"
        val isIos = osName == "IOS"
        _state.update {
            it.copy(
                canUseGoogleSignIn = isAndroid || isIos,
                canUseAppleSignIn = isIos,
            )
        }
    }

    private fun login() {
        if (!state.value.canLogin || state.value.socialSignInLoading != null) {
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

    private fun startSocialSignIn(provider: SocialProvider) {
        // Ignore taps while an email login or another social attempt is already running.
        if (state.value.isLoggingIn || state.value.socialSignInLoading != null) {
            return
        }

        // Fresh single-use nonce per attempt: send the raw one to our backend, the hash to the provider.
        val rawNonce = nonceFactory.newRawNonce()
        pendingRawNonce = rawNonce
        val hashedNonce = nonceFactory.hashedNonce(rawNonce)

        _state.update {
            it.copy(
                socialSignInLoading = provider,
                error = null,
            )
        }

        viewModelScope.launch {
            val event = when (provider) {
                SocialProvider.GOOGLE -> LoginEvent.LaunchGoogleSignIn(hashedNonce)
                SocialProvider.APPLE -> LoginEvent.LaunchAppleSignIn(hashedNonce)
            }
            eventChannel.send(event)
        }
    }

    private fun onSocialSignInResult(provider: SocialProvider, result: SocialSignInResult) {
        when (result) {
            is SocialSignInResult.Success -> authenticateWithProvider(provider, result)

            SocialSignInResult.Cancelled -> {
                // User backed out of the provider sheet — just re-enable the buttons.
                pendingRawNonce = null
                _state.update { it.copy(socialSignInLoading = null) }
            }

            is SocialSignInResult.Failure -> {
                pendingRawNonce = null
                _state.update {
                    it.copy(
                        socialSignInLoading = null,
                        error = UiText.Resource(Res.string.error_social_sign_in_failed),
                    )
                }
            }
        }
    }

    private fun authenticateWithProvider(provider: SocialProvider, success: SocialSignInResult.Success) {
        val rawNonce = pendingRawNonce
        if (rawNonce == null) {
            _state.update { it.copy(socialSignInLoading = null) }
            return
        }

        viewModelScope.launch {
            val result = when (provider) {
                SocialProvider.GOOGLE -> authService.loginWithGoogle(
                    idToken = success.token,
                    rawNonce = rawNonce,
                )
                SocialProvider.APPLE -> authService.loginWithApple(
                    identityToken = success.token,
                    rawNonce = rawNonce,
                    fullName = success.fullName,
                )
            }

            result
                .onSuccess { authInfo ->
                    sessionStorage.set(authInfo)
                    pendingRawNonce = null
                    _state.update { it.copy(socialSignInLoading = null) }
                    eventChannel.send(LoginEvent.Success)
                }
                .onFailure { error ->
                    pendingRawNonce = null
                    _state.update {
                        it.copy(
                            socialSignInLoading = null,
                            error = socialErrorToUiText(error),
                        )
                    }
                }
        }
    }

    /**
     * Token-shaped failures (bad/expired token, wrong aud, nonce mismatch, unverified email, client
     * bug) all surface as a single generic "sign-in failed" message — we never tell the user which.
     * Genuine infrastructure errors (offline, timeout, rate limit, server) keep their specific copy.
     */
    private fun socialErrorToUiText(error: DataError.Remote): UiText {
        return when (error) {
            DataError.Remote.UNAUTHORIZED,
            DataError.Remote.BAD_REQUEST,
            DataError.Remote.FORBIDDEN,
            DataError.Remote.CONFLICT,
            DataError.Remote.NOT_FOUND,
            DataError.Remote.SERIALIZATION,
            DataError.Remote.UNKNOWN,
            -> UiText.Resource(Res.string.error_social_sign_in_failed)

            else -> error.toUiText()
        }
    }
}
