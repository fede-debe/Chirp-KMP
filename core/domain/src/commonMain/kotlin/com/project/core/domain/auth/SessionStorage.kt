package com.project.core.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for persisting and observing the user's active session data.
 *
 * ## Strategy / Decisions
 * This interface is placed in the `core domain` layer to ensure the rest of the app
 * (and the domain itself) is not coupled to the underlying persistence library (e.g., DataStore).
 * By using an interface, the session state can be accessed globally across the app without
 * leaking implementation details.
 *
 * ## How It Works
 * 1. Exposes a stream (`Flow`) of the current session state.
 * 2. Emits a valid `AuthInfo` when a session is active, or `null` when a session expires,
 * fails to refresh, or the user intentionally logs out.
 * 3. Provides a suspending function to update or clear the active session.
 *
 * ## Alternatives / Why Not
 * * **Why not a local database (Room/SQLDelight)?** The instructor explicitly noted that databases
 * are meant for multiple elements of one type. Since a session is a single, unique instance
 * of data at any given time, a key-value preference approach is more suited.
 *
 * Technical Details:
 * * Relies on `kotlinx.coroutines.flow.Flow` for observable data structures, allowing central
 * navigation controllers to immediately react to session expiration (e.g., 401 from API)
 * and boot the user to the login screen.
 */
interface SessionStorage {
    fun observeAuthInfo(): Flow<AuthInfo?>
    suspend fun set(info: AuthInfo?)
}
