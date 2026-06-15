package com.project.chirp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavUri

/**
 * A side effect Composable that binds the navigation controller to the external URI handler.
 *
 * ## Strategy / Decisions
 * We use a `DisposableEffect` with a constant key (`Unit`) to bridge our non-composable singleton (`ExternalUriHandler`) with the composable `NavController`.
 * This ensures our listener is safely attached when the app enters the composition and cleanly detached when it leaves, preventing memory leaks.
 *
 * ## How It Works
 * 1. Upon entering the composition, assigns a lambda to `ExternalUriHandler.listener`.
 * 2. When the lambda is invoked (receiving a URI string), it calls `navController.navigate(uri)`.
 * 3. `navController` resolves the string against the `navDeepLink` rules in `AuthGraph`.
 * 4. `onDispose` resets the listener to `null` if the app is closed.
 *
 * ## Alternatives / Why Not
 * A standard `LaunchedEffect` was rejected because we need explicit cleanup (`onDispose`) to unregister the listener callback when the composable tree is destroyed.
 *
 * ## Technical Details
 * - The key is `Unit` so the effect only runs once on mount, rather than recomposing.
 */

@Composable
fun DeepLinkListener(
    navController: NavController,
    onSetup: () -> Unit,
) {
    DisposableEffect(Unit) {
        ExternalUriHandler.listener = { uri ->
            navController.navigate(NavUri(uri))
        }

        onSetup()

        onDispose {
            ExternalUriHandler.listener = null
        }
    }
}
