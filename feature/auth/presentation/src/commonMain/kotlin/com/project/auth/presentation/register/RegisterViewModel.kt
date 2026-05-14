package com.project.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * Acts as the brain of the Register UI, receiving user actions and mutating the screen's state.
 *
 * ## Strategy / Decisions
 * - **Reactive State Management:** Uses Kotlin `StateFlow` instead of raw Compose `State` to hold the UI state. This enables reactive programming, making it easier to automatically chain operations (e.g., listening to text changes to validate an email state automatically via Flow operators).
 * - **Lazy Initialization:** Utilizes the `onStart` flow operator to trigger initial data loading rather than the `init` block. This decouples object instantiation from side effects, allowing strict control from the outside over when data is collected.
 *
 * ## How It Works
 * 1. Exposes an `onAction` function that accepts `RegisterAction` events.
 * 2. Evaluates the incoming action via a `when` expression.
 * 3. Updates the relevant properties in the internal StateFlow based on the exact action (e.g., updating the password state if the action was `EnterPassword`).
 * 4. When the UI begins collecting the Flow, the `onStart` operator checks if initial data is needed and loads it.
 *
 * ## Alternatives / Why Not
 * - **Compose State in ViewModel:** Rejected in favor of `StateFlow`. While Compose State successfully notifies the UI of changes, it lacks the rich, reactive stream operators of Kotlin Flows which are beneficial for reactive form validations.
 * - **Loading Data in `init` block:** Rejected because placing initial API calls or data loading inside the `init` block triggers side-effects immediately upon creation. This makes testing highly difficult, as creating a ViewModel reference for an isolated test would unnecessarily trigger initialization logic without providing a way to bypass it.
 *
 * ## Technical Details
 * - Flow emissions must be converted back to Compose State in the UI layer using `collectAsStateWithLifecycle()`.
 */
class RegisterViewModel : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(RegisterState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                // TODO: Load initial data here

                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = RegisterState(),
        )

    fun onAction(action: RegisterAction) {
        when (action) {
            else -> Unit
        }
    }
}
