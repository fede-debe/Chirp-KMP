@file:Suppress("ktlint:standard:filename")

package com.project.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.project.core.data.util.appDataDirectory
import java.io.File

/**
 * Desktop-specific implementation for instantiating local DataStore preferences.
 *
 * ## Strategy / Decisions
 * Reuses the centralized `appDataDirectory` utility to find the correct system folder.
 * By appending the common DataStore filename to this OS-specific path, we ensure that user
 * preferences are safely stored within standard application boundaries.
 *
 * ## How It Works
 * 1. Invokes the `appDataDirectory` utility to get the base file path.
 * 2. Appends the shared `dataStoreFileName` to create the final File reference.
 * 3. Passes the absolute path of this file to the common `createDataStore` builder.
 */
fun createDataStore(): DataStore<Preferences> = createDataStore {
    val directory = appDataDirectory

    if (!directory.exists()) {
        directory.mkdirs()
    }

    File(directory, DATA_STORE_FILE_NAME).absolutePath
}
