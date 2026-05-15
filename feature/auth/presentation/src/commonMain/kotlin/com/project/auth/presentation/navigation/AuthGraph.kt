package com.project.auth.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.project.auth.presentation.register.RegisterRoot
import com.project.auth.presentation.registerSuccess.RegisterSuccessRoot

/**
 * Extension function on `NavGraphBuilder` that constructs the nested navigation graph for the Authentication feature.
 *
 * ## Strategy / Decisions
 * - **Feature Isolation (Zero-Knowledge Features):** A core architectural decision is that features must never navigate directly to other features. If the login screen hardcoded a transition to the "Chat" feature, the Auth module would become tightly coupled to Chat. Instead, inter-feature navigation is bubbled up to the app-level module.
 * - **Intra-Feature Navigation:** Navigation *within* the feature (e.g., Register -> Register Success) is perfectly fine and is handled locally by the feature's graph.
 *
 * ## How It Works
 * 1. Invokes the `navigation` builder, passing the type-safe `AuthGraphRoutes.Graph` as the route.
 * 2. Defines the `startDestination` (currently set to `Register` for layout testing purposes).
 * 3. Registers `composable` blocks for each route.
 * 4. Within the `Register` composable, listens for the success callback and uses the local `navController` to transition to the `RegisterSuccess` route, passing the required email argument.
 *
 * ## Alternatives / Why Not
 * - **String-based overload:** The `navigation` builder overload taking a `String` start destination was explicitly rejected in favor of the `KClass` overload to maintain full type safety across the graph.
 *
 * Technical Details:
 * Operates strictly within the scoped context of a `NavHost`.
 *
 * @param navController Used to perform internal transitions between auth screens.
 * @param onLoginSuccess Lambda triggered to bubble up the inter-feature navigation event to the App module once authentication is finalized.
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    onLoginSuccess: () -> Unit,
) {
    navigation<AuthGraphRoutes.Graph>(
        startDestination = AuthGraphRoutes.Register,
    ) {
        composable<AuthGraphRoutes.Register> {
            RegisterRoot(
                onRegisterSuccess = {
                    navController.navigate(AuthGraphRoutes.RegisterSuccess(it))
                },
            )
        }
        composable<AuthGraphRoutes.RegisterSuccess> {
            RegisterSuccessRoot()
        }
    }
}
