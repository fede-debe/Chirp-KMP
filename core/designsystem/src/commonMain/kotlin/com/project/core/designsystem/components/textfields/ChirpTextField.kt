package com.project.core.designsystem.components.textfields

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.designsystem.theme.extended

/**
 * A comprehensive design system component that bundles a text input field with an optional title and
 * supporting text, capable of rendering various interactive states (idle, focused, disabled, error).
 *
 * ## Strategy / Decisions
 * - **TextFieldState over Raw Strings:** We strictly use the Compose `TextFieldState` rather than
 *   passing raw strings. Default Material text fields can struggle with asynchronous typing (especially
 *   when state is saved in a StateFlow), leading to letters being inserted out of order. `TextFieldState`
 *   safely resolves this and provides a solid foundation for reactive programming.
 * - **BasicTextField with Decorator:** Instead of using a standard Material `TextField` or `OutlinedTextField`,
 *   we use `BasicTextField`. This gives us absolute control over the visual structure (custom backgrounds, borders)
 *   and allows us to construct the inner container (placeholder + cursor + actual text) using the `decorator` API.
 *
 * ## How It Works
 * 1. **Layout Container:** Elements are arranged top-to-bottom in a `Column` (Title -> Input Field -> Supporting Text).
 * 2. **Interaction Tracking:** A `MutableInteractionSource` is remembered and used to collect the focus state
 *    (`isFocusedAsState`). A `LaunchedEffect` watches this focus boolean and triggers the `onFocusChanged` callback
 *    whenever focus shifts, notifying the parent component.
 * 3. **Dynamic Styling:** The text field `modifier` dynamically calculates background and border colors.
 *    For example, a focused field gets a 5% alpha primary background and a primary-colored border. Errors
 *    force a red border, and disabled fields use a placeholder outline.
 * 4. **Inner Decorator Box:** The `decorator` composable lambda wraps the `innerBox`. Inside a `Box` aligned to
 *    `CenterStart`, it checks if the underlying text is empty. If empty and a placeholder exists, it draws the
 *    placeholder text. Crucially, it then executes the `innerBox` lambda to draw the actual text content and cursor.
 *
 * ## Alternatives / Why Not
 * - **Leading Icons Rejected:** Initially, a `leadingIcon` parameter was considered. However, after reviewing
 *   the design mockups, it was realized that standard text fields do not use leading icons (password text fields
 *   use trailing icons, but that is a separate component). The parameter was removed to keep the API clean.
 *
 * ## Technical Details
 * - **State Management:** Fully relies on Compose foundation's `TextFieldState` and `MutableInteractionSource`.
 * - **Styling Constraints:** Border shapes are hardcoded to `RoundedCornerShape(8.dp)`. Colors rely heavily
 *   on both standard Material Scheme and extended Material Theme properties (e.g., `extendedTextSecondary`, `extendedTextPlaceholder`).
 *
 * @param modifier The modifier to be applied to the outermost layout column.
 * @param state The `TextFieldState` managing the reactive text input.
 * @param placeholder Optional text shown inside the field when the input is empty.
 * @param title Optional text displayed above the text field.
 * @param supportingText Optional helper or error text displayed below the text field.
 * @param isError Boolean indicating if the field should render in an error state (red border, red supporting text).
 * @param singleLine Boolean dictating whether the text field restricts input to a single line or allows multiline.
 * @param keyboardType Configures the displayed keyboard layout (e.g., adjusting prominent keys for Email).
 * @param onFocusChanged Lambda invoked whenever the focus state of the text field changes.
 * @param enabled Controls the interactive and visual disabled/enabled state of the field.
 */
@Composable
fun ChirpTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    title: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
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
        BasicTextField(
            state = state,
            enabled = enabled,
            lineLimits = if (singleLine) {
                TextFieldLineLimits.SingleLine
            } else {
                TextFieldLineLimits.Default
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.extended.textPlaceholder
                },
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            interactionSource = interactionSource,
            modifier = styleModifier,
            decorator = { innerBox ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
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
            },
        )
    }
}

@Composable
@Preview(
    showBackground = true,
)
fun ChirpTextFieldEmptyPreview() {
    ChirpTheme {
        ChirpTextField(
            state = rememberTextFieldState(),
            modifier = Modifier
                .width(300.dp),
            placeholder = "test@test.com",
            title = "Email",
            supportingText = "Please enter your email",
        )
    }
}

@Composable
@Preview(
    showBackground = true,
)
fun ChirpTextFieldFilledPreview() {
    ChirpTheme {
        ChirpTextField(
            state = rememberTextFieldState(
                initialText = "test@test.com",
            ),
            modifier = Modifier
                .width(300.dp),
            placeholder = "test@test.com",
            title = "Email",
            supportingText = "Please enter your email",
        )
    }
}

@Composable
@Preview(
    showBackground = true,
)
fun ChirpTextFieldDisabledPreview() {
    ChirpTheme {
        ChirpTextField(
            state = rememberTextFieldState(),
            modifier = Modifier
                .width(300.dp),
            placeholder = "test@test.com",
            title = "Email",
            supportingText = "Please enter your email",
            enabled = false,
        )
    }
}

@Composable
@Preview(
    showBackground = true,
)
fun ChirpTextFieldErrorPreview() {
    ChirpTheme {
        ChirpTextField(
            state = rememberTextFieldState(),
            modifier = Modifier
                .width(300.dp),
            placeholder = "test@test.com",
            title = "Email",
            supportingText = "This is not a valid email",
            isError = true,
        )
    }
}
