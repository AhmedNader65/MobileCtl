package com.mobilectl.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Minimalist Premium Theme - Inspired by Cursor, Vercel, Maestro Studio
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),           // Pure black
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF5F5F5),
    onPrimaryContainer = Color(0xFF171717),

    secondary = Color(0xFF404040),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFAFAFA),
    onSecondaryContainer = Color(0xFF262626),

    tertiary = Color(0xFF666666),
    onTertiary = Color(0xFFFFFFFF),

    background = Color(0xFFFFFFFF),         // Pure white
    onBackground = Color(0xFF0A0A0A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFFAFAFA),
    onSurfaceVariant = Color(0xFF737373),

    outline = Color(0xFFE5E5E5),
    outlineVariant = Color(0xFFF5F5F5),

    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),

    surfaceTint = Color.Transparent
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),           // Pure white
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Color(0xFFFAFAFA),

    secondary = Color(0xFFA3A3A3),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF171717),
    onSecondaryContainer = Color(0xFFD4D4D4),

    tertiary = Color(0xFF737373),
    onTertiary = Color(0xFF000000),

    background = Color(0xFF000000),         // Pure black
    onBackground = Color(0xFFFAFAFA),

    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF171717),
    onSurfaceVariant = Color(0xFFA3A3A3),

    outline = Color(0xFF262626),
    outlineVariant = Color(0xFF1A1A1A),

    error = Color(0xFFEF4444),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),

    surfaceTint = Color.Transparent
)

// Minimal accent colors for subtle highlights
object AccentColors {
    val success = Color(0xFF10B981)
    val successLight = Color(0xFFD1FAE5)
    val successDark = Color(0xFF065F46)

    val warning = Color(0xFFF59E0B)
    val warningLight = Color(0xFFFEF3C7)
    val warningDark = Color(0xFF92400E)

    val info = Color(0xFF3B82F6)
    val infoLight = Color(0xFFDBEAFE)
    val infoDark = Color(0xFF1E40AF)

    // Neutral palette
    val neutral50 = Color(0xFFFAFAFA)
    val neutral100 = Color(0xFFF5F5F5)
    val neutral200 = Color(0xFFE5E5E5)
    val neutral300 = Color(0xFFD4D4D4)
    val neutral400 = Color(0xFFA3A3A3)
    val neutral500 = Color(0xFF737373)
    val neutral600 = Color(0xFF525252)
    val neutral700 = Color(0xFF404040)
    val neutral800 = Color(0xFF262626)
    val neutral900 = Color(0xFF171717)
    val neutral950 = Color(0xFF0A0A0A)
}

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
