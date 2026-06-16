package com.project.chirp.windows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.FrameWindowScope
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

/**
 * A Composable extension on `FrameWindowScope` used to track the focus state of individual desktop windows.
 * This is critical for determining whether the application is in the background and should actually display system notifications.
 *
 * ## Strategy / Decisions
 * - **State Bubbling:** Window focus state must be captured at the low-level AWT window API but orchestrated at the higher application level. We use this observer to capture these events and bubble them up to the application state holder.
 *
 * ## How It Works
 * 1. Utilizes a `DisposableEffect` to manage the lifecycle of an AWT `WindowFocusListener`.
 * 2. Overrides `windowGainedFocus` to emit `true` and `windowLostFocus` to emit `false` via the `onFocusChanged` lambda.
 * 3. Cleans up by calling `removeWindowFocusListener` when the Composable leaves the composition to prevent memory leaks.
 *
 * ## Technical Details
 * - Requires `FrameWindowScope` to access the underlying desktop window instance via `window.addWindowFocusListener`.
 *
 * @param onFocusChanged Callback invoked with `true` when focused and `false` when unfocused.
 */
@Composable
fun FrameWindowScope.FocusObserver(
    onFocusChanged: (Boolean) -> Unit,
) {
    DisposableEffect(Unit) {
        val focusListener = object : WindowFocusListener {
            override fun windowGainedFocus(p0: WindowEvent?) {
                onFocusChanged(true)
            }

            override fun windowLostFocus(p0: WindowEvent?) {
                onFocusChanged(false)
            }
        }

        window.addWindowFocusListener(focusListener)

        onDispose {
            window.removeWindowFocusListener(focusListener)
        }
    }
}
