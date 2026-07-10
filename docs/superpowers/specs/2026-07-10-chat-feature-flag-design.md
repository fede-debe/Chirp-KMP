# Design: Chat feature flag (KMP)

**Date:** 2026-07-10
**Branch:** `feature/chat-feature-flag`
**Status:** Approved

## Purpose

This KMP app is being turned into a reusable **template** (and then repurposed as "Pet"). Chat is a
feature not every app needs. We want chat **dormant behind a single build-time flag** — kept in the
codebase for possible future use, but off by default — mirroring the backend, where chat is already
gated behind a `chatEnabled` flag.

This spec covers **only** introducing the flag and gating chat's navigation wiring. No renaming, no
Pet features, no changes to the chat feature's internals.

## Context

- Config mechanism in this repo is **BuildKonfig** — build-time constants sourced from
  `local.properties`, generated into a `BuildKonfig` object (existing example: `API_KEY`). Applied
  per-module via the `com.project.convention.buildkonfig` convention plugin.
- Chat is the **only** post-login feature; there is no `home` module. `feature/` = `auth`, `chat`.
- The auth feature is cleanly decoupled: `authGraph` bubbles login success up via an `onLoginSuccess`
  callback and never references chat. **All chat coupling lives in `composeApp/`.**

### The two coupling touchpoints (map)

1. `composeApp/.../navigation/NavigationRoot.kt`
   - **inbound:** always registers `chatGraph(...)`.
   - **outbound:** `authGraph`'s `onLoginSuccess` navigates to `ChatGraphRoutes.Graph`.
2. `composeApp/.../App.kt`
   - **outbound:** `startDestination = if (isLoggedIn) ChatGraphRoutes.Graph else AuthGraphRoutes.Graph`.

## Decision

**A single build-time flag `CHAT_ENABLED: Boolean`, via BuildKonfig, default `false`.** Chat on/off is
a per-project build decision, not a user-facing runtime toggle, so build-time is the correct altitude
and it reuses the existing `API_KEY` pattern. (A Koin runtime `FeatureFlags` object was rejected as
over-engineering — no runtime toggle is needed.)

**Post-login landing when chat is off: the developer wires their own** (no placeholder screen). The
template compiles and runs auth, but hitting a logged-in state with chat off lands on an explicit
`TODO(...)` extension point — a loud, clear "wire your destination here." This is intentional: the
template is not meant to run past login until the consuming project adds its first feature.

## Design

### The flag
- Add `CHAT_ENABLED` (Boolean) to BuildKonfig, sourced from `local.properties` with a **`false`**
  default when the property is absent (so a fresh clone doesn't fail the build — unlike `API_KEY`,
  which is required). Exposed via the existing `core/shared` `BuildKonfig` so `composeApp` can read it.

### Touchpoint 1 — `App.kt` (start destination)
```kotlin
startDestination = when {
    !state.isLoggedIn        -> AuthGraphRoutes.Graph
    BuildKonfig.CHAT_ENABLED -> ChatGraphRoutes.Graph
    else                     -> TODO("Wire your app's post-login start destination")
}
```

### Touchpoint 2 — `NavigationRoot.kt`
- `authGraph(onLoginSuccess = ...)` → navigate to `ChatGraphRoutes.Graph` when `CHAT_ENABLED`, else
  the same post-login `TODO(...)` extension point.
- `chatGraph(...)` is registered **only when `CHAT_ENABLED`** (inbound gating — chat routes do not
  exist when off).

### Dormant, not deleted
- The `chat` module and all its code are **untouched**.
- A short note at `chatGraph`'s entry point (and the flag definition): *"Gated by `CHAT_ENABLED`;
  dormant unless enabled. Retained for possible future use."*

## Acceptance

With `CHAT_ENABLED = false` (default):
- App compiles; the `chat` module still builds.
- Auth flow runs: launch → login/register screens work.
- Logged-in + chat off → the app reaches the `TODO(...)` extension point (clear signal to wire a
  destination), and **no chat graph/routes are registered**.

With `CHAT_ENABLED = true` (set in `local.properties`):
- App behaves exactly as today: login → chat.

## Out of scope

Renaming (`chirp` → project name), Pet features, a `home`/placeholder module, changes to the chat
feature's internal code, and the template snapshot itself (separate follow-up steps).
