package com.project.chirp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.project.auth.presentation.navigation.AuthGraphRoutes
import com.project.chat.presentation.navigation.ChatGraphRoutes
import com.project.chirp.main.MainEvent
import com.project.chirp.main.MainViewModel
import com.project.chirp.navigation.DeepLinkListener
import com.project.chirp.navigation.NavigationRoot
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.presentation.util.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onAuthenticationChecked: () -> Unit = {},
    onDeepLinkListenerSetup: () -> Unit = {},
    viewModel: MainViewModel = koinViewModel(),
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isCheckingAuth) {
        if (!state.isCheckingAuth) {
            onAuthenticationChecked()
        }
    }

    /**
     * The root composable wrapping the application UI and main navigation graph.
     * * ## Strategy / Decisions
     * - **Global Event Observation:** Listens to one-time events from `MainViewModel` using an `ObserveAsEvents` pattern
     * to handle system-wide side effects (like forced logouts) independent of the specific screen the user is currently on.
     * - **Backstack Clearing:** When navigating back to the authentication flow, the entire backstack must be obliterated
     * so the user cannot use the system back button to return to a secure area after their session expires.
     * * ## How It Works
     * 1. Collects events from `MainViewModel`'s event flow.
     * 2. When `MainEvent.OnSessionExpired` is received, it triggers the `NavController`.
     * 3. Navigates to `AuthGraphRoutes.Graph`.
     * 4. Executes a `popUpTo(AuthGraphRoutes.Graph) { inclusive = false }` to clear all previous destinations from the navigation backstack.
     */
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is MainEvent.OnSessionExpired -> {
                navController.navigate(AuthGraphRoutes.Graph) {
                    popUpTo(AuthGraphRoutes.Graph) {
                        inclusive = false
                    }
                }
            }
        }
    }

    ChirpTheme(
        darkTheme = isDarkTheme,
    ) {
        if (!state.isCheckingAuth) {
            NavigationRoot(
                navController = navController,
                startDestination = when {
                    !state.isLoggedIn -> AuthGraphRoutes.Graph
                    // Chat is gated by CHAT_ENABLED. When off, the consuming project wires its own
                    // post-login destination here (there is intentionally no placeholder screen).
                    BuildKonfig.CHAT_ENABLED -> ChatGraphRoutes.Graph
                    else -> TODO("Wire your app's post-login start destination")
                },
            )
            DeepLinkListener(navController, onDeepLinkListenerSetup)
        }
    }
}
