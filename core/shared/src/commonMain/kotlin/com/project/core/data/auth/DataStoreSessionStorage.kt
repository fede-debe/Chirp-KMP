package com.project.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.project.core.data.dto.AuthInfoSerializable
import com.project.core.data.mappers.toDomain
import com.project.core.data.mappers.toSerializable
import com.project.core.domain.auth.AuthInfo
import com.project.core.domain.auth.SessionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Jetpack Preferences DataStore implementation of the [SessionStorage].
 *
 * ## Strategy / Decisions
 * Uses `Preferences DataStore` as the primary local persistence mechanism because we only need
 * to store a single instance of session data (access token, refresh token, user ID, etc.).
 * The underlying model is mapped to a serializable DTO (`AuthInfoSerializable`) and stored
 * as a JSON string to bypass DataStore's primitive-only limitations while avoiding the setup
 * overhead of Proto DataStore.
 *
 * ## How It Works
 * 1. **Initialization:** Takes a `DataStore<Preferences>` instance injected via constructor.
 * 2. **Setting Data:** * * If `null` is passed, it executes an `edit { it.remove(...) }` block to clear the storage.
 * * If a valid object is passed, it uses a custom JSON serializer configuration
 * (`ignoreUnknownKeys = true`) to convert the domain model into a JSON string, then
 * saves it under a specific `stringPreferencesKey`.
 * 3. **Observing Data:** Maps the native `dataStore.data` Flow. It reads the JSON string
 * by key, and if present, decodes it back into the domain model. If null, it emits null.
 *
 * Technical Details:
 * * **JSON Configuration:** Configured with `ignoreUnknownKeys = true` to prevent the app
 * from crashing if the serialized JSON in storage contains keys that are no longer present
 * in the current `AuthInfoSerializable` data class.
 * * **Security Note:** The transcript notes that storing highly sensitive data like access
 * tokens ideally requires an encrypted DataStore. However, due to KMP's platform-specific
 * keystores (Android Keystore vs. iOS Keychain), basic DataStore is used here.
 * * **Thread Safety:** DataStore inherently fully supports Kotlin Flows and safe, transactional
 * mutations via `edit`.
 */
class DataStoreSessionStorage(
    private val dataStore: DataStore<Preferences>,
) : SessionStorage {

    private val authInfoKey = stringPreferencesKey("KEY_AUTH_INFO")

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun observeAuthInfo(): Flow<AuthInfo?> {
        return dataStore.data.map { preferences ->
            val serializedJson = preferences[authInfoKey]
            serializedJson?.let {
                json.decodeFromString<AuthInfoSerializable>(it).toDomain()
            }
        }
    }

    override suspend fun set(info: AuthInfo?) {
        if (info == null) {
            dataStore.edit {
                it.remove(authInfoKey)
            }
            return
        }

        val serialized = json.encodeToString(info.toSerializable())
        dataStore.edit { prefs ->
            prefs[authInfoKey] = serialized
        }
    }
}
