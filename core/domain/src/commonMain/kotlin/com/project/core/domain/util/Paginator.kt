package com.project.core.domain.util

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * A generic utility class that encapsulates the raw pagination logic for fetching data from an API.
 * It manages current keys, prevents duplicate loads, and handles loading/error state delegation.
 *
 * ## Strategy / Decisions
 * - **API-Only Pagination:** This class handles pagination strictly for the remote API. We stick to a Single Source
 * of Truth principle: the API responses are fetched via this class, inserted into the local database, and the UI
 * automatically updates by observing the DB.
 * - **Gene\ric Design (`<Key, Item>`):** Decoupled from specific models to support any type of pagination schema.
 * It can utilize integer-based pages or timestamp cursors (e.g., an `Instant` representing a `before` timestamp).
 *
 * ## How It Works
 * 1. Validates the request: Cancels if a request is currently active (`isMakingRequest`) or if the `currentKey`
 * is identical to the `lastRequestKey` (preventing duplicate page loads).
 * 2. Prepares the state: Sets the loading indicator to true via `onLoadUpdated` and registers `lastRequestKey`.
 * 3. Execution: Invokes the suspending `onRequest` lambda with the `currentKey`.
 * 4. On Success: Passes the retrieved items to the `onSuccess` lambda. Extracts the key for the *next* page
 * using the `getNextKey` lambda and updates the `currentKey` internally.
 * 5. On Error: Wraps specific data errors into a `DataErrorException` (allowing type-safe UI text parsing later)
 * and triggers the `onError` lambda.
 * 6. Cleanup: A `finally` block ensures `onLoadUpdated(false)` and `isMakingRequest = false` are always called.
 *
 * ## Alternatives / Why Not
 * - **Paging Library / RemoteMediator:** In pure Android, the Paging Library's `RemoteMediator` is used to orchestrate
 * paging between a local DB and a remote API. However, this feature is not natively mature/available in Compose
 * Multiplatform (KMP) at the time of authoring.
 * - **Paginating the Local DB:** Rejected due to complexity vs. reward. Reading from a local DB is extremely fast
 * (50-100ms). Paginating local DB queries alongside network queries is incredibly complex. Instead, combining
 * a non-paginated DB observer with a `LazyColumn` (which inherently recycles views) provides excellent performance.
 *
 * ## Technical Details
 * - **Thread Safety & Coroutines:** Relies on suspending lambdas for asynchronous data fetching.
 * - **Exception Handling:** Catches general exceptions during the request but explicitly requires
 * `coroutineContext.ensureActive()` to ensure coroutine cancellation exceptions are respected and not swallowed.
 *
 * @param initialKey The starting key (e.g., `null` to load the very first page).
 * @param onLoadUpdated Lambda invoked with a boolean to update the view model's loading state.
 * @param onRequest Suspending lambda that executes the actual HTTP call using the current key.
 * @param getNextKey Lambda that computes the next pagination key from the loaded list of items (e.g., the last timestamp).
 * @param onError Suspending lambda invoked when the request fails, providing the wrapped exception.
 * @param onSuccess Suspending lambda invoked when items are fetched, providing the new items and the calculated new key.
 */
class Paginator<Key, Item>(
    private val initialKey: Key,
    private val onLoadUpdated: (Boolean) -> Unit,
    private val onRequest: suspend (nextKey: Key) -> Result<List<Item>, DataError>,
    private val getNextKey: suspend (List<Item>) -> Key,
    private val onError: suspend (Throwable?) -> Unit,
    private val onSuccess: suspend (items: List<Item>, newKey: Key) -> Unit,
) {
    private var currentKey = initialKey
    private var isMakingRequest = false
    private var lastRequestKey: Key? = null

    suspend fun loadNextItems() {
        if (isMakingRequest) {
            return
        }

        if (currentKey != null && currentKey == lastRequestKey) {
            return
        }

        isMakingRequest = true
        onLoadUpdated(true)

        try {
            onRequest(currentKey)
                .onSuccess { items ->
                    val newKey = getNextKey(items)
                    onSuccess(items, newKey)
                    lastRequestKey = currentKey

                    currentKey = newKey
                }
                .onFailure { error ->
                    onError(DataErrorException(error))
                }
        } catch (e: Exception) {
            coroutineContext.ensureActive()

            onError(e)
        } finally {
            onLoadUpdated(false)
            isMakingRequest = false
        }
    }

    fun reset() {
        currentKey = initialKey
        lastRequestKey = null
    }
}
