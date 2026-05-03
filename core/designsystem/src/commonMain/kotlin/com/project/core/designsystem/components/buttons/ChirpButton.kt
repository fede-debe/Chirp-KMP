package com.project.core.designsystem.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.designsystem.theme.extended

/**
 * Reusable button component implementing the core design system from Figma mock-ups.
 * Consolidates various button styles (primary, secondary, destructive, text) into a single, unified composable.
 *
 * ## Strategy / Decisions
 * - **Material 3 Foundation:** Wraps the standard Material 3 `Button` composable rather than building custom touch targets from raw `Box` or `Column` primitives. This was chosen because Material 3 inherently enforces fundamental UX best practices natively (e.g., proper touch target areas, accessible spacings) engineered by Google.
 * - **Enum-Driven Styling:** Employs a `ChirpButtonStyle` enum to drive the visual state instead of exposing raw color or border properties. This guarantees strict adherence to the predefined design system without allowing fragmentation.
 * - **Alpha-based Loading Overlays:** Toggles the visibility of the loading indicator and button content by manipulating their `alpha` values (1f vs 0f). This prevents the button's layout dimensions from aggressively shrinking or jittering when transitioning to a loading state.
 *
 * ## How It Works
 * 1. Evaluates the injected `ChirpButtonStyle` in a `when` block to map out specific Material `ButtonColors` (handling container, content, and disabled permutations) tied to the active theme.
 * 2. Dynamically calculates a `BorderStroke`, factoring in both the button's style enum and its current `enabled` status (e.g., injecting an outline when a primary button is disabled).
 * 3. Encapsulates the internal layout within a `Box` to stack a `CircularProgressIndicator` on top of a `Row` (which holds the optional leading icon and text).
 * 4. Cross-fades the `alpha` visibility of the indicator and the `Row` depending on the state of the `isLoading` flag.
 *
 * ## Alternatives / Why Not
 * - **Custom Layouts from Scratch:** Rejected building a fully custom button from standard layout nodes to avoid losing the baked-in UX and accessibility benefits that come for free with Material 3 components.
 * - **Conditional `if` Rendering for Loading State:** Rejected completely removing the text content from the composition via `if (isLoading)`. Doing so causes the button's overall height to abruptly collapse if the progress indicator's bounding box is smaller than the typography's line height.
 *
 * Technical Details:
 * - Employs an 8.dp `RoundedCornerShape` as the standard baseline rounding metric.
 * - Multiplatform rendering of `@Preview` functions inside the module requires `ui-tooling-preview` to be forcefully applied within the CMP library convention plugins.
 * - A strict `null` value is leveraged over an empty lambda for the icon slot to distinctly signal the absence of an icon down to the composition layer.
 *
 * @param style The visual enum mapping (Primary, Secondary, DestructivePrimary, DestructiveSecondary, Text). Defaults to Primary.
 * @param enabled Toggles user interaction state and shifts layout configuration to disabled styling. Defaults to true.
 * @param isLoading Reveals the circular progress indicator and visually suppresses the button text. Defaults to false.
 * @param leadingIcon An optional composable lambda to render UI elements alongside the text. Defaults to null.
 */
enum class ChirpButtonStyle {
    PRIMARY,
    DESTRUCTIVE_PRIMARY,
    SECONDARY,
    DESTRUCTIVE_SECONDARY,
    TEXT,
}

@Composable
fun ChirpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChirpButtonStyle = ChirpButtonStyle.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val colors = when (style) {
        ChirpButtonStyle.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.extended.disabledFill,
            disabledContentColor = MaterialTheme.colorScheme.extended.textDisabled,
        )
        ChirpButtonStyle.DESTRUCTIVE_PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.colorScheme.extended.disabledFill,
            disabledContentColor = MaterialTheme.colorScheme.extended.textDisabled,
        )
        ChirpButtonStyle.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.extended.textSecondary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.extended.textDisabled,
        )
        ChirpButtonStyle.DESTRUCTIVE_SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.error,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.extended.textDisabled,
        )
        ChirpButtonStyle.TEXT -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.tertiary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.extended.textDisabled,
        )
    }

    val defaultBorderStroke = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.extended.disabledOutline,
    )
    val border = when {
        style == ChirpButtonStyle.PRIMARY && !enabled -> defaultBorderStroke
        style == ChirpButtonStyle.SECONDARY -> defaultBorderStroke
        style == ChirpButtonStyle.DESTRUCTIVE_PRIMARY && !enabled -> defaultBorderStroke
        style == ChirpButtonStyle.DESTRUCTIVE_SECONDARY -> {
            val borderColor = if (enabled) {
                MaterialTheme.colorScheme.extended.destructiveSecondaryOutline
            } else {
                MaterialTheme.colorScheme.extended.disabledOutline
            }
            BorderStroke(
                width = 1.dp,
                color = borderColor,
            )
        }
        else -> null
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = colors,
        border = border,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(15.dp)
                    .alpha(
                        alpha = if (isLoading) 1f else 0f,
                    ),
                strokeWidth = 1.5.dp,
                color = Color.Black,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    8.dp,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(
                    if (isLoading) 0f else 1f,
                ),
            ) {
                leadingIcon?.invoke()
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
@Preview
fun ChirpPrimaryButtonPreview() {
    ChirpTheme(
        darkTheme = true,
    ) {
        ChirpButton(
            text = "Hello world!",
            onClick = {},
            style = ChirpButtonStyle.PRIMARY,
        )
    }
}

@Composable
@Preview
fun ChirpSecondaryButtonPreview() {
    ChirpTheme(
        darkTheme = true,
    ) {
        ChirpButton(
            text = "Hello world!",
            onClick = {},
            style = ChirpButtonStyle.SECONDARY,
        )
    }
}

@Composable
@Preview
fun ChirpDestructivePrimaryButtonPreview() {
    ChirpTheme(
        darkTheme = true,
    ) {
        ChirpButton(
            text = "Hello world!",
            onClick = {},
            style = ChirpButtonStyle.DESTRUCTIVE_PRIMARY,
        )
    }
}

@Composable
@Preview
fun ChirpDestructiveSecondaryButtonPreview() {
    ChirpTheme(
        darkTheme = true,
    ) {
        ChirpButton(
            text = "Hello world!",
            onClick = {},
            style = ChirpButtonStyle.DESTRUCTIVE_SECONDARY,
        )
    }
}

@Composable
@Preview
fun ChirpTextButtonPreview() {
    ChirpTheme(
        darkTheme = true,
    ) {
        ChirpButton(
            text = "Hello world!",
            onClick = {},
            style = ChirpButtonStyle.TEXT,
        )
    }
}
