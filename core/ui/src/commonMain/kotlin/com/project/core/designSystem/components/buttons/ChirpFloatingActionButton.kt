package com.project.core.designsystem.components.buttons

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.core.designsystem.theme.ChirpTheme

/**
 * A reusable floating action button (FAB) component that serves as the final button variation for the design system model.
 *
 * ## Strategy / Decisions
 * The core strategy is to wrap the standard Material 3 `FloatingActionButton` rather than building a custom component from scratch. Because the desired design system button looks "pretty much exactly like" the Material 3 default, we can simply pass through our custom styling (colors and shapes) to the standard component, saving development time and ensuring stability.
 *
 * ## How It Works
 * 1. Takes in an `onClick` lambda, an optional `Modifier`, and a `content` composable lambda (usually an Icon).
 * 2. Delegates the actual rendering to the Material 3 `FloatingActionButton`.
 * 3. Enforces the design system's geometry by applying a `RoundedCornerShape` of `8.dp`.
 * 4. Resolves the internal colors directly from the `MaterialTheme`, setting the `containerColor` to `primary` and the `contentColor` to `onPrimary`.
 *
 * ## Alternatives / Why Not
 * - **Building a fully custom layout:** Rejected because the custom FAB behaves and looks too similar to the native Material 3 component to justify the overhead of creating custom interaction boundaries and ripples.
 * - **Configuring multi-theme Previews:** Rejected for this specific component because the `primary`/`onPrimary` color combination results in an identical visual appearance in both Light and Dark themes.
 *
 * Technical Details:
 * - **Workflow Optimization:** This file is intended to be scaffolded using a custom IDE live template (`compref`) which hardcodes the specific theme package imports. This bypasses the need to manually wire up the `Preview` theme wrappers for every new component in the course.
 *
 * @param onClick The callback to be invoked when the FAB is clicked.
 * @param modifier The modifier to be applied to the FAB container.
 * @param content The composable content (typically an [androidx.compose.material3.Icon]) to be displayed inside the FAB.
 */
@Composable
fun ChirpFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        content = content,
    )
}

@Composable
@Preview
fun ChirpFloatingActionButtonPreview() {
    ChirpTheme {
        ChirpFloatingActionButton(
            onClick = {},
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
            )
        }
    }
}
