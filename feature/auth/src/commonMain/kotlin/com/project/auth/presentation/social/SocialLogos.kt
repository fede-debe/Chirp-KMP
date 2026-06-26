package com.project.auth.presentation.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Brand logos for the social sign-in buttons, declared as [ImageVector]s in code so they work on
 * every Compose target without per-platform drawable resources. Provider brand guidelines require the
 * marks be shown unmodified, so the Google "G" keeps its four brand colors (rendered with [Image],
 * never tinted); the monochrome Apple mark is an [Icon] tinted to the button's content color.
 */

private val GoogleLogoVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "GoogleLogo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Blue
        addPath(
            pathData = PathParser().parsePathString(
                "M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 " +
                    "3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z",
            ).toNodes(),
            fill = SolidColor(Color(0xFF4285F4)),
        )
        // Green
        addPath(
            pathData = PathParser().parsePathString(
                "M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 " +
                    "0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z",
            ).toNodes(),
            fill = SolidColor(Color(0xFF34A853)),
        )
        // Yellow
        addPath(
            pathData = PathParser().parsePathString(
                "M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 " +
                    "10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z",
            ).toNodes(),
            fill = SolidColor(Color(0xFFFBBC05)),
        )
        // Red
        addPath(
            pathData = PathParser().parsePathString(
                "M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 " +
                    "3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z",
            ).toNodes(),
            fill = SolidColor(Color(0xFFEA4335)),
        )
    }.build()
}

private val AppleLogoVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "AppleLogo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M16.365 1.43c0 1.14-.493 2.27-1.177 3.08-.744.9-1.99 1.57-2.987 1.57-.12 " +
                    "0-.23-.02-.3-.03-.01-.06-.04-.22-.04-.39 0-1.15.572-2.27 1.206-2.98.804-.94 " +
                    "2.142-1.64 3.248-1.68.03.13.05.28.05.43zm4.565 15.71c-.03.07-.463 1.58-1.518 " +
                    "3.12-.945 1.34-1.94 2.71-3.43 2.71-1.517 0-1.9-.88-3.63-.88-1.698 0-2.302.91-3.67.91-1.377 " +
                    "0-2.332-1.26-3.428-2.8-1.287-1.82-2.323-4.63-2.323-7.28 0-4.28 2.797-6.55 " +
                    "5.552-6.55 1.448 0 2.675.95 3.6.95.865 0 2.222-1.01 3.902-1.01.613 0 2.886.06 " +
                    "4.374 2.19-.13.09-2.383 1.37-2.383 4.19 0 3.26 2.854 4.42 2.955 4.45z",
            ).toNodes(),
            fill = SolidColor(Color.Black),
        )
    }.build()
}

@Composable
fun GoogleLogo(modifier: Modifier = Modifier.size(18.dp)) {
    Image(
        imageVector = GoogleLogoVector,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun AppleLogo(modifier: Modifier = Modifier.size(18.dp)) {
    // Tint defaults to LocalContentColor (the button's content color), keeping the mark monochrome.
    Icon(
        imageVector = AppleLogoVector,
        contentDescription = null,
        modifier = modifier,
    )
}
