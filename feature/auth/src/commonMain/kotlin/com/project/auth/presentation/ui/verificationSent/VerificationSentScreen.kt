package com.project.auth.presentation.ui.verificationSent

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.auth.presentation.Res
import com.project.auth.presentation.email_not_verified_title
import com.project.auth.presentation.login
import com.project.auth.presentation.resend_verification_email
import com.project.auth.presentation.resent_verification_email
import com.project.auth.presentation.verification_email_sent_to_x
import com.project.core.designsystem.components.brand.ChirpSuccessIcon
import com.project.core.designsystem.components.buttons.ChirpButton
import com.project.core.designsystem.components.buttons.ChirpButtonStyle
import com.project.core.designsystem.components.layouts.ChirpAdaptiveResultLayout
import com.project.core.designsystem.components.layouts.ChirpSimpleResultLayout
import com.project.core.designsystem.components.layouts.ChirpSnackbarScaffold
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.presentation.util.ObserveAsEvents
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Displays the "Verify your email" confirmation shown when an unverified user tries to log in.
 *
 * The backend blocks the login with a 403 and (when not rate-limited) has already resent the verification
 * email, so this screen confirms where the email was sent and offers a manual resend as a backup. It is the
 * login-flow counterpart of [com.project.auth.presentation.ui.registerSuccess.RegisterSuccessScreen] and is
 * built from the same layout primitives for visual consistency.
 */
@Composable
fun VerificationSentRoot(
    viewModel: VerificationSentViewModel = koinViewModel(),
    onLoginClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is VerificationSentEvent.ResendVerificationEmailSuccess -> {
                snackbarHostState.showSnackbar(
                    message = getString(
                        resource = Res.string.resent_verification_email,
                    ),
                )
            }
        }
    }

    VerificationSentScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is VerificationSentAction.OnLoginClick -> onLoginClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun VerificationSentScreen(
    state: VerificationSentState,
    onAction: (VerificationSentAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    ChirpSnackbarScaffold(
        snackbarHostState = snackbarHostState,
    ) {
        ChirpAdaptiveResultLayout {
            ChirpSimpleResultLayout(
                title = stringResource(Res.string.email_not_verified_title),
                description = stringResource(
                    Res.string.verification_email_sent_to_x,
                    state.email,
                ),
                icon = {
                    ChirpSuccessIcon()
                },
                primaryButton = {
                    ChirpButton(
                        text = stringResource(Res.string.login),
                        onClick = {
                            onAction(VerificationSentAction.OnLoginClick)
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                },
                secondaryButton = {
                    ChirpButton(
                        text = stringResource(Res.string.resend_verification_email),
                        onClick = {
                            onAction(VerificationSentAction.OnResendVerificationEmailClick)
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        enabled = !state.isResendingVerificationEmail,
                        isLoading = state.isResendingVerificationEmail,
                        style = ChirpButtonStyle.SECONDARY,
                    )
                },
                secondaryError = state.resendVerificationError?.asString(),
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ChirpTheme {
        VerificationSentScreen(
            state = VerificationSentState(
                email = "test@preview.com",
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
