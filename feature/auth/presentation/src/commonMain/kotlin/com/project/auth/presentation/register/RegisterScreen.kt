package com.project.auth.presentation.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.core.designsystem.theme.ChirpTheme

/**
 * Renders the Registration UI by composing design system components and wires them to the ViewModel and State.
 *
 * ## Strategy / Decisions
 * - **Two-Composable Pattern:** The screen is split into a "Root" composable and a "Stateless" composable (`RegisterScreen`). This completely separates dependency resolution from UI rendering, ensuring the UI remains perfectly previewable and modular.
 *
 * ## How It Works
 * 1. `RegisterScreenRoot` acts as the entry point used by the navigation graph and securely holds the ViewModel reference.
 * 2. `RegisterScreenRoot` collects the `StateFlow` from the ViewModel as Compose State using `collectAsStateWithLifecycle()`.
 * 3. `RegisterScreenRoot` passes the raw `RegisterState` and an `onAction` lambda down to `RegisterScreen`.
 * 4. `RegisterScreen` uses this raw state to construct the UI elements (reused from the generic design system) and invokes the `onAction` lambda when the user performs an interaction.
 *
 * ## Alternatives / Why Not
 * - **Passing ViewModel Directly to UI:** Rejected. If the main screen composable requires a ViewModel reference, standard Compose Previews will break. Previews run in an isolated display container that cannot construct complex ViewModel instances, especially when relying on Dependency Injection (like Koin) which mandates ViewModel Factories.
 *
 * ## Technical Details
 * - Guarantees that Compose Previews work smoothly since the preview only needs to instantiate a simple `RegisterState` data class mock rather than an active, fully-featured ViewModel.
 */
@Composable
fun RegisterRoot(
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = viewModel(), // Swap with koinViewModel() or hiltViewModel() if needed
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RegisterScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: Build your screen UI here
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    // Note: You may need to manually import your project's specific Theme here
    ChirpTheme {
        RegisterScreen(
            state = RegisterState(),
            onAction = {},
        )
    }
}
