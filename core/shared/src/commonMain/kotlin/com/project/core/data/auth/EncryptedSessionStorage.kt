package com.project.core.data.auth

import com.project.core.data.dto.AuthInfoSerializable
import com.project.core.data.mappers.toDomain
import com.project.core.data.mappers.toSerializable
import com.project.core.data.security.SecureStorage
import com.project.core.domain.auth.AuthInfo
import com.project.core.domain.auth.SessionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * [SessionStorage] backed by the platform's secure store via [SecureStorage].
 *
 * ## Strategy / Decisions
 * - **Encryption without changing the contract:** Replaces the plaintext DataStore implementation
 *   ([DataStoreSessionStorage]) on mobile while keeping the exact same `Flow<AuthInfo?>` interface, so
 *   every consumer (token refresh, auto-logout, websocket, repositories) is untouched.
 * - **Reactivity via an in-memory [MutableStateFlow]:** Secure-store reads are synchronous, so the
 *   current session is loaded once at construction and held in a `StateFlow`. Reads (`first()`,
 *   `firstOrNull()`) therefore return the persisted value immediately — no async seeding race.
 *
 * ## How It Works
 * 1. On construction, the encrypted `AuthInfo` JSON is read from [SecureStorage] and decoded into the
 *    `StateFlow`'s initial value (or `null` if absent/corrupt).
 * 2. `observeAuthInfo()` exposes that `StateFlow`.
 * 3. `set(info)` writes the encoded JSON to [SecureStorage] (or removes it when `null`) and updates the
 *    `StateFlow` so observers react instantly.
 *
 * ## Error Handling
 * If a stored blob can't be decrypted/decoded (e.g. a rotated key or corrupted data), it's treated as
 * "no session": the bad entry is cleared and `null` is emitted, sending the user to login rather than
 * crashing.
 */
class EncryptedSessionStorage(
    private val secureStorage: SecureStorage,
) : SessionStorage {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val authInfoState = MutableStateFlow(loadAuthInfo())

    override fun observeAuthInfo(): Flow<AuthInfo?> = authInfoState.asStateFlow()

    override suspend fun set(info: AuthInfo?) {
        if (info == null) {
            secureStorage.remove(KEY_AUTH_INFO)
        } else {
            val serialized = json.encodeToString(info.toSerializable())
            secureStorage.putString(KEY_AUTH_INFO, serialized)
        }
        authInfoState.value = info
    }

    private fun loadAuthInfo(): AuthInfo? {
        return try {
            secureStorage.getString(KEY_AUTH_INFO)?.let { serialized ->
                json.decodeFromString<AuthInfoSerializable>(serialized).toDomain()
            }
        } catch (e: Exception) {
            secureStorage.remove(KEY_AUTH_INFO)
            null
        }
    }

    private companion object {
        const val KEY_AUTH_INFO = "KEY_AUTH_INFO"
    }
}
