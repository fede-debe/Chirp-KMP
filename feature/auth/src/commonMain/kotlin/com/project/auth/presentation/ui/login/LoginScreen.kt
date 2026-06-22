package com.project.auth.presentation.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.auth.presentation.Res
import com.project.auth.presentation.create_account
import com.project.auth.presentation.email
import com.project.auth.presentation.email_placeholder
import com.project.auth.presentation.forgot_password
import com.project.auth.presentation.login
import com.project.auth.presentation.password
import com.project.auth.presentation.welcome_back
import com.project.core.designsystem.components.brand.ChirpBrandLogo
import com.project.core.designsystem.components.buttons.ChirpButton
import com.project.core.designsystem.components.buttons.ChirpButtonStyle
import com.project.core.designsystem.components.layouts.ChirpAdaptiveFormLayout
import com.project.core.designsystem.components.layouts.ChirpSnackbarScaffold
import com.project.core.designsystem.components.textFields.ChirpPasswordTextField
import com.project.core.designsystem.components.textFields.ChirpTextField
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.presentation.util.ObserveAsEvents
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoot(
    viewModel: LoginViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onEmailNotVerified: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.Success -> onLoginSuccess()
            is LoginEvent.EmailNotVerified -> onEmailNotVerified(event.email)
        }
    }

    LoginScreen(
        state = state,
        onAction = { action ->
            println("[LoginDebug] UI is holding EmailTextFieldState instance: ${state.emailTextFieldState.hashCode()}")
            when (action) {
                LoginAction.OnForgotPasswordClick -> onForgotPasswordClick()
                LoginAction.OnSignUpClick -> onCreateAccountClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
) {
    ChirpSnackbarScaffold {
        ChirpAdaptiveFormLayout(
            headerText = stringResource(Res.string.welcome_back),
            errorText = state.error?.asString(),
            logo = {
                ChirpBrandLogo()
            },
            modifier = Modifier
                .fillMaxSize(),
        ) {
            ChirpTextField(
                state = state.emailTextFieldState,
                placeholder = stringResource(Res.string.email_placeholder),
                keyboardType = KeyboardType.Email,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(),
                title = stringResource(Res.string.email),
            )
            Spacer(modifier = Modifier.height(16.dp))
            ChirpPasswordTextField(
                state = state.passwordTextFieldState,
                placeholder = stringResource(Res.string.password),
                isPasswordVisible = state.isPasswordVisible,
                onToggleVisibilityClick = {
                    onAction(LoginAction.OnTogglePasswordVisibility)
                },
                title = stringResource(Res.string.password),
                modifier = Modifier
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.forgot_password),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable {
                        onAction(LoginAction.OnForgotPasswordClick)
                    },
            )
            Spacer(modifier = Modifier.height(24.dp))

            ChirpButton(
                text = stringResource(Res.string.login),
                onClick = {
                    onAction(LoginAction.OnLoginClick)
                },
                enabled = state.canLogin,
                isLoading = state.isLoggingIn,
                modifier = Modifier
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChirpButton(
                text = stringResource(Res.string.create_account),
                onClick = {
                    onAction(LoginAction.OnSignUpClick)
                },
                style = ChirpButtonStyle.SECONDARY,
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun LightThemePreview() {
    ChirpTheme {
        LoginScreen(
            state = LoginState(),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun DarkThemePreview() {
    ChirpTheme(darkTheme = true) {
        LoginScreen(
            state = LoginState(),
            onAction = {},
        )
    }
}
