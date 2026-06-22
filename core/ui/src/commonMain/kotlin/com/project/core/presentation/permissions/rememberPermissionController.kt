@file:Suppress("ktlint:standard:filename", "filename")

package com.project.core.presentation.permissions

import androidx.compose.runtime.Composable

/**
 * Composable factory function to instantiate and remember the [PermissionController].
 *
 * ## Strategy / Decisions
 * Moco requires a composable context to properly attach and manage the native side effects associated with rendering system dialogs over the UI.
 *
 * ## How It Works
 * 1. Defines an `expect` composable function in `commonMain`.
 * 2. In `mobileMain` (the `actual` implementation), creates a Moco `PermissionsControllerFactory`.
 * 3. Instantiates the Moco controller via the factory.
 * 4. Wraps the Moco controller inside Moco's `BindEffect` composable to attach the native OS lifecycle hooks to the current Compose tree.
 * 5. Injects the configured Moco controller into our custom domain `PermissionController` wrapper.
 */
@Composable
expect fun rememberPermissionController(): PermissionController
