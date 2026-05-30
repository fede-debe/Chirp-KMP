package com.project.chirp

/**
 * Represents the global authentication state during application launch.
 *
 * ## Strategy / Decisions
 * This state forces the UI to wait before making routing decisions. By explicitly tracking
 * the loading state (`isCheckingAuth`), the app prevents premature rendering of the Auth
 * graph for users who are already logged in.
 *
 * ## How It Works
 * 1. `isCheckingAuth` defaults to `true` when the app opens.
 * 2. `isLoggedIn` defaults to `false`.
 * 3. Once the storage check resolves, `isCheckingAuth` flips to `false` and `isLoggedIn` reflects the token's presence.
 */
data class MainState(
    val isLoggedIn: Boolean = false,
    val isCheckingAuth: Boolean = true,
)
