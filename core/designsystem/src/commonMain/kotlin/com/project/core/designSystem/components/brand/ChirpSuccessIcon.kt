package com.project.core.designsystem.components.brand

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.project.core.designsystem.Res
import com.project.core.designsystem.success_checkmark
import com.project.core.designsystem.theme.extended
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ChirpSuccessIcon(
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = vectorResource(Res.drawable.success_checkmark),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.extended.success,
        modifier = modifier,
    )
}
