package com.project.auth.presentation.ui.resetPassword

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.auth.presentation.Res
import com.project.auth.presentation.password
import com.project.auth.presentation.password_hint
import com.project.auth.presentation.reset_password_successfully
import com.project.auth.presentation.set_new_password
import com.project.auth.presentation.submit
import com.project.core.designsystem.components.brand.ChirpBrandLogo
import com.project.core.designsystem.components.buttons.ChirpButton
import com.project.core.designsystem.components.layouts.ChirpAdaptiveFormLayout
import com.project.core.designsystem.components.textFields.ChirpPasswordTextField
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.designsystem.theme.extended
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ResetPasswordRoot(
    viewModel: ResetPasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ResetPasswordScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordState,
    onAction: (ResetPasswordAction) -> Unit,
) {
    ChirpAdaptiveFormLayout(
        headerText = stringResource(Res.string.set_new_password),
        errorText = state.errorText?.asString(),
        logo = {
            ChirpBrandLogo()
        },
    ) {
        ChirpPasswordTextField(
            state = state.passwordTextState,
            modifier = Modifier
                .fillMaxWidth(),
            placeholder = stringResource(Res.string.password),
            title = stringResource(Res.string.password),
            supportingText = stringResource(Res.string.password_hint),
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibilityClick = {
                onAction(ResetPasswordAction.OnTogglePasswordVisibilityClick)
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        ChirpButton(
            text = stringResource(Res.string.submit),
            onClick = {
                onAction(ResetPasswordAction.OnSubmitClick)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.canSubmit,
            isLoading = state.isLoading,
        )
        if (state.isResetSuccessful) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.reset_password_successfully),
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
        ResetPasswordScreen(
            state = ResetPasswordState(),
            onAction = {},
        )
    }
}
