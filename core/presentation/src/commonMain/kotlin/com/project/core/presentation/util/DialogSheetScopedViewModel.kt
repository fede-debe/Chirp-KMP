@file:OptIn(ExperimentalUuidApi::class)

package com.project.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A Composable wrapper that strictly scopes a ViewModel to the visibility lifecycle of a Dialog or BottomSheet.
 *
 * ## Strategy / Decisions
 * By default, Jetpack Compose Dialog routes give you a dedicated backstack entry to scope ViewModels to.
 * However, BottomSheets on mobile do not have this route option. To share the same ViewModel instance
 * between a mobile BottomSheet and a tablet Dialog, we needed a custom solution that manages the
 * ViewModelStore manually based on a visibility boolean, while surviving configuration changes.
 *
 * ## How It Works
 * 1. Retrieves the nearest `LocalViewModelStoreOwner.current` (the parent screen or navigation graph).
 * 2. Fetches the `ScopedStoreRegistryViewModel` via Koin, bound to this parent owner.
 * 3. Listens to the `isVisible` parameter.
 * - If `isVisible` becomes true and no owner exists: Creates a new anonymous `ViewModelStoreOwner`. Its store is retrieved from the registry using `getOrCreate(scopeId)`.
 * - If `isVisible` becomes false and an owner exists: Calls `clear(scopeId)` on the registry to destroy the ViewModel, and sets the local owner back to null.
 * 4. Uses `CompositionLocalProvider` to override the `LocalViewModelStoreOwner` with the newly created dialog owner. Any Koin injected ViewModel inside the `content` block will now bind to this temporary store.
 *
 * ## Alternatives / Why Not
 * * **Using `remember` for the Store:** Storing the `ViewModelStoreOwner` in a standard `remember` block would cause the ViewModel to be destroyed upon device rotation.
 * * **Manual Lifecycle Management:** Forcing developers to manually call clear on their ViewModels when dismissing dialogs leads to memory leaks if forgotten. This automates the teardown.
 *
 * Technical Details:
 * * **Constraints:** This does not perfectly handle Android Process Death out-of-the-box. If the app is killed in the background, the UI state resets to false and the `stores` map is not preserved via `SavedStateHandle`. For this specific project scope, this rare edge case is considered acceptable.
 * * **Dependencies:** Requires the `koin-compose-viewmodel` dependency to inject the registry scoped to the parent.
 *
 * @param isVisible Boolean representing whether the dialog/sheet is currently on screen.
 * @param scopeId A unique string identifier for the dialog's scope. Defaults to a `rememberSaveable` random UUID.
 * @param content The composable content of the dialog.
 * @throws IllegalStateException if called outside of a valid Compose setup where no parent `ViewModelStoreOwner` can be found.
 */
@Composable
fun DialogSheetScopedViewModel(
    visible: Boolean,
    scopeId: String = rememberSaveable { Uuid.random().toString() },
    content: @Composable () -> Unit,
) {
    val parentOwner = LocalViewModelStoreOwner.current
        ?: throw IllegalStateException("No parent owner found")

    val registry = koinViewModel<ScopedStoreRegistryViewModel>(
        viewModelStoreOwner = parentOwner,
    )

    var owner by remember { mutableStateOf<ViewModelStoreOwner?>(null) }

    LaunchedEffect(visible, scopeId) {
        if (visible && owner == null) {
            owner = object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore
                    get() = registry.getOrCreate(scopeId)
            }
        } else if (!visible && owner != null) {
            registry.clear(scopeId)
            owner = null
        }
    }

    owner?.let { dialogOwner ->
        CompositionLocalProvider(LocalViewModelStoreOwner provides dialogOwner) {
            content()
        }
    }
}
