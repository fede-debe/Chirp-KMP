package com.project.chirp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.core.domain.auth.SessionStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Application-scoped ViewModel responsible for evaluating the initial authentication state
 * and handling global session events (like expired tokens).
 *
 * ## Strategy / Decisions
 * The ViewModel suspends initialization until it explicitly reads the first token emission
 * from storage. This guarantees the app knows exactly where to navigate before the UI draws.
 *
 * ## How It Works
 * 1. In the `init` block, a coroutine launches to observe the `SessionStorage`.
 * 2. It suspends using `firstOrNull()` to get the immediate initial state of the stored token.
 * 3. Based on the presence of the token, it updates `MainState` to set `isLoggedIn` and flips `isCheckingAuth` to false.
 * 4. (Future) Will continuously observe `SessionStorage` to log the user out if the session expires.
 *
 * ## Alternatives / Why Not
 * **Rejected:** Using a reactive `onEach` block directly on the flow property.
 * **Why:** While listening to the flow via `onEach` works for ongoing updates, using the `init` block with `firstOrNull()` is more logical for startup. It actively suspends the coroutine until the initial check is complete, ensuring a definitive state update before the app continues.
 *
 * ## Technical Details
 * - Must be registered in the Koin DI container (`AppModule`) via `viewModelOf(::MainViewModel)`.
 */
class MainViewModel(
    private val sessionStorage: SessionStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val authInfo = sessionStorage.observeAuthInfo().firstOrNull()
            _state.update {
                it.copy(
                    isCheckingAuth = false,
                    isLoggedIn = authInfo != null,
                )
            }
        }
    }
}
