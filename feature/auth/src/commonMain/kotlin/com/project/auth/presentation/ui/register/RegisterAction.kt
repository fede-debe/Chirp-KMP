package com.project.auth.presentation.ui.register

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

/**
 * Represents all possible user interactions on the Register Screen to be processed by the ViewModel.
 *
 * ## Strategy / Decisions
 * - **UI-Centric Event Naming:** Actions are named strictly based on what happened in the UI (e.g., `OnInputTextFocusGain`, `OnLoginClick`) rather than what the ViewModel is expected to do as a result (e.g., `ClearErrors`, `LoginUser`). This ensures the UI remains decoupled and entirely unaware of business logic. If the ViewModel's response to an event changes in the future, the action's name remains accurate and untouched.
 *
 * ## How It Works
 * - Defines a sealed interface mapping directly to user touch points:
 *   1. Clicking the register button.
 *   2. Clicking the login redirection button.
 *   3. Toggling password visibility.
 *   4. Gaining focus on any text field (used to clear visible validation errors).
 *
 * ## Alternatives / Why Not
 * - **No Text Change Actions:** There are intentionally no actions for `OnUsernameChanged` or `OnEmailChanged`. Because the design system uses a stateful `TextFieldState` instance, the text can be mutated directly via the state reference in the UI, bypassing the need to send an action to the ViewModel for every single keystroke.
 */
sealed interface RegisterAction {
    data object OnLoginClick : RegisterAction
    data object OnInputTextFocusGain : RegisterAction
    data object OnRegisterClick : RegisterAction
    data object OnTogglePasswordVisibilityClick : RegisterAction
}
