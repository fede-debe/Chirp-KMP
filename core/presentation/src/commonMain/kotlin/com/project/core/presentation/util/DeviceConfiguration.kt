package com.project.core.presentation.util

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND

/**
 * Maps abstract window size classes into explicit device configuration types to simplify
 * responsive UI development across varying screen formats.
 *
 * ## Strategy / Decisions
 * We utilize the Material 3 Adaptive Library for its predefined `WindowSizeClass` thresholds
 * (Compact, Medium, Expanded). However, handling these raw size classes directly in UI code
 * is too abstract and repetitive. By mapping width and height constraints together, we categorize
 * the screen into actionable enum values. Relying solely on width is insufficient because an
 * "Expanded" width could represent a desktop, a foldable, or a mobile phone turned sideways;
 * evaluating both width and height simultaneously ensures proper layout selection.
 *
 * ## How It Works
 * 1. `fromWindowSizeClass` takes a `WindowSizeClass` and opens a `with` block to directly access dimensional values.
 * 2. It compares `minWidthDp` and `minHeightDp` against standard Material 3 bounds.
 * 3. **Mobile Portrait:** Width is Compact (< Medium lower bound) AND Height is at least Medium.
 * 4. **Mobile Landscape:** Width is Expanded (>= Expanded lower bound) AND Height is Compact (< Medium lower bound).
 * 5. **Tablet Portrait:** Width is in the Medium range AND Height is Expanded.
 * 6. **Tablet Landscape:** Width is Expanded AND Height is in the Medium range.
 * 7. **Desktop:** Acts as the fallback for all other combinations (typically the largest screens).
 * 8. `currentDeviceConfiguration()` is a Composable helper that fetches `currentWindowAdaptiveInfo()` and applies this mapping.
 *
 * ## Alternatives / Why Not
 * - **Raw WindowSizeClasses:** Rejected checking purely `widthSizeClass == Expanded` as it fails to
 *   differentiate between form factors (like desktop vs. mobile landscape).
 *
 * ## Technical Details
 * - **Edge Cases/Constraints:** This logic evaluates available UI dimensions, not hardware identifiers.
 *   A very large tablet might trigger the `DESKTOP` configuration. This is expected and desired,
 *   as the goal is rendering responsive layouts appropriate for the available screen real estate.
 * - **Dependency:** Requires the `material3-adaptive` library in the presentation module.
 *
 * @param windowSizeClass The current window size dimensions to evaluate.
 * @return The resulting categorized [DeviceConfiguration].
 */

/**
 * Retrieves the current categorized device configuration based on adaptive window sizing.
 */
@Composable
fun currentDeviceConfiguration(): DeviceConfiguration {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return DeviceConfiguration.fromWindowSizeClass(windowSizeClass)
}

enum class DeviceConfiguration {
    MOBILE_PORTRAIT,
    MOBILE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE,
    DESKTOP,
    ;

    val isMobile: Boolean
        get() = this in listOf(MOBILE_PORTRAIT, MOBILE_LANDSCAPE)

    companion object {
        fun fromWindowSizeClass(windowSizeClass: WindowSizeClass): DeviceConfiguration {
            return with(windowSizeClass) {
                when {
                    minWidthDp < WIDTH_DP_MEDIUM_LOWER_BOUND &&
                        minHeightDp >= HEIGHT_DP_MEDIUM_LOWER_BOUND -> MOBILE_PORTRAIT

                    minWidthDp >= WIDTH_DP_EXPANDED_LOWER_BOUND &&
                        minHeightDp < HEIGHT_DP_MEDIUM_LOWER_BOUND -> MOBILE_LANDSCAPE

                    minWidthDp in WIDTH_DP_MEDIUM_LOWER_BOUND..WIDTH_DP_EXPANDED_LOWER_BOUND &&
                        minHeightDp >= HEIGHT_DP_EXPANDED_LOWER_BOUND -> TABLET_PORTRAIT

                    minWidthDp >= WIDTH_DP_EXPANDED_LOWER_BOUND &&
                        minHeightDp in HEIGHT_DP_MEDIUM_LOWER_BOUND..HEIGHT_DP_EXPANDED_LOWER_BOUND -> TABLET_LANDSCAPE

                    else -> DESKTOP
                }
            }
        }
    }
}
