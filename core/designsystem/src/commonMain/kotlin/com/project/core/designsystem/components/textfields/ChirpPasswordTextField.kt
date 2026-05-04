package com.project.core.designsystem.components.textfields

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chirp.core.designsystem.generated.resources.Res
import chirp.core.designsystem.generated.resources.eye_icon
import chirp.core.designsystem.generated.resources.eye_off_icon
import chirp.core.designsystem.generated.resources.hide_password
import chirp.core.designsystem.generated.resources.show_password
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.designsystem.theme.extended
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * A secure text field component specifically designed for password input, featuring a visibility toggle.
 *
 * ## Strategy / Decisions
 * Built on top of the newly extracted `ChirpTextFieldLayout` to ensure visual consistency with other inputs.
 * It utilizes a `BasicSecureTextField` to handle password obfuscation seamlessly. A custom `Row` layout is
 * implemented for the inner decorator to properly align the text input space and the trailing visibility
 * toggle icon without overlapping text.
 *
 * ## How It Works
 * 1. Wraps `ChirpTextFieldLayout`, forwarding standard layout parameters.
 * 2. Sets the `textObfuscationMode` dynamically (`visible` or `hidden`) based on the `isPasswordVisible` state.
 * 3. Structures the inner decorator as a `Row`. The text input `Box` is assigned a `weight(1f)`. This ensures
 *    the text input spans the available space but stops exactly at the left edge of the trailing icon, preventing overlap.
 * 4. Renders a trailing `Icon` that swaps its vector resource (`eye_off` / `eye_on`) and content description
 *    (`hide_password` / `show_password`) based on the current visibility state.
 *
 * ## Alternatives / Why Not
 * - **Rejected `IconButton`:** Standard Material `IconButton` components have a default touch target size of 48dp.
 *   Using one would undesirably increase the overall height of the text field. Instead, a standard `Icon` is used
 *   with a custom `clickable` modifier and an unbounded 24dp ripple effect to maintain precise dimensional control
 *   while preserving accessibility.
 * - **Rejected Direct SVG Usage:** Compose Multiplatform in Android Studio currently lacks direct SVG resource
 *   creation shortcuts. SVGs were exported from Figma and first converted to XML Drawables via the Android
 *   module's Vector Asset studio to ensure proper cross-platform rendering and integration.
 *
 * ## Technical Details
 * - Enforces `singleLine = true` and `keyboardType = Password` universally; they are hardcoded as they do not change for passwords.
 * - Uses localized string resources (from a generated `string.xml` in `composeResources/values`) for accessibility
 *   descriptions on the toggle icon to support screen readers.
 *
 * @param isPasswordVisible Boolean controlling whether the obfuscation is hidden or visible.
 * @param onToggleVisibilityClick Lambda triggered to invert the `isPasswordVisible` state.
 */
@Composable
fun ChirpPasswordTextField(
    state: TextFieldState,
    isPasswordVisible: Boolean,
    onToggleVisibilityClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    title: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    ChirpTextFieldLayout(
        title = title,
        isError = isError,
        supportingText = supportingText,
        enabled = enabled,
        onFocusChanged = onFocusChanged,
        modifier = modifier,
    ) { styleModifier, interactionSource ->
        BasicSecureTextField(
            state = state,
            modifier = styleModifier,
            enabled = enabled,
            textObfuscationMode = if (isPasswordVisible) {
                TextObfuscationMode.Visible
            } else {
                TextObfuscationMode.Hidden
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.extended.textPlaceholder
                },
            ),
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorator = { innerBox ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.text.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                color = MaterialTheme.colorScheme.extended.textPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        innerBox()
                    }

                    Icon(
                        imageVector = if (isPasswordVisible) {
                            vectorResource(Res.drawable.eye_off_icon)
                        } else {
                            vectorResource(Res.drawable.eye_icon)
                        },
                        contentDescription = if (isPasswordVisible) {
                            stringResource(Res.string.hide_password)
                        } else {
                            stringResource(Res.string.show_password)
                        },
                        tint = MaterialTheme.colorScheme.extended.textDisabled,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(
                                    bounded = false,
                                    radius = 24.dp,
                                ),
                                onClick = onToggleVisibilityClick,
                            ),
                    )
                }
            },
        )
    }
}

@Composable
@Preview(
    showBackground = true,
)
fun ChirpPasswordTextFieldEmptyPreview() {
    ChirpTheme {
        ChirpPasswordTextField(
            state = rememberTextFieldState(),
            isPasswordVisible = true,
            onToggleVisibilityClick = {},
            modifier = Modifier
                .width(300.dp),
            placeholder = "Password",
            title = "Password",
            supportingText = "Use 9+ characters, at least one digit and one uppercase letter",
        )
    }
}

@Composable
@Preview(
    showBackground = true,
)
fun ChirpPasswordTextFieldFilledPreview() {
    ChirpTheme {
        ChirpPasswordTextField(
            state = rememberTextFieldState("password123"),
            isPasswordVisible = false,
            onToggleVisibilityClick = {},
            modifier = Modifier
                .width(300.dp),
            placeholder = "Password",
            title = "Password",
            supportingText = "Use 9+ characters, at least one digit and one uppercase letter",
        )
    }
}

@Composable
@Preview(
    showBackground = true,
)
fun ChirpPasswordTextFieldErrorPreview() {
    ChirpTheme {
        ChirpPasswordTextField(
            state = rememberTextFieldState("password123"),
            isPasswordVisible = true,
            onToggleVisibilityClick = {},
            modifier = Modifier
                .width(300.dp),
            placeholder = "Password",
            title = "Password",
            supportingText = "Doesn't contain an uppercase character",
            isError = true,
        )
    }
}
