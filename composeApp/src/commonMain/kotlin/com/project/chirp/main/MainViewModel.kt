package com.project.chirp.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.core.domain.auth.SessionStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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

/**
 * Root-level ViewModel responsible for monitoring global application state, specifically the session lifecycle.
 * * ## Strategy / Decisions
 * - **State vs. Events:** Global session expiration requires a one-time navigation action (kicking the user out to the login screen).
 * This is modeled as a one-time event sent through a Kotlin `Channel` exposed as a Flow, ensuring the navigation trigger
 * is consumed exactly once by the UI.
 * - **Expiration Detection Logic:** A session is only considered "expired" if it transitions from a valid state to a null state.
 * We must track the *previous* token state to differentiate between a true expiration and a fresh launch where the user simply isn't logged in.
 * * ## How It Works
 * 1. Upon initialization (specifically when initial data is loaded via `onStart`), `observeSession()` is called.
 * 2. It observes the `authInfo` flow from `SessionStorage`.
 * 3. On every emission, it updates a `currentRefreshToken` variable.
 * 4. It compares `previousRefreshToken` (must not be null) against `currentRefreshToken` (must be null).
 * 5. If this condition is met, the session has expired. It resets the session storage, updates the `isLoggedIn` state to false,
 * and sends a `MainEvent.OnSessionExpired` event to the root UI.
 * 6. Finally, it updates `previousRefreshToken` for the next emission cycle.
 * * ## Alternatives / Why Not
 * - **Triggering navigation purely on a `null` emission:** Rejected. If the app is launched and the user is not logged in,
 * the immediate emission is `null`. Triggering an expiration flow on *any* null value would cause an erroneous navigation transition on startup.
 * * Technical Details:
 * - Employs a private `Channel<MainEvent>` exposed as a Flow via `receiveAsFlow()`.
 * - Requires caching state (`previousRefreshToken`) across asynchronous Flow emissions to compute transitions.
 */
class MainViewModel(
    private val sessionStorage: SessionStorage,
) : ViewModel() {

    private val eventChannel = Channel<MainEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(MainState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeSession()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = MainState(),
        )

    private var previousRefreshToken: String? = null

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

    private fun observeSession() {
        sessionStorage
            .observeAuthInfo()
            .onEach { authInfo ->
                val currentRefreshToken = authInfo?.refreshToken
                val isSessionExpired = previousRefreshToken != null && currentRefreshToken == null
                if (isSessionExpired) {
                    sessionStorage.set(null)
                    _state.update {
                        it.copy(
                            isLoggedIn = false,
                        )
                    }
                    eventChannel.send(MainEvent.OnSessionExpired)
                }

                previousRefreshToken = currentRefreshToken
            }
            .launchIn(viewModelScope)
    }
}
