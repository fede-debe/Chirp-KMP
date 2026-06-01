package com.project.chirp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.project.auth.presentation.navigation.AuthGraphRoutes
import com.project.auth.presentation.navigation.authGraph
import com.project.chat.presentation.navigation.ChatGraphRoutes
import com.project.chat.presentation.navigation.chatGraph

/**
 * The root composable that hosts the application's primary `NavHost` and wires independent feature modules together.
 *
 * ## Strategy / Decisions
 * - **Centralized Application Wiring:** The app module acts as the sole orchestrator. It imports independent feature graphs (like Auth and Chat) and defines how they transition between each other, maintaining modular purity.
 * - **Graph-Level Backstack Management:** By bundling the authentication screens into a distinct `NavGraph`, the entire graph functions as a single entry on the backstack. When a user successfully logs in, the app can pop the *entire* graph at once. This ensures no orphaned auth screens are left on the backstack, preventing the user from accidentally navigating back to a login screen after authenticating.
 *
 * ## How It Works
 * 1. Instantiates the root `NavController` via `rememberNavController()`.
 * 2. Sets up the `NavHost`, using the `AuthGraphRoutes.Graph` as the entry point start destination.
 * 3. Calls the `authGraph` builder extension to mount the authentication screens.
 * 4. (Pending) Will implement the `onLoginSuccess` callback to pop the auth graph and transition to the chat graph.
 */
@Composable
fun NavigationRoot(
    navController: NavHostController,
    startDestination: Any,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        authGraph(
            navController = navController,
            onLoginSuccess = {
                navController.navigate(ChatGraphRoutes.Graph) {
                    popUpTo(AuthGraphRoutes.Graph) {
                        inclusive = true
                    }
                }
            },
        )
        chatGraph(
            navController = navController,
        )
    }
}
