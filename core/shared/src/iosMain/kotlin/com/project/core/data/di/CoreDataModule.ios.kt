package com.project.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.project.core.data.auth.createDataStore
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
}
