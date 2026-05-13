package com.project.core.designsystem.components.layouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chirp.core.designsystem.generated.resources.Res
import chirp.core.designsystem.generated.resources.logo_chirp
import com.project.core.designsystem.theme.ChirpTheme
import org.jetbrains.compose.resources.vectorResource

/**
 * A foundational slot-based layout for the custom design system, functioning similarly to a
 * Material Design scaffold. It structures repeating UI elements (like the background and top-rounded
 * content sheet) while allowing flexible content injection.
 *
 * ## Strategy / Decisions
 * - **Slot-Based Architecture:** Just like Material 3's Scaffold uses existing components (toolbars, FABs)
 *   in intended ways, `ChirpSurface` encapsulates the repetitive boilerplate of the app's main screens
 *   (like login or registration). It handles the overall container structure, allowing the caller to
 *   only worry about the dynamic slots (header and content).
 * - **Scope Delegation:** Both the `header` and `content` lambdas extend `ColumnScope`. This is a
 *   purposeful API design choice to communicate layout intent directly to the caller, preventing them
 *   from unnecessarily wrapping their provided content in redundant `Column` components.
 * - **Theme Consistency:** Automatically accommodates light and dark theme aesthetics by correctly
 *   separating the root layer (`colorScheme.background`) from the inner content sheet layer
 *   (`colorScheme.surface`).
 *
 * ## How It Works
 * 1. **Root Container:** A base Material `Surface` is created using the theme's background color.
 * 2. **Alignment Wrapper:** A `Column` filling the maximum size is placed inside, configured to
 *    horizontally center all its children (`Alignment.CenterHorizontally`).
 * 3. **Header Slot:** The optional `header` lambda is invoked at the top of the column (typically
 *    used for the app logo in portrait mode, but omitted in landscape).
 * 4. **Content Sheet:** A secondary `Surface` is rendered below the header using the theme's surface color.
 *    It is assigned a `weight(1f)` so it dynamically consumes all remaining vertical space.
 * 5. **Content Slot:** Inside the content sheet, a `Column` filling the max size invokes the main
 *    `content` lambda.
 *
 * ## Alternatives / Why Not
 * - **BoxScope vs. ColumnScope for Content:** Initially, `BoxScope` was considered for the `content`
 *   slot to allow standard 2D alignment. However, this was rejected in favor of `ColumnScope` because
 *   the anticipated UI components (registration, login forms, etc.) inherently follow a vertical,
 *   column-wise flow.
 * - **Modifier.clip vs. Surface Shape Parameter:** While `Modifier.clip(RoundedCornerShape(...))`
 *   was briefly considered to round the top corners of the inner sheet, it was decided to pass the
 *   shape directly to the `Surface` component's built-in `shape` parameter to adhere to the component's
 *   intended API usage.
 * - **Root Vertical Centering:** Explicit vertical centering (`Arrangement.Center`) on the root column
 *   was considered but discarded as unnecessary; the root column fills the max size and the inner
 *   sheet's `weight(1f)` naturally forces the layout to distribute vertical space properly.
 *
 * Technical Details:
 * - Requires an active MaterialTheme context to resolve `colorScheme.background`, `colorScheme.surface`,
 *   and `colorScheme.primary`.
 * - Top corners of the content sheet are defined with a 20dp radius (`topStart` and `topEnd`).
 *
 * @param modifier The modifier to be applied to the root layout surface.
 * @param header An optional composable slot for top-level elements (e.g., logos). Runs inside a [ColumnScope].
 * @param content The main required body of the screen, placed within a distinct surface sheet. Runs inside a [ColumnScope].
 */
@Composable
fun ChirpSurface(
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize(),
        ) {
            header()
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
@Preview
fun ChirpSurfacePreview() {
    ChirpTheme {
        ChirpSurface(
            modifier = Modifier
                .fillMaxSize(),
            header = {
                Icon(
                    imageVector = vectorResource(Res.drawable.logo_chirp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(vertical = 32.dp),
                )
            },
            content = {
                Text(
                    text = "Welcome to Chirp!",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(vertical = 40.dp)
                        .align(Alignment.CenterHorizontally),
                )
            },
        )
    }
}
