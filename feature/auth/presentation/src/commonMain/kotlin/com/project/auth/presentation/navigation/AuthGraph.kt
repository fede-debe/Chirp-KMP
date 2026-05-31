package com.project.auth.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.project.auth.presentation.ui.emailVerification.EmailVerificationRoot
import com.project.auth.presentation.ui.forgotPassword.ForgotPasswordRoot
import com.project.auth.presentation.ui.login.LoginRoot
import com.project.auth.presentation.ui.register.RegisterRoot
import com.project.auth.presentation.ui.registerSuccess.RegisterSuccessRoot
import com.project.auth.presentation.ui.resetPassword.ResetPasswordRoot

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

/**
 * Defines the navigation graph and routes incoming deep link URIs to specific composable screens.
 *
 * ## Strategy / Decisions
 * Deep links are integrated directly into the Compose Navigation framework using `navDeepLink`.
 * This ensures that when a URI is triggered, the navigation controller automatically parses the path and arguments without requiring manual string manipulation.
 *
 * ## How It Works
 * 1. The `EmailVerificationScreenRoute` is assigned a list of `navDeepLink` configurations.
 * 2. Both `https` and `chirp` schemes are registered matching the exact API path (`/api/auth/verify`).
 * 3. The `{token}` parameter is appended to the URI pattern.
 * 4. When a URL matches, the navigation framework extracts the string in place of `{token}` and injects it into the `SavedStateHandle` for the `VerificationViewModel`.
 *
 * ## Alternatives / Why Not
 * A manual deep link parsing mechanism at the root `App.kt` level was avoided because Compose Navigation natively supports argument extraction and deep link routing, keeping state management clean.
 *
 * ## Technical Details
 * - The placeholder `{token}` in the deep link URI MUST exactly match the key defined as a navigation argument for retrieval in the ViewModel.
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    onLoginSuccess: () -> Unit,
) {
    navigation<AuthGraphRoutes.Graph>(
        startDestination = AuthGraphRoutes.Login,
    ) {
        composable<AuthGraphRoutes.Login> {
            LoginRoot(
                onLoginSuccess = onLoginSuccess,
                onForgotPasswordClick = {
                    navController.navigate(AuthGraphRoutes.ForgotPassword)
                },
                onCreateAccountClick = {
                    navController.navigate(AuthGraphRoutes.Register) {
                        restoreState = true
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<AuthGraphRoutes.Register> {
            RegisterRoot(
                onRegisterSuccess = {
                    navController.navigate(AuthGraphRoutes.RegisterSuccess(it))
                },
                onLoginClick = {
                    navController.navigate(AuthGraphRoutes.Login) {
                        /**
                         * State Preservation: Uses saveState = true and restoreState = true to reuse existing screens in the history.
                         * This prevents duplicate instances and keeps user inputs (like partially typed emails) intact.
                         *
                         * Stack Cleanup: Rejects the default Maps() behavior. Instead, it relies on launchSingleTop = true to ensure only one instance of a screen sits at the top,
                         * and uses popUpTo() to trim the back stack and maintain a clean navigation hierarchy.*/
                        popUpTo(AuthGraphRoutes.Register) {
                            inclusive = true
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        composable<AuthGraphRoutes.RegisterSuccess> {
            RegisterSuccessRoot(
                onLoginClick = {
                    navController.navigate(AuthGraphRoutes.Login) {
                        popUpTo<AuthGraphRoutes.RegisterSuccess> {
                            inclusive = true
                        }
                    }
                },
            )
        }

        composable<AuthGraphRoutes.EmailVerification>(
            deepLinks = listOf(
                navDeepLink {
                    this.uriPattern = "https://chirp.adamapp.dev/api/auth/verify?token={token}"
                },
                navDeepLink {
                    this.uriPattern = "chirp://chirp.adamapp.dev/api/auth/verify?token={token}"
                },
            ),
        ) {
            EmailVerificationRoot(
                onLoginClick = {
                    navController.navigate(AuthGraphRoutes.Login) {
                        popUpTo<AuthGraphRoutes.EmailVerification> {
                            inclusive = true
                        }
                    }
                },
                onCloseClick = {
                    navController.navigate(AuthGraphRoutes.Login) {
                        popUpTo<AuthGraphRoutes.EmailVerification> {
                            inclusive = true
                        }
                    }
                },
            )
        }
        composable<AuthGraphRoutes.ForgotPassword> {
            ForgotPasswordRoot()
        }
        composable<AuthGraphRoutes.ResetPassword>(
            deepLinks = listOf(
                navDeepLink {
                    this.uriPattern = "https://chirp.adamapp.dev/api/auth/reset-password?token={token}"
                },
                navDeepLink {
                    this.uriPattern = "chirp://chirp.adamapp.dev/api/auth/reset-password?token={token}"
                },
            ),
        ) {
            ResetPasswordRoot()
        }
    }
}
