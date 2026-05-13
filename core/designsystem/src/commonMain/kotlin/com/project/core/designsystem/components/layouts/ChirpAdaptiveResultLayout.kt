package com.project.core.designsystem.components.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.core.designsystem.components.brand.ChirpBrandLogo
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.presentation.util.DeviceConfiguration
import com.project.core.presentation.util.currentDeviceConfiguration

/**
 * Provides an adaptive UI layout specifically designed for result-based screens (e.g., account successfully
 * created, verification sent) rather than form inputs.
 *
 * ## Strategy / Decisions
 * - **Adaptive Rendering:** The layout fundamentally changes based on device configuration to optimize screen real estate.
 * - **Mobile Portrait:** Utilizes a full-width `ChirpSurface`.
 * - **Tablet & Desktop:** Renders a constrained, boxed layout (max 480dp width) centered on the screen, accompanied by the brand logo at the top.
 * - **Mobile Landscape Exception:** Mirrors the tablet boxed layout but intentionally hides the top brand logo to conserve limited vertical space.
 * - **Separation from Form Layout:** A dedicated component was created because the `ChirpAdaptiveFormLayout` relies on a two-column row design (welcome text on the left, form on the right). Result screens require a strictly centered, single-column focus.
 *
 * ## How It Works
 * 1. Retrieves the `currentDeviceConfiguration`.
 * 2. Wraps the entire layout in a root `Scaffold` to automatically manage default window insets and retrieve `innerPadding`.
 * 3. **Portrait Flow:** If the device is in Mobile Portrait, it renders the standard `ChirpSurface` passing the padding, the `ChirpBrandLogo` surrounded by 32dp vertical spacers, and the injected `content` lambda.
 * 4. **Boxed Flow (Non-Portrait):** Renders a root `Column` that fills the maximum size with the theme's background color.
 * 5. Conditionally injects the `ChirpBrandLogo` only if the configuration is *not* Mobile Landscape.
 * 6. Renders the inner `Column` acting as the content surface, applying a 480dp maximum width constraint, 32dp rounded corners, a surface background, vertical scrolling, and centered alignments.
 *
 * ## Alternatives / Why Not
 * - **Applying Default Top Spacing to Surface:** The instructor considered adding default top spacing/padding directly to the `ChirpSurface` to give the content breathing room. This was rejected because result screens often require an "offset effect" where the status icon is pulled upward. Imposing strict top padding at the layout level would break this offset, so vertical spacing is delegated to the `content` lambda itself.
 *
 * ## Technical Details
 * - **Window Insets:** Handled automatically via the root `Scaffold`.
 * - **Width Constraints:** Uses `widthIn(max = 480.dp)` for the centered surface to ensure it wraps its content height but caps its horizontal footprint.
 * - **Scrollability:** The inner surface `Column` implements `verticalScroll` to prevent clipping on smaller landscape devices.
 *
 * @param content The composable lambda containing the actual result UI (e.g., status text, offset icons, and action buttons).
 */
@Composable
fun ChirpAdaptiveResultLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = currentDeviceConfiguration()

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        if (configuration == DeviceConfiguration.MOBILE_PORTRAIT) {
            ChirpSurface(
                modifier = Modifier
                    .padding(innerPadding),
                header = {
                    Spacer(modifier = Modifier.height(32.dp))
                    ChirpBrandLogo()
                    Spacer(modifier = Modifier.height(32.dp))
                },
                content = content,
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                if (configuration != DeviceConfiguration.MOBILE_LANDSCAPE) {
                    ChirpBrandLogo()
                }
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
@Preview
fun ChirpAdaptiveResultLayoutPreview() {
    ChirpTheme {
        ChirpAdaptiveResultLayout(
            modifier = Modifier
                .fillMaxSize(),
            content = {
                Text(
                    text = "Registration successful!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
}
