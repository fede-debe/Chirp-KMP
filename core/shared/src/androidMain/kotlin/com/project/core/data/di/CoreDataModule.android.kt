package com.project.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.project.core.data.auth.createDataStore
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
}
