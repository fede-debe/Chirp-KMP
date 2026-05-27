package com.project.core.designsystem.components.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A wrapper layout utilizing the Material Scaffold to display snackbars (e.g., for error messages) on specific screens.
 *
 * ## Strategy / Decisions
 * This component was designed as a centralized, reusable wrapper to encapsulate snackbar boilerplate.
 * By creating this wrapper, we avoid having to repeatedly recreate the Scaffold, SnackbarHost, and
 * complex WindowInsets logic across multiple screens that require snackbar capabilities.
 *
 * ## How It Works
 * 1. **Scaffold Setup:** Initializes a Material `Scaffold` to structure the layout.
 * 2. **Window Insets Calculation:** Customizes the `contentWindowInsets` by unioning three types of insets:
 * - `statusBars`: Accommodates the system toolbar/notification icons.
 * - `displayCutout`: Accounts for hardware like inbuilt cameras.
 * - `ime`: Ensures the layout reacts to the software keyboard, forcing snackbars to render above an open keyboard.
 * 3. **Snackbar Host Configuration:** Links the `SnackbarHost` to the provided state and applies a 24.dp bottom padding so the snackbar doesn't rest at the absolute bottom edge of the screen.
 * 4. **Content Wrapping:** Wraps the screen's `content` inside a `Box` to automatically apply the `innerPadding` calculated by the Scaffold, protecting the content from obscuring system UI elements.
 *
 * ## Technical Details
 * - Relies on `SnackbarHostState` to orchestrate when the snackbars are displayed.
 * - This file intentionally lacks a `@Preview` annotation as it is a structural layout wrapper without inherent visual styling.
 *
 * @param modifier The modifier to apply to the root scaffold layout.
 * @param snackbarHostState The state instance used to trigger and manage snackbar queues.
 * @param content The composable screen content to be wrapped by the scaffold.
 */
@Composable
fun ChirpSnackbarScaffold(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars
            .union(WindowInsets.displayCutout)
            .union(WindowInsets.ime),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding),
        ) {
            content()
        }
    }
}
