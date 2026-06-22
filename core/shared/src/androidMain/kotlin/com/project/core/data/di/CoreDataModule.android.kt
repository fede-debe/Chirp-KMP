package com.project.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.project.core.data.auth.EncryptedSessionStorage
import com.project.core.data.auth.createDataStore
import com.project.core.data.auth.deleteLegacySessionFile
import com.project.core.data.security.KeystoreSecureStorage
import com.project.core.data.security.SecureStorage
import com.project.core.domain.auth.SessionStorage
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android implementation of the platform core data module.
 *
 * ## Strategy / Decisions
 * * **Engine Selection:** Utilizes the `OkHttp` engine for Ktor as it is the industry standard
 *   for Android networking, offering superior performance and interceptor support.
 */
actual val platformCoreDataModule = module {
    single<HttpClientEngine> { OkHttp.create() }
    single<DataStore<Preferences>> {
        createDataStore(androidContext())
    }
    single<SecureStorage> { KeystoreSecureStorage(androidContext()) }
    single<SessionStorage> {
        // One-time migration: drop the legacy plaintext DataStore session file so no readable
        // tokens linger on disk now that the session is stored encrypted.
        deleteLegacySessionFile(androidContext())
        EncryptedSessionStorage(get())
    }
}
