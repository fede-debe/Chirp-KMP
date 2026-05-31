package com.project.core.designSystem.components.layouts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.project.core.designSystem.components.brand.ChirpBrandLogo
import com.project.core.designSystem.theme.ChirpTheme
import com.project.core.designSystem.theme.extended
import com.project.core.presentation.util.DeviceConfiguration
import com.project.core.presentation.util.clearFocusOnTap
import com.project.core.presentation.util.currentDeviceConfiguration

/**
 * A responsive layout wrapper that arranges form-like content (login, registration, etc.) consistently
 * across different device configurations (mobile portrait, mobile landscape, tablet, and desktop).
 *
 * ## Strategy / Decisions
 * - **Adaptive Rendering:** Centralizes form layout responsiveness by reading `currentDeviceConfiguration()` and branching into three distinct UI archetypes. This prevents form logic duplication across different screen sizes.
 * - **Module Architecture:** A conscious decision was made to add `implementation(projects.core.presentation)` to the `designsystem` build.gradle.kts. This provides access to presentation utilities (like device configuration) without causing a circular dependency, as the presentation layer does not arrange design system components.
 * - **Flexible Branding:** The `logo` is passed as a `Composable` lambda rather than hardcoding a specific brand asset inside the design system module. This ensures the design system remains agnostic and reusable across different apps with different branding.
 * - **Centralized Error Handling:** Uses a shared `AuthHeaderSection` internally to handle the `errorText` via `AnimatedVisibility`, ensuring the appearance and disappearance of error states are uniformly animated across all device layouts.
 *
 * ## How It Works
 * 1. **Configuration Resolution:** Retrieves the current window configuration to determine the appropriate layout branch and conditionally sets the header text color (e.g., handling dark backgrounds in landscape).
 * 2. **Mobile Portrait:** Employs a full-width `ChirpSurface` that consumes window insets (navigation bars, display cutouts), vertically stacking the logo, header section, and form content inside a scrollable column.
 * 3. **Mobile Landscape:** Implements a two-column `Row` layout evenly split (`weight(1f)`). The left pane holds the logo and header; the right pane contains the form content within a `ChirpSurface`.
 * 4. **Tablet & Desktop:** Centers the content in the middle of the screen using a constrained `Column` (max width 480dp) with heavy rounded corners (32dp) and a surface background, mirroring standard web form behaviors.
 *
 * ## Alternatives / Why Not
 * - **Hardcoding the Logo:** Rejected defining the app logo directly in the design system or presentation module to avoid tying the reusable library to a specific app's copyright or context.
 * - **Shared Multiplatform Previews:** Due to current limitations in Compose Multiplatform regarding orientation and screen size preview annotations, generating multi-device previews within shared code was rejected. Previews were instead delegated to the `androidMain` source set using AndroidX Tooling (`@PreviewScreenSizes`).
 *
 * Technical Details:
 * - Thread Safety: Standard Compose thread safety applies (run on Main/UI thread).
 * - Constraints: Tablet/Desktop central form container is hard-capped at a maximum width of 480dp.
 * - Insets: Explicitly consumes `WindowInsets.navigationBars` and `WindowInsets.displayCutout` to prevent content overlap with system UI or camera hardware.
 *
 * @param headerText The primary title displayed at the top of the form (e.g., "Welcome to Chirp").
 * @param modifier The standard Compose modifier applied to the root layout.
 * @param errorText An optional error message displayed directly below the header. Animates in/out if null.
 * @param logo A composable lambda providing the specific brand logo to display.
 * @param formContent A composable lambda with `ColumnScope` containing the actual input fields and buttons.
 */
@Composable
fun ChirpAdaptiveFormLayout(
    headerText: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    logo: @Composable () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit,
) {
    val configuration = currentDeviceConfiguration()
    val headerColor = if (configuration == DeviceConfiguration.MOBILE_LANDSCAPE) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.extended.textPrimary
    }

    when (configuration) {
        DeviceConfiguration.MOBILE_PORTRAIT -> {
            MobilePortraitConfigSurface(
                headerText = headerText,
                headerColor = headerColor,
                errorText = errorText,
                logo = logo,
                formContent = formContent,
                modifier = modifier,
            )
        }

        DeviceConfiguration.MOBILE_LANDSCAPE -> {
            MobileLandscapeConfigSurface(
                headerText = headerText,
                headerColor = headerColor,
                errorText = errorText,
                logo = logo,
                formContent = formContent,
                modifier = modifier,
            )
        }

        DeviceConfiguration.TABLET_PORTRAIT,
        DeviceConfiguration.TABLET_LANDSCAPE,
        DeviceConfiguration.DESKTOP,
        -> {
            TabletDesktopConfigSurface(
                headerText = headerText,
                headerColor = headerColor,
                errorText = errorText,
                logo = logo,
                formContent = formContent,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun MobilePortraitConfigSurface(
    headerText: String,
    headerColor: Color,
    errorText: String?,
    logo: @Composable () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    ChirpSurface(
        modifier = modifier
            .clearFocusOnTap()
            .consumeWindowInsets(WindowInsets.navigationBars)
            .consumeWindowInsets(WindowInsets.displayCutout),
        header = {
            Spacer(modifier = Modifier.height(32.dp))
            logo()
            Spacer(modifier = Modifier.height(32.dp))
        },
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        AuthHeaderSection(
            headerText = headerText,
            headerColor = headerColor,
            errorText = errorText,
        )
        Spacer(modifier = Modifier.height(24.dp))
        formContent()
    }
}

@Composable
private fun MobileLandscapeConfigSurface(
    headerText: String,
    headerColor: Color,
    errorText: String?,
    logo: @Composable () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .consumeWindowInsets(WindowInsets.displayCutout)
            .consumeWindowInsets(WindowInsets.navigationBars),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            logo()
            AuthHeaderSection(
                headerText = headerText,
                headerColor = headerColor,
                errorText = errorText,
                headerTextAlignment = TextAlign.Start,
            )
        }
        ChirpSurface(
            modifier = Modifier.weight(1f),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            formContent()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TabletDesktopConfigSurface(
    headerText: String,
    headerColor: Color,
    errorText: String?,
    logo: @Composable () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        logo()
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthHeaderSection(
                headerText = headerText,
                headerColor = headerColor,
                errorText = errorText,
            )
            formContent()
        }
    }
}

@Composable
fun ColumnScope.AuthHeaderSection(
    headerText: String,
    headerColor: Color,
    errorText: String? = null,
    headerTextAlignment: TextAlign = TextAlign.Center,
) {
    Text(
        text = headerText,
        style = MaterialTheme.typography.titleLarge,
        color = headerColor,
        textAlign = headerTextAlignment,
        modifier = Modifier.fillMaxWidth(),
    )
    AnimatedVisibility(
        visible = errorText != null,
    ) {
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = headerTextAlignment,
            )
        }
    }
}

class ThemePreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Composable
@PreviewScreenSizes
@Preview
fun ChirpAdaptiveFormLayoutPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) isDarkTheme: Boolean,
) {
    ChirpTheme(darkTheme = isDarkTheme) {
        ChirpAdaptiveFormLayout(
            headerText = "Welcome to Chirp!",
            errorText = "Login failed!",
            logo = { ChirpBrandLogo() },
            formContent = {
                Text(
                    text = "Sample form title",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Sample form title 2",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
}
