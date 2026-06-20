@file:Suppress("ktlint:standard:filename")

package com.project.core.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Android-specific instantiation of the Preferences DataStore.
 *
 * ## Strategy / Decisions
 * Uses the Android `Context` to securely resolve the file path within the app's internal
 * storage sandbox, ensuring no other apps can read the session data.
 *
 * ## How It Works
 * 1. Takes an Android `Context` as a parameter.
 * 2. Calls the common `createDataStore` function.
 * 3. In the lambda, accesses `context.filesDir` to target the private sandbox.
 * 4. Resolves the absolute path by appending the shared `dataStoreFileName`.
 *
 * Technical Details:
 * * **DI Integration:** Supplied to Koin via `androidContext()` leveraging the `koin-android`
 * dependency to automatically fetch the application context.
 */
fun createDataStore(context: Context): DataStore<Preferences> {
    return createDataStore {
        context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
    }
}
