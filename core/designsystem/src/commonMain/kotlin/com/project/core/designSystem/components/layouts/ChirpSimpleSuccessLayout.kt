package com.project.core.designSystem.components.layouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.project.core.designSystem.components.brand.ChirpSuccessIcon
import com.project.core.designSystem.components.buttons.ChirpButton
import com.project.core.designSystem.components.buttons.ChirpButtonStyle
import com.project.core.designSystem.theme.ChirpTheme
import com.project.core.designSystem.theme.extended

/**
 * A slot-based layout designed to display consistent success messages (e.g., successful account creation or verification) across the application.
 * It provides a standardized, centralized column structure for an icon, text content, and action buttons.
 *
 * ## Strategy / Decisions
 * - **Slot-Based API:** Chosen to make the layout highly reusable. By accepting composables for the icon and buttons, the layout remains agnostic to the specific context (like login vs. resend email actions) while enforcing a strict visual arrangement.
 * - **Visual Overlap:** The design requires the text content to slightly overlap the top icon. This is achieved strategically by wrapping the text and buttons in a nested `Column` and applying a negative Y-axis offset (`-25.dp`), rather than relying on complex custom layout measurements.
 * - **Static Adaptive Nature:** Unlike other core layouts in the design system, this specific layout does not implement different variants based on screen configuration; it remains a simple, uniform column layout across form factors.
 *
 * ## How It Works
 * 1. A root `Column` is established with 16dp horizontal padding and centered horizontal alignment.
 * 2. The top-level `icon` composable slot is rendered at the top of the root column.
 * 3. A nested `Column` is created with `fillMaxWidth` and an explicit `offset(y = -25.dp)` to pull the subsequent content upwards, overlapping the icon.
 * 4. Inside the nested column, the `title` and `description` texts are rendered using specific Material typography (`titleLarge` and `bodySmall`) and extended color schemes (`textPrimary`, `textSecondary`).
 * 5. Manual `Spacer` components are used to define the vertical rhythm (8dp between text elements, 24dp before the primary button).
 * 6. The `primaryButton` slot is rendered.
 * 7. If provided, the `secondaryButton` slot is rendered alongside additional 8dp spacers.
 *
 * ## Alternatives / Why Not
 * - **Rejected `Arrangement.spacedBy()`:** Initially, applying `verticalArrangement = Arrangement.spacedBy(8.dp)` to the inner column was considered to handle spacing automatically. This was explicitly rejected because it applies blindly to all children, including explicit `Spacer` elements, which would result in unintended double-spacing (e.g., a 8dp spacer would actually create 24dp of space). Manual spacers were chosen for precise, granular control.
 *
 * ## Technical Details
 * - The `secondaryButton` is implemented as a nullable composable lambda (`@Composable (() -> Unit)? = null`) to allow optional rendering, as not all success screens require a secondary action.
 *
 * @param title The main heading text indicating the success state.
 * @param description Supporting text providing further details or instructions.
 * @param icon A composable slot for the top graphic, typically a success checkmark.
 * @param primaryButton A composable slot for the primary call-to-action button (e.g., "Log In").
 * @param secondaryButton An optional composable slot for an alternative action (e.g., "Resend verification email").
 */
@Composable
fun ChirpSimpleSuccessLayout(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    primaryButton: @Composable () -> Unit,
    secondaryButton: @Composable (() -> Unit)? = null,
    secondaryError: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -(25).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.extended.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))

            primaryButton()

            if (secondaryButton != null) {
                Spacer(modifier = Modifier.height(8.dp))
                secondaryButton()
                if (secondaryError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = secondaryError,
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

class SuccessLayoutPreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Composable
@Preview
fun ChirpSimpleSuccessLayoutPreview(
    @PreviewParameter(SuccessLayoutPreviewParameterProvider::class) isDarkTheme: Boolean,
) {
    ChirpTheme(darkTheme = isDarkTheme) {
        ChirpSimpleSuccessLayout(
            title = "Hello world!",
            description = "Test description",
            icon = {
                ChirpSuccessIcon()
            },
            primaryButton = {
                ChirpButton(
                    text = "Log In",
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            },
            secondaryButton = {
                ChirpButton(
                    text = "Resend verification email",
                    onClick = {},
                    style = ChirpButtonStyle.SECONDARY,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            },
        )
    }
}
