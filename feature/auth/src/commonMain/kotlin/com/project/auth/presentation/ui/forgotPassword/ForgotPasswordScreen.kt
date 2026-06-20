package com.project.auth.presentation.ui.forgotPassword

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.auth.presentation.Res
import com.project.auth.presentation.email
import com.project.auth.presentation.email_placeholder
import com.project.auth.presentation.forgot_password
import com.project.auth.presentation.forgot_password_email_sent_successfully
import com.project.auth.presentation.submit
import com.project.core.designsystem.components.brand.ChirpBrandLogo
import com.project.core.designsystem.components.buttons.ChirpButton
import com.project.core.designsystem.components.layouts.ChirpAdaptiveFormLayout
import com.project.core.designsystem.components.textFields.ChirpTextField
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.designsystem.theme.extended
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ForgotPasswordRoot(
    viewModel: ForgotPasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ForgotPasswordScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onAction: (ForgotPasswordAction) -> Unit,
) {
    ChirpAdaptiveFormLayout(
        headerText = stringResource(Res.string.forgot_password),
        errorText = state.errorText?.asString(),
        logo = {
            ChirpBrandLogo()
        },
    ) {
        ChirpTextField(
            state = state.emailTextFieldState,
            modifier = Modifier
                .fillMaxWidth(),
            placeholder = stringResource(Res.string.email_placeholder),
            title = stringResource(Res.string.email),
            isError = state.errorText != null,
            supportingText = state.errorText?.asString(),
            keyboardType = KeyboardType.Email,
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        ChirpButton(
            text = stringResource(Res.string.submit),
            onClick = {
                onAction(ForgotPasswordAction.OnSubmitClick)
            },
            modifier = Modifier
                .fillMaxWidth(),
            enabled = !state.isLoading && state.canSubmit,
            isLoading = state.isLoading,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (state.isEmailSentSuccessfully) {
            Text(
                text = stringResource(Res.string.forgot_password_email_sent_successfully),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.extended.success,
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ChirpTheme {
        ForgotPasswordScreen(
            state = ForgotPasswordState(),
            onAction = {},
        )
    }
}
