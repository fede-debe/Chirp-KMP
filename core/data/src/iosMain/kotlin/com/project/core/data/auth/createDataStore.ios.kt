@file:Suppress("ktlint:standard:filename")
@file:OptIn(ExperimentalForeignApi::class)

package com.project.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS-specific instantiation of the Preferences DataStore.
 *
 * ## Strategy / Decisions
 * Interacts directly with the iOS `NSFileManager` via Kotlin/Native interoperability
 * to find the appropriate system directory for persisting user preferences.
 *
 * ## How It Works
 * 1. Calls the common `createDataStore` function.
 * 2. Retrieves the `NSFileManager.defaultManager`.
 * 3. Queries for the `NSDocumentDirectory` within the `NSUserDomainMask` (the standard iOS
 * location for user-generated or app-persistent data).
 * 4. Extracts the path string from the resulting URL and appends the shared file name.
 *
 * Technical Details:
 * * Requires `@OptIn(ExperimentalForeignApi::class)` because accessing C/Objective-C iOS
 * APIs (like `NSFileManager`) from Kotlin Multiplatform is considered an experimental
 * foreign API interaction.
 */
fun createDataStore(): DataStore<Preferences> {
    return createDataStore {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        requireNotNull(directory).path + "/$DATA_STORE_FILE_NAME"
    }
}
