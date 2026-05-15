package com.project.auth.presentation.register

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chirp.feature.auth.presentation.generated.resources.Res
import chirp.feature.auth.presentation.generated.resources.error_account_exists
import chirp.feature.auth.presentation.generated.resources.error_invalid_email
import chirp.feature.auth.presentation.generated.resources.error_invalid_password
import chirp.feature.auth.presentation.generated.resources.error_invalid_username
import com.project.auth.domain.EmailValidator
import com.project.core.domain.auth.AuthService
import com.project.core.domain.util.DataError
import com.project.core.domain.util.onFailure
import com.project.core.domain.util.onSuccess
import com.project.core.domain.validation.PasswordValidator
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

/**
 * Acts as the brain of the Register UI, receiving user actions and mutating the screen's state.
 *
 * ## Strategy / Decisions
 * - **Reactive State Management:** Uses Kotlin `StateFlow` instead of raw Compose `State` to hold the UI state. This enables reactive programming, making it easier to automatically chain operations (e.g., listening to text changes to validate an email state automatically via Flow operators).
 * - **Lazy Initialization:** Utilizes the `onStart` flow operator to trigger initial data loading rather than the `init` block. This decouples object instantiation from side effects, allowing strict control from the outside over when data is collected.
 *
 * ## How It Works
 * 1. Exposes an `onAction` function that accepts `RegisterAction` events.
 * 2. Evaluates the incoming action via a `when` expression.
 * 3. Updates the relevant properties in the internal StateFlow based on the exact action (e.g., updating the password state if the action was `EnterPassword`).
 * 4. When the UI begins collecting the Flow, the `onStart` operator checks if initial data is needed and loads it.
 *
 * ## Alternatives / Why Not
 * - **Compose State in ViewModel:** Rejected in favor of `StateFlow`. While Compose State successfully notifies the UI of changes, it lacks the rich, reactive stream operators of Kotlin Flows which are beneficial for reactive form validations.
 * - **Loading Data in `init` block:** Rejected because placing initial API calls or data loading inside the `init` block triggers side effects immediately upon creation. This makes testing highly difficult, as creating a ViewModel reference for an isolated test would unnecessarily trigger initialization logic without providing a way to bypass it.
 *
 * ## Technical Details
 * - Flow emissions must be converted back to Compose State in the UI layer using `collectAsStateWithLifecycle()`.
 */

/**
 * Manages UI state, captures user input, and coordinates form validation for the registration flow.
 *
 * ## Strategy / Decisions
 * Form validation is executed in a single batch function (`validateFormInputs`). To ensure a clean slate,
 * all UI errors are cleared at the very beginning of the validation run before being re-evaluated and
 * updated with the latest state. String resources are dynamically mapped based on validation state failures.
 *
 * ## How It Works
 * 1. Calls `clearAllTextFieldErrors()` to reset the UI state.
 * 2. Extracts the underlying strings from the current `emailTextState`, `usernameTextState`, and `passwordTextState`.
 * 3. Passes the extracted strings through the domain validators (`EmailValidator`, `PasswordValidator`).
 * 4. Checks username validation locally (validates length between 3 and 20 characters).
 * 5. Maps any validation failures to their respective UI String resources (e.g., `error_invalid_email`, `error_invalid_password`).
 * 6. Commits the new errors to the UI state.
 *
 * ## Alternatives / Why Not
 * - **Clearing errors on field focus (`onInputTextFocusGain`):** Initially attempted, but ultimately rejected
 *   due to poor User Experience (UX). Clearing all text field errors just because the user focused on one
 *   input hides valuable context before the user has a chance to correct the specific issue.
 *
 * Technical Details:
 * - **Thread Safety & Race Conditions:** State mutations strictly utilize `state.update { it.copy(...) }`.
 *   The alternative `state.value = state.value.copy()` is explicitly avoided. `update` guarantees an atomic
 *   transaction, preventing race conditions between reading and writing the state when operating in a multithreaded Coroutine environment.
 *
 * @return `true` if all fields (username, email, password) pass validation, `false` otherwise.
 */
class RegisterViewModel(
    private val authService: AuthService,
) : ViewModel() {

    private val eventChannel = Channel<RegisterEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(RegisterState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeValidationStates()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = RegisterState(),
        )

    private val isEmailValidFlow = snapshotFlow { state.value.emailTextState.text.toString() }
        .map { email -> EmailValidator.validate(email) }
        .distinctUntilChanged()

    private val isUsernameValidFlow = snapshotFlow { state.value.usernameTextState.text.toString() }
        .map { username -> username.length in 3..20 }
        .distinctUntilChanged()

    private val isPasswordValidFlow = snapshotFlow { state.value.passwordTextState.text.toString() }
        .map { password -> PasswordValidator.validate(password).isValidPassword }
        .distinctUntilChanged()

    private val isRegisteringFlow = state
        .map { it.isRegistering }
        .distinctUntilChanged()

    private fun observeValidationStates() {
        combine(
            isEmailValidFlow,
            isUsernameValidFlow,
            isPasswordValidFlow,
            isRegisteringFlow,
        ) { isEmailValid, isUsernameValid, isPasswordValid, isRegistering ->
            val allValid = isEmailValid && isUsernameValid && isPasswordValid
            _state.update {
                it.copy(
                    canRegister = !isRegistering && allValid,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: RegisterAction) {
        when (action) {
            RegisterAction.OnLoginClick -> Unit
            RegisterAction.OnRegisterClick -> register()
            RegisterAction.OnTogglePasswordVisibilityClick -> {
                _state.update {
                    it.copy(
                        isPasswordVisible = !it.isPasswordVisible,
                    )
                }
            }
            else -> Unit
        }
    }

    private fun register() {
        if (!validateFormInputs()) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isRegistering = true,
                )
            }

            val email = state.value.emailTextState.text.toString()
            val username = state.value.usernameTextState.text.toString()
            val password = state.value.passwordTextState.text.toString()

            authService
                .register(
                    email = email,
                    username = username,
                    password = password,
                )
                .onSuccess {
                    _state.update {
                        it.copy(
                            isRegistering = false,
                        )
                    }
                    eventChannel.send(RegisterEvent.Success(email))
                }
                .onFailure { error ->
                    val registrationError = when (error) {
                        DataError.Remote.CONFLICT -> UiText.Resource(Res.string.error_account_exists)
                        else -> error.toUiText()
                    }
                    _state.update {
                        it.copy(
                            isRegistering = false,
                            registrationError = registrationError,
                        )
                    }
                }
        }
    }

    private fun clearAllTextFieldErrors() {
        _state.update {
            it.copy(
                emailError = null,
                usernameError = null,
                passwordError = null,
                registrationError = null,
            )
        }
    }

    private fun validateFormInputs(): Boolean {
        clearAllTextFieldErrors()

        val currentState = state.value
        val email = currentState.emailTextState.text.toString()
        val username = currentState.usernameTextState.text.toString()
        val password = currentState.passwordTextState.text.toString()

        val isEmailValid = EmailValidator.validate(email)
        val passwordValidationState = PasswordValidator.validate(password)
        val isUsernameValid = username.length in 3..20

        val emailError = if (!isEmailValid) {
            UiText.Resource(Res.string.error_invalid_email)
        } else {
            null
        }
        val usernameError = if (!isUsernameValid) {
            UiText.Resource(Res.string.error_invalid_username)
        } else {
            null
        }
        val passwordError = if (!passwordValidationState.isValidPassword) {
            UiText.Resource(Res.string.error_invalid_password)
        } else {
            null
        }

        _state.update {
            it.copy(
                emailError = emailError,
                usernameError = usernameError,
                passwordError = passwordError,
            )
        }

        return isUsernameValid && isEmailValid && passwordValidationState.isValidPassword
    }
}
