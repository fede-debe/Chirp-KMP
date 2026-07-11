---
name: error-handling
description: Use when handling an API/operation failure, mapping an error to a user-facing message, adding a screen-specific error case (e.g. invalid credentials, email not verified), or surfacing errors to the UI in the Chirp KMP app — covers the Result/DataError types, the three-tier error→UiText mapping, and how errors reach the UI.
---

# Error handling (Chirp KMP)

## Convention

Errors are **values, not exceptions**. Operations return `Result<D, E : Error>` (`core/shared/.../domain/util/Result.kt`):

```kotlin
sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Failure<out E : Error>(val error: E) : Result<Nothing, E>
}
// + inline map / onSuccess / onFailure / asEmptyResult ; typealias EmptyResult<E> = Result<Unit, E>
```

`Error` is a marker interface; the concrete error set is the sealed `DataError` with `Remote`, `Local`, and `Connection` enums (`UNAUTHORIZED`, `NO_INTERNET`, `DISK_FULL`, `MESSAGE_SEND_FAILED`, …). `DataErrorException` exists only to push a `DataError` through throwing channels (e.g. the paginator).

Errors map to UI text in **three tiers**:

1. **HTTP status → `DataError.Remote`** — `responseToResult()` in `core/shared/.../data/networking/HttpClientExt.kt` (`401 → UNAUTHORIZED`, `403 → FORBIDDEN`, `409 → CONFLICT`, …). Platform network exceptions become `NO_INTERNET` etc. in `platformSafeCall`.
2. **`DataError` → `UiText` (generic)** — `DataError.toUiText()` in `core/ui/.../presentation/util/DataErrorToUiText.kt` maps every enum case to a localized string resource. This is the default for any failure.
3. **Screen-specific overrides** — in the ViewModel's `onFailure`, branch on the specific error *before* falling back to `toUiText()`. This is where context-dependent copy lives (the same `UNAUTHORIZED` means "invalid credentials" on login).

> Note: the server does **not** send string error codes like `EMAIL_NOT_VERIFIED`. Those names are **local** string resources (`error_email_not_verified`, `error_invalid_credentials`) chosen in the ViewModel from the HTTP-derived `DataError`. Map by `DataError` case, not by a server code field.

Errors reach the UI as **`UiText`** (never raw strings — keeps localization reactive). Durable errors go into `state.error: UiText?` (rendered inline, e.g. a form's `errorText`); transient ones go through the event `Channel` (snackbar) — see the state-management skill.

## Example

Screen-specific mapping then generic fallback (`feature/auth/.../ui/login/LoginViewModel.kt`):

```kotlin
authService.login(email, password)
    .onSuccess { authInfo ->
        sessionStorage.set(authInfo)
        eventChannel.send(LoginEvent.Success)
    }
    .onFailure { error ->
        val errorMessage = when (error) {
            DataError.Remote.UNAUTHORIZED -> UiText.Resource(Res.string.error_invalid_credentials) // override
            DataError.Remote.FORBIDDEN    -> UiText.Resource(Res.string.error_email_not_verified)  // override
            else                          -> error.toUiText()                                       // generic tier-2
        }
        _state.update { it.copy(error = errorMessage, isLoggingIn = false) }                        // into state
    }
```

Generic mapping (`DataErrorToUiText.kt`, tier 2):

```kotlin
fun DataError.toUiText(): UiText = UiText.Resource(when (this) {
    DataError.Remote.UNAUTHORIZED            -> Res.string.error_unauthorized
    DataError.Remote.NO_INTERNET             -> Res.string.error_no_internet
    DataError.Connection.MESSAGE_SEND_FAILED -> Res.string.error_unable_to_send_message
    …
})
```

Rendering (`LoginScreen.kt`): `errorText = state.error?.asString()` — `UiText` is resolved to a `String` only inside the composable.

## What to avoid

- ❌ Throwing exceptions across layers. Return `Result.Failure(DataError.…)`; use `onSuccess`/`onFailure`/`map`. (`DataErrorException` is only for the paginator's throwing path.)
- ❌ Surfacing a raw `DataError` (or HTTP status / Ktor exception) to the UI. Convert to `UiText` first.
- ❌ Resolving a string resource inside the ViewModel. Store `UiText` and call `asString()` in the composable so language changes apply without a relaunch.
- ❌ Inventing a server "error code" string field. Branch on the `DataError` enum produced from the HTTP status.
- ❌ Hand-rolling status-code or try/catch handling in a service. `responseToResult` + `platformSafeCall` already produce a `DataError.Remote`.
- ❌ Putting a one-shot error toast in `State` (re-fires on rotation). Use the event `Channel` for transient errors; use `state.error` only for persistent inline messages.
