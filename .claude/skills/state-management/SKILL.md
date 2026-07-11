---
name: state-management
description: Use when creating a screen's ViewModel, modeling UI state, handling user actions, or emitting one-time events (navigation, snackbars) in the Chirp KMP app — covers the State/Action/Event/ViewModel/Root-Screen pattern, StateFlow exposure, and Channel-based events.
---

# State management (Chirp KMP)

## Convention

Each screen is a small MVI-style bundle in `…presentation/ui/<screen>/`:

| File | Type | Role |
|------|------|------|
| `<Screen>State.kt` | `data class` | All UI state, immutable, sensible defaults |
| `<Screen>Action.kt` | `sealed interface` | Every user intent (`OnLoginClick`, `OnTogglePasswordVisibility`) |
| `<Screen>Event.kt` | `sealed interface` | One-time effects (`Success`) — navigation / snackbars |
| `<Screen>ViewModel.kt` | `ViewModel` | Holds state, handles actions, emits events |
| `<Screen>Screen.kt` | `@Composable` | `…Root` (stateful) + stateless `…Screen` |

**ViewModel rules** (`feature/auth/.../ui/login/LoginViewModel.kt`):
- Private `_state = MutableStateFlow(State())`; public `state` is `_state.onStart { loadOnce() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), State())`. Guard one-time init with a `hasLoadedInitialData` flag.
- Mutate with `_state.update { it.copy(...) }` — never reassign.
- Single entry point `fun onAction(action: Action)` with an exhaustive `when`.
- One-time events: `private val eventChannel = Channel<Event>()` + `val events = eventChannel.receiveAsFlow()`; emit with `eventChannel.send(...)`.
- Derive flags reactively: `snapshotFlow { state.value.…TextFieldState.text }` + `combine(...)` + `launchIn(viewModelScope)` (see `canLogin`).
- ViewModels are registered via `viewModelOf(::LoginViewModel)` in the feature's `…presentation/di` module.

**State models loading/error/success as plain fields**, not a single sealed `UiState`: e.g. `isLoggingIn: Boolean`, `canLogin: Boolean`, `error: UiText? = null`. Errors are `UiText` (resolved in the UI), never raw strings.

**Root/Screen split** (`LoginScreen.kt`): the `…Root` collects state with `collectAsStateWithLifecycle()`, wires events via `ObserveAsEvents(viewModel.events)`, obtains the VM with `koinViewModel()`, and calls the stateless `…Screen(state, onAction)`. The stateless `…Screen` is what `@Preview`s target.

## Example

```kotlin
// LoginState.kt
data class LoginState(
    val emailTextFieldState: TextFieldState = TextFieldState(),
    val passwordTextFieldState: TextFieldState = TextFieldState(),
    val isPasswordVisible: Boolean = false,
    val canLogin: Boolean = false,
    val isLoggingIn: Boolean = false,
    val error: UiText? = null,
)

// LoginViewModel.kt (essentials)
private val _state = MutableStateFlow(LoginState())
val state = _state
    .onStart { if (!hasLoadedInitialData) { observeTextStates(); hasLoadedInitialData = true } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), LoginState())

private val eventChannel = Channel<LoginEvent>()
val events = eventChannel.receiveAsFlow()

fun onAction(action: LoginAction) = when (action) {
    LoginAction.OnLoginClick -> login()
    LoginAction.OnTogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    else -> Unit
}

private fun login() = viewModelScope.launch {
    _state.update { it.copy(isLoggingIn = true) }
    authService.login(email, password)
        .onSuccess { eventChannel.send(LoginEvent.Success) }                 // one-time → Channel
        .onFailure { error -> _state.update { it.copy(error = error.toUiText(), isLoggingIn = false) } } // durable → state
}
```

```kotlin
// LoginScreen.kt
@Composable
fun LoginRoot(viewModel: LoginViewModel = koinViewModel(), onLoginSuccess: () -> Unit, …) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event -> when (event) { LoginEvent.Success -> onLoginSuccess() } }
    LoginScreen(state = state, onAction = { … ; viewModel.onAction(it) })
}
```

## What to avoid

- ❌ Exposing the `MutableStateFlow`. Expose a read-only `StateFlow` via `stateIn`.
- ❌ Putting one-time effects (navigation, snackbar) in `State` — they re-fire on rotation/return. Use `Channel` + `ObserveAsEvents`.
- ❌ `collectAsState()` for events. Events use `ObserveAsEvents` (lifecycle-aware, `Main.immediate`); state uses `collectAsStateWithLifecycle()`.
- ❌ Resolving string resources in the ViewModel. Store `UiText` and call `asString()` in the composable (localization-safe).
- ❌ Reassigning `_state.value = …` for partial updates. Use `_state.update { it.copy(...) }`.
- ❌ Business logic in the stateless `…Screen`. It only renders `state` and forwards `onAction`.
- ❌ Leftover debug `println` in `onAction` / `Root` (there are a couple in the repo) — don't copy them into new screens.
