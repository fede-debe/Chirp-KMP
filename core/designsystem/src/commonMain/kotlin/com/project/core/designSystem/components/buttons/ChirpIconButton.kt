package com.project.core.designSystem.components.buttons

/**
 * A reusable, theme-styled outlined icon button used throughout the Chirp application.
 *
 * ## Strategy / Decisions
 * This component wraps the Material 3 `OutlinedIconButton` to enforce the Chirp design system's
 * specific styling constraints centrally. By accepting a composable lambda for its `content`,
 * it decouples the visual container from the specific icon being displayed, allowing it to act
 * as a versatile base component for various UI needs (e.g., close buttons, back buttons) without
 * hardcoding the internal asset.
 *
 * ## How It Works
 * 1. Initializes an `OutlinedIconButton` to handle base interaction and outline semantics.
 * 2. Sets a preferred, slightly larger touch target size of 45.dp via a modifier.
 * 3. Applies a specific `RoundedCornerShape` with an 8.dp radius.
 * 4. Draws a 1.dp `BorderStroke` using the standard `outline` color from the application's color scheme.
 * 5. Overrides default colors by mapping the container color to `surface` and the content color
 *    to the custom extended theme's `textSecondary` color.
 * 6. Renders the provided `content` trailing lambda (usually an `Icon`) inside this styled container.
 *
 * ## Technical Details
 * - Uses the trailing lambda convention for the `content` parameter to improve call-site readability.
 * - Configured with internal composable previews for both Light and Dark themes to ensure visual consistency
 *   across different application states.
 * - Utilizes `Icons.AutoMirrored` in previews to prevent deprecation warnings for directional icons.
 *
 * @param onClick The callback to be invoked when this button is clicked.
 * @param modifier The modifier to be applied to the button layout.
 * @param content The composable lambda defining the visual content of the button.
 */
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.core.designSystem.theme.ChirpTheme
import com.project.core.designSystem.theme.extended

@Composable
fun ChirpIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier
            .size(45.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.extended.textSecondary,
        ),
    ) {
        content()
    }
}

@Composable
@Preview
fun ChirpButtonPreview() {
    ChirpTheme {
        ChirpIconButton(
            onClick = {},
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
            )
        }
    }
}

@Composable
@Preview
fun ChirpButtonDarkThemePreview() {
    ChirpTheme(darkTheme = true) {
        ChirpIconButton(
            onClick = {},
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
            )
        }
    }
}
