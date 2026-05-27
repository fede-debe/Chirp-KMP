package com.project.chirp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.project.chirp.navigation.DeepLinkListener
import com.project.chirp.navigation.NavigationRoot
import com.project.core.designsystem.theme.ChirpTheme

@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    DeepLinkListener(navController)

    ChirpTheme {
        NavigationRoot(navController)
    }
}
