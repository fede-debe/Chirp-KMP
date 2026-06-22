package com.project.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.project.core.data.auth.EncryptedSessionStorage
import com.project.core.data.auth.createDataStore
import com.project.core.data.auth.deleteLegacySessionFile
import com.project.core.data.security.KeychainSecureStorage
import com.project.core.data.security.SecureStorage
import com.project.core.domain.auth.SessionStorage
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

/**
 * iOS implementation of the platform core data module.
 *
 * ## Strategy / Decisions
 * * **Engine Selection:** Utilizes the `Darwin` engine for Ktor to leverage native iOS
 *   URLSession capabilities, ensuring optimal integration with the iOS networking stack.
 */
actual val platformCoreDataModule = module {
    single<HttpClientEngine> { Darwin.create() }
    single<DataStore<Preferences>> {
        createDataStore()
    }
    single<SecureStorage> { KeychainSecureStorage() }
    single<SessionStorage> {
        // One-time migration: drop the legacy plaintext DataStore session file so no readable
        // tokens linger on disk now that the session is stored encrypted.
        deleteLegacySessionFile()
        EncryptedSessionStorage(get())
    }
}
