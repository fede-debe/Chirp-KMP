package com.project.chirp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.project.chirp.navigation.NavigationRoot
import com.project.core.designsystem.theme.ChirpTheme

@Composable
@Preview
fun App() {
    ChirpTheme {
        NavigationRoot()
    }
}
