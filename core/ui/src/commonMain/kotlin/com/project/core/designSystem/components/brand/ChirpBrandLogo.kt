package com.project.core.designsystem.components.brand

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.project.core.ui.Res
import com.project.core.ui.logo_chirp
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ChirpBrandLogo(
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = vectorResource(Res.drawable.logo_chirp),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}
