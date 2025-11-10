package com.mobilectl.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Premium Light Theme Colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1), // Vibrant indigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),

    secondary = Color(0xFF8B5CF6), // Purple
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5F3FF),
    onSecondaryContainer = Color(0xFF5B21B6),

    tertiary = Color(0xFF06B6D4), // Cyan
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECFEFF),
    onTertiaryContainer = Color(0xFF164E63),

    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),

    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1F2937),

    surface = Color.White,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B7280),

    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFF3F4F6),
)

// Premium Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8), // Softer indigo for dark mode
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),

    secondary = Color(0xFFA78BFA), // Lighter purple
    onSecondary = Color(0xFF4C1D95),
    secondaryContainer = Color(0xFF6D28D9),
    onSecondaryContainer = Color(0xFFF3E8FF),

    tertiary = Color(0xFF22D3EE), // Bright cyan
    onTertiary = Color(0xFF164E63),
    tertiaryContainer = Color(0xFF0E7490),
    onTertiaryContainer = Color(0xFFCFFAFE),

    error = Color(0xFFF87171),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFECECA),

    background = Color(0xFF0F172A), // Deep slate
    onBackground = Color(0xFFF1F5F9),

    surface = Color(0xFF1E293B), // Slate surface
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),

    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
)

// Premium gradient colors
object GradientColors {
    val primaryGradient = listOf(
        Color(0xFF6366F1),
        Color(0xFF8B5CF6)
    )

    val successGradient = listOf(
        Color(0xFF10B981),
        Color(0xFF06B6D4)
    )

    val warningGradient = listOf(
        Color(0xFFF59E0B),
        Color(0xFFEF4444)
    )

    val accentGradient = listOf(
        Color(0xFFEC4899),
        Color(0xFF8B5CF6)
    )
}

fun Brush.Companion.premiumGradient(colors: List<Color>) = Brush.horizontalGradient(colors)

@Composable
fun MobileCtlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
