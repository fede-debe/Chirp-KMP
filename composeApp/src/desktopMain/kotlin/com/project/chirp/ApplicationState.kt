package com.project.chirp

import androidx.compose.ui.window.TrayState
import com.project.chirp.windows.WindowState
import com.project.core.domain.preferences.ThemePreference

data class ApplicationState(
    val windows: List<WindowState> = listOf(WindowState()),
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val trayState: TrayState = TrayState(),
)
