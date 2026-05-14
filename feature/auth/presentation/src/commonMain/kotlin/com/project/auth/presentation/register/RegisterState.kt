package com.project.auth.presentation.register

/**
 * Bundles all state variables for the Register Screen.
 *
 * ## Strategy / Decisions
 * - **MVI Architecture:** Adheres to the Model-View-Intent (MVI) pattern by consolidating all fields and values that dictate the screen's appearance into a single cohesive data class. This ensures a single source of truth for the UI state.
 *
 * ## How It Works
 * 1. Holds properties that represent the current state of the UI (e.g., loading indicators, text field inputs).
 * 2. Whenever a property changes in the ViewModel, a new instance of this data class is emitted to the UI.
 *
 * ## Technical Details
 * - Serves as an immutable state holder that is collected by the presentation layer.
 */
data class RegisterState(
    val isLoading: Boolean = false,
    // TODO: Add your screen-specific state variables here
)
