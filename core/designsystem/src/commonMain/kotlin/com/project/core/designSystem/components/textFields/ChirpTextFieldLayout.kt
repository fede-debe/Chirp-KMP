package com.project.core.designSystem.components.textFields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.core.designSystem.theme.extended

/**
 * Extracts the common structural layout for text fields to enable reuse across different input types.
 *
 * ## Strategy / Decisions
 * The overarching container, title, supporting text, and focus tracking are identical between standard
 * text fields (e.g., email) and secure text fields (e.g., passwords). To prevent duplication, this layout
 * component acts as a wrapper, sandwiching the varying input component via a composable lambda. It also
 * defines and distributes a shared `textFieldStyleModifier` to ensure consistent visual styling regardless
 * of the underlying text field implementation.
 *
 * ## How It Works
 * 1. Receives standard state parameters (error, enabled) and UI strings (title, supporting text).
 * 2. Manages the `MutableInteractionSource` to track focus state and handle formatting.
 * 3. Provides the shared style modifier and interaction source back to the caller through a composable
 *    lambda. The caller uses these to inject the specific text field (standard or secure) into the layout.
 *
 * @param title The label displayed above the text field container.
 * @param isError Boolean flag to toggle the error visual state.
 * @param supportingText Optional text displayed below the field (e.g., validation requirements or error messages).
 * @param enabled Boolean indicating if the field is interactive.
 * @param onFocusChanged Lambda triggered when the focus state of the field changes.
 * @param textField A composable lambda that injects the actual text field component, receiving the shared modifier and interaction source.
 */
@Composable
fun ChirpTextFieldLayout(
    title: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    textField: @Composable (Modifier, MutableInteractionSource) -> Unit,
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
    }

    val textFieldStyleModifier = Modifier
        .fillMaxWidth()
        .background(
            color = when {
                isFocused -> MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.05f,
                )
                enabled -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.extended.secondaryFill
            },
            shape = RoundedCornerShape(8.dp),
        )
        .border(
            width = 1.dp,
            color = when {
                isError -> MaterialTheme.colorScheme.error
                isFocused -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            },
            shape = RoundedCornerShape(8.dp),
        )
        .padding(12.dp)

    Column(
        modifier = modifier,
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.extended.textSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        textField(textFieldStyleModifier, interactionSource)

        if (supportingText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = supportingText,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.extended.textTertiary
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
