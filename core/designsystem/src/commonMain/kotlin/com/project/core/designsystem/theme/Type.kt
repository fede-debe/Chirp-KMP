package com.project.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import chirp.core.designsystem.generated.resources.Res
import chirp.core.designsystem.generated.resources.plusjakartasans_bold
import chirp.core.designsystem.generated.resources.plusjakartasans_light
import chirp.core.designsystem.generated.resources.plusjakartasans_medium
import chirp.core.designsystem.generated.resources.plusjakartasans_regular
import chirp.core.designsystem.generated.resources.plusjakartasans_semibold
import org.jetbrains.compose.resources.Font

/**
 * Defines the custom typography and font families used across the multiplatform design system.
 *
 * ## Strategy / Decisions
 * To utilize custom fonts (Plus Jakarta Sans) across Android, iOS, and Desktop without writing
 * platform-specific code, we rely on Compose Multiplatform's resource generation. We map the
 * Figma design text styles directly to Material 3's `Typography` object. Because Material 3 lacks
 * certain styles defined by the designers (e.g., extra small labels), we extend the `Typography`
 * object using Kotlin extension properties to maintain a single, cohesive typography system.
 *
 * ## How It Works
 * 1. Triggers a project rebuild to allow Compose Resources to generate the platform-agnostic
 *    `Res.font` accessors.
 * 2. Creates a custom `FontFamily` mapping these generated resources to their respective `FontWeight`
 *    (Light, Normal, Medium, SemiBold, Bold).
 * 3. Constructs a `Typography` instance, directly porting font sizes (in SP) and line heights
 *    from the Figma mockups and binding them to our custom `FontFamily`.
 * 4. Adds extension properties (like `Typography.labelXSmall`) for custom styles not supported natively by M3.
 *
 * ## Alternatives / Why Not
 * If we mapped our custom fonts only to explicitly defined M3 text styles, utilizing default M3
 * composables (like `Text`) might fallback to the default system font (Roboto) if they reference
 * an unspecified text style (like `displayLarge`) under the hood.
 *
 * ## Technical Details
 * - Uses Scale-Independent Pixels (SP) to ensure text visually scales according to the user's
 *   system font size preferences.
 * - Font instantiations act as composable getters because `Font(Res.font...)` must be invoked
 *   within a composable context.
 */
val PlusJakartaSans @Composable get() = FontFamily(
    Font(
        resource = Res.font.plusjakartasans_light,
        weight = FontWeight.Light,
    ),
    Font(
        resource = Res.font.plusjakartasans_regular,
        weight = FontWeight.Normal,
    ),
    Font(
        resource = Res.font.plusjakartasans_medium,
        weight = FontWeight.Medium,
    ),
    Font(
        resource = Res.font.plusjakartasans_semibold,
        weight = FontWeight.SemiBold,
    ),
    Font(
        resource = Res.font.plusjakartasans_bold,
        weight = FontWeight.Bold,
    ),
)

val Typography.labelXSmall: TextStyle
    @Composable get() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

val Typography.titleXSmall: TextStyle
    @Composable get() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )

val Typography @Composable get() = Typography(
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)
