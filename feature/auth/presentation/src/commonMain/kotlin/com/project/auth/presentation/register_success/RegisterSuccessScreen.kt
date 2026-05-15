package com.project.auth.presentation.register_success

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import chirp.feature.auth.presentation.generated.resources.Res
import chirp.feature.auth.presentation.generated.resources.account_successfully_created
import chirp.feature.auth.presentation.generated.resources.login
import chirp.feature.auth.presentation.generated.resources.resend_verification_email
import chirp.feature.auth.presentation.generated.resources.verification_email_sent_to_x
import com.project.core.designsystem.components.brand.ChirpSuccessIcon
import com.project.core.designsystem.components.buttons.ChirpButton
import com.project.core.designsystem.components.buttons.ChirpButtonStyle
import com.project.core.designsystem.components.layouts.ChirpAdaptiveResultLayout
import com.project.core.designsystem.components.layouts.ChirpSimpleSuccessLayout
import com.project.core.designsystem.theme.ChirpTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Displays the successful registration screen, confirming the account creation and allowing the user to navigate to log in or resend the verification email.
 *
 * ## Strategy / Decisions
 * - **Layout Reusability:** Built entirely by composing existing layout primitives (`ChirpAdaptiveResultLayout` and `ChirpSimpleSuccessLayout`). This approach significantly reduces development time and enforces visual consistency across the entire authentication feature.
 * - **Lightweight State Management:** The state is intentionally kept minimal, only tracking the `registeredEmail` (for UI display) and `isResendingVerificationEmail` (to manage button states and loading indicators).
 *
 * ## How It Works
 * 1. **State Observation:** Receives `RegisterSuccessState` to access the user's email and current loading status.
 * 2. **Root Layout:** Wraps the entire screen in `ChirpAdaptiveResultLayout` since this is conceptually a result/success destination.
 * 3. **Content Layout:** Uses `ChirpSimpleSuccessLayout` to render the textual content, setting the `ChirpSuccessIcon` (a checkmark) as the top visual element.
 * 4. **String Formatting:** Fetches the localized description string and injects the `registeredEmail` state into the string's placeholder.
 * 5. **Action Binding:**
 *    - The Primary Button (Login) fires `OnLoginClick` directly without a loading state, as navigation is instant.
 *    - The Secondary Button (Resend Email) fires `OnResendVerificationEmailClick`. It binds its `isLoading` and `enabled` parameters to the inverse of `isResendingVerificationEmail` to prevent multiple concurrent API requests.
 *
 * ## Alternatives / Why Not
 * - **Standard Android String Placeholders Rejection:** Initially, the standard `%s` placeholder was used for the email injection. This was rejected and replaced because it fails to render in Compose Multiplatform.
 *
 * ## Technical Details
 * - **String Resource Constraints:** Kotlin Multiplatform resource generation requires string templates to specify the argument number and type explicitly. Placeholders must use the format `%1$s` rather than the traditional `%s` to resolve properly at runtime.
 * - **Dependency Injection:** Assumes the associated ViewModel is provided via Koin (`koinViewModel()`) from the auth presentation module, passing the initial email state upon creation.
 *
 * @param state The current [RegisterSuccessState] holding the user's email and loading status.
 * @param onAction Callback to propagate user intents (login, resend email) up to the ViewModel.
 */
@Composable
fun RegisterSuccessRoot(
    viewModel: RegisterSuccessViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RegisterSuccessScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun RegisterSuccessScreen(
    state: RegisterSuccessState,
    onAction: (RegisterSuccessAction) -> Unit,
) {
    ChirpAdaptiveResultLayout {
        ChirpSimpleSuccessLayout(
            title = stringResource(Res.string.account_successfully_created),
            description = stringResource(
                Res.string.verification_email_sent_to_x,
                state.registeredEmail,
            ),
            icon = {
                ChirpSuccessIcon()
            },
            primaryButton = {
                ChirpButton(
                    text = stringResource(Res.string.login),
                    onClick = {
                        onAction(RegisterSuccessAction.OnLoginClick)
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            },
            secondaryButton = {
                ChirpButton(
                    text = stringResource(Res.string.resend_verification_email),
                    onClick = {
                        onAction(RegisterSuccessAction.OnResendVerificationEmailClick)
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    enabled = !state.isResendingVerificationEmail,
                    isLoading = state.isResendingVerificationEmail,
                    style = ChirpButtonStyle.SECONDARY,
                )
            },
        )
    }
}

@Preview
@Composable
private fun Preview() {
    ChirpTheme {
        RegisterSuccessScreen(
            state = RegisterSuccessState(
                registeredEmail = "test@preview.com",
            ),
            onAction = {},
        )
    }
}
