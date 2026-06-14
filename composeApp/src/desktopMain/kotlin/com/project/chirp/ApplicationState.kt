package com.project.chirp

import com.project.chirp.windows.WindowState

data class ApplicationState(
    val windows: List<WindowState> = listOf(WindowState()),
)
