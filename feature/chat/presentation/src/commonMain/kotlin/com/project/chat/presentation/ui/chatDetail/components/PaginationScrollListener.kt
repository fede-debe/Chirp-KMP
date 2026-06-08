package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A non-visual Composable that listens to the scroll position of a LazyList to proactively trigger pagination when the user scrolls near the top.
 *
 * ## Strategy / Decisions
 * - **SnapshotFlow over direct LaunchedEffect:** The `LazyListState` properties (like `firstVisibleItemIndex`) change at a very high frequency during scrolling. Reacting to them directly in a `LaunchedEffect` would cause constant, expensive reinvocations. A `snapshotFlow` is used to safely convert these compose states into a reactive flow that only emits when specific derived calculations change.
 * - **Race Condition Mitigation:** A local `lastTriggerItemCount` is maintained to prevent double-triggering. If pagination succeeds but the Room DB hasn't emitted the new list to the UI yet, the listener might falsely think it's still at the top. Checking if the `itemCount` has actually grown since the last trigger prevents this.
 *
 * ## How It Works
 * 1. Uses `rememberUpdatedState` for `itemCount`, `isPaginationLoading`, and `isEndReached` to ensure the flow evaluates the latest data without restarting the coroutine.
 * 2. Extracts `visibleItemsInfo` from the `LazyListState` layout info.
 * 3. Finds the `topVisibleIndex` (referencing `lastOrNull()` due to the reverse layout).
 * 4. Calculates `remainingItems` by subtracting the visible top index from the total items.
 * 5. Constructs a `PaginationScrollState` data class and filters identical sequential emissions using `distinctUntilChanged`.
 * 6. In the collector, evaluates if `remainingItems <= 5` and ensures no active loading or end-reached flags are true.
 * 7. Checks that the current item count is greater than the `lastTriggerItemCount` before invoking the `onNearTop` lambda.
 *
 * ## Alternatives / Why Not
 * - **Why `lastOrNull()` instead of `firstOrNull()`?** In a standard list, the top item is the first index. In a reverse layout, the highest visual item actually has the largest index. Therefore, looking for the last visible item mathematically equates to the top of the viewport.
 * * ## Technical Details
 * - **Thread Safety/State Management:** Uses `mutableIntStateOf` to store `lastTriggerItemCount` locally.
 * - **Threshold:** The pre-fetch threshold is hardcoded to 5 items to proactively fetch data before the user physically hits the ceiling.
 *
 * @param lazyListState The state of the list being scrolled.
 * @param itemCount The current number of real items loaded in the list.
 * @param isPaginationLoading True if a network request is currently active.
 * @param isEndReached True if the API has indicated no more historical pages exist.
 * @param onNearTop Lambda invoked when the scroll threshold is breached.
 */
@Composable
fun PaginationScrollListener(
    lazyListState: LazyListState,
    itemCount: Int,
    isPaginationLoading: Boolean,
    isEndReached: Boolean,
    onNearTop: () -> Unit,
) {
    val updatedItemCount by rememberUpdatedState(itemCount)
    val isPaginationLoading by rememberUpdatedState(isPaginationLoading)
    val isEndReached by rememberUpdatedState(isEndReached)

    var lastTriggerItemCount by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val info = lazyListState.layoutInfo
            val total = info.totalItemsCount
            val topVisibleIndex = info.visibleItemsInfo.lastOrNull()?.index
            val remainingItems = if (topVisibleIndex != null) {
                total - topVisibleIndex - 1
            } else {
                null
            }

            PaginationScrollState(
                currentItemCount = updatedItemCount,
                isEligible = remainingItems != null &&
                    remainingItems <= 5 &&
                    !isPaginationLoading &&
                    !isEndReached,
            )
        }
            .distinctUntilChanged()
            .collect { (itemCount, isEligible) ->
                val shouldTrigger = isEligible && itemCount > lastTriggerItemCount

                if (shouldTrigger) {
                    lastTriggerItemCount = itemCount
                    onNearTop()
                }
            }
    }
}

data class PaginationScrollState(
    val currentItemCount: Int,
    val isEligible: Boolean,
)
