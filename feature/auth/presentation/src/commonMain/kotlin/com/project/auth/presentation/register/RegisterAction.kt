package com.project.auth.presentation.register

/**
 * Bundles all distinct actions or events that a user can trigger on the Register Screen.
 *
 * ## Strategy / Decisions
 * - **MVI Architecture:** Encapsulates UI events (intents) into a sealed interface. This provides an exhaustive, type-safe way to represent user intentions and thoroughly decouples the UI layout from the business logic.
 *
 * ## How It Works
 * 1. The user interacts with the UI (e.g., typing text, clicking submit, requesting a verification email).
 * 2. The UI wraps this specific interaction in a corresponding `RegisterAction` class/object.
 * 3. The action is dispatched to the ViewModel's `onAction` function for processing.
 *
 * ## Technical Details
 * - Implemented as a sealed interface to guarantee exhaustive `when` statements when handled by the ViewModel.
 */
sealed interface RegisterAction {
    data object OnButtonAClick : RegisterAction
    data class OnTypeInTextFieldB(val text: String) : RegisterAction
}
