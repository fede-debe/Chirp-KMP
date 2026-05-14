package com.project.chirp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.project.auth.presentation.register.RegisterRoot
import com.project.core.designsystem.theme.ChirpTheme

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun App() {
    ChirpTheme {
        RegisterRoot()
    }
}
