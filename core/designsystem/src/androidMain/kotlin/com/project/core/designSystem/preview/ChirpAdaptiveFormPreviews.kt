package com.project.core.designSystem.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.project.core.designSystem.components.brand.ChirpBrandLogo
import com.project.core.designSystem.components.layouts.ChirpAdaptiveFormLayout
import com.project.core.designSystem.theme.ChirpTheme

@Composable
@PreviewLightDark
@PreviewScreenSizes
fun ChirpAdaptiveFormLayoutLightPreview() {
    ChirpTheme {
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
