package com.project.auth.presentation.ui.verificationSent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.core.domain.auth.AuthService
import com.project.core.domain.util.onFailure
import com.project.core.domain.util.onSuccess
import com.project.core.presentation.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs the "Verify your email" screen shown when an unverified user attempts to log in.
 *
 * ## Strategy / Decisions
 * - **Mirrors [RegisterSuccessViewModel]:** Both screens confirm that a verification email was sent and
 *   expose a manual resend action, so this ViewModel intentionally follows the same minimal shape rather
 *   than introducing a new pattern.
 * - **Email via route argument:** The email is supplied by the navigation argument (the value the user typed
 *   on the login screen) and surfaced through [SavedStateHandle], keeping this destination self-contained.
 *
 * ## How It Works
 * 1. Reads the `email` navigation argument on creation; absence is a programmer error and fails fast.
 * 2. Holds it in state for display in the confirmation copy.
 * 3. On [VerificationSentAction.OnResendVerificationEmailClick], calls [AuthService.resendVerificationEmail]
 *    and emits [VerificationSentEvent.ResendVerificationEmailSuccess] so the UI can show a snackbar.
 */
class VerificationSentViewModel(
    private val authService: AuthService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val eventChannel = Channel<VerificationSentEvent>()
    val events = eventChannel.receiveAsFlow()

    private val email = savedStateHandle.get<String>("email")
        ?: throw IllegalStateException("No email passed to verification sent screen")

    private val _state = MutableStateFlow(
        VerificationSentState(
            email = email,
        ),
    )
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                /** Load initial data here **/
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = VerificationSentState(),
        )

    fun onAction(action: VerificationSentAction) {
        when (action) {
            is VerificationSentAction.OnResendVerificationEmailClick -> resendVerification()
            else -> Unit
        }
    }

    private fun resendVerification() {
        if (state.value.isResendingVerificationEmail) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isResendingVerificationEmail = true,
                )
            }

            authService
                .resendVerificationEmail(email)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isResendingVerificationEmail = false,
                        )
                    }
                    eventChannel.send(VerificationSentEvent.ResendVerificationEmailSuccess)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isResendingVerificationEmail = false,
                            resendVerificationError = error.toUiText(),
                        )
                    }
                }
        }
    }
}
