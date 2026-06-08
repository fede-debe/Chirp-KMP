package com.project.core.presentation.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore

/**
 * A utility ViewModel designed to maintain references to other ViewModelStores, specifically
 * for scoping ViewModels to transient UI components like Dialogs and Bottom Sheets.
 *
 * ## Strategy / Decisions
 * On Android, configuration changes (like screen rotations or theme switches) cause the Compose UI
 * to be recreated. If we held ViewModel references in a simple map directly inside a Composable,
 * they would be reset and lost. By housing this map inside a dedicated ViewModel scoped to a
 * higher-level component (like the navigation graph or activity), the registry survives
 * configuration changes, keeping our dialog ViewModels intact.
 *
 * ## How It Works
 * 1. Maintains a `mutableMapOf<String, ViewModelStore>`, where the key is a unique ID for a specific dialog.
 * 2. Provides a `getOrCreate(id)` function that returns an existing `ViewModelStore` or creates a new one for that ID.
 * 3. Provides a `clear(id)` function that removes the store from the map and explicitly calls its `clear()` method, destroying the underlying ViewModel when a dialog is dismissed.
 * 4. Overrides the standard `onCleared()` function to iterate through all stored map values, clearing every child ViewModel automatically when the parent screen is permanently destroyed.
 *
 * ## Alternatives / Why Not
 * * **Scoping to the Parent Screen:** You could just scope dialog ViewModels to the underlying screen, but they would outlive the dialog and remain active until the screen itself is popped. Manual clearing is highly error-prone.
 * * **"God" ViewModels:** You could shove all dialog state (e.g., complex profile updates, file uploads) into the parent screen's ViewModel. This was rejected because complex dialogs possess the complexity of full screens, and merging them would create massive, unmaintainable "God ViewModels".
 *
 * Technical Details:
 * * This ViewModel acts as a Singleton-like registry for the lifecycle of the parent owner it is attached to (usually the parent backstack entry).
 */
class ScopedStoreRegistryViewModel : ViewModel() {

    private val stores = mutableMapOf<String, ViewModelStore>()

    fun getOrCreate(id: String): ViewModelStore =
        stores.getOrPut(id) { ViewModelStore() }

    fun clear(id: String) {
        stores.remove(id)?.clear()
    }

    override fun onCleared() {
        super.onCleared()
        stores.values.forEach { it.clear() }
        stores.clear()
    }
}
