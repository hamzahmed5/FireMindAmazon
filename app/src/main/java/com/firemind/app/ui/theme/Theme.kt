package com.firemind.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// FireMind brand palette - dark, high contrast, TV-safe.
val Brand = Color(0xFFFF6B35)
val Background = Color(0xFF0B0E14)
val Surface = Color(0xFF141A24)
val SurfaceVariant = Color(0xFF1E2733)
val TextPrimary = Color(0xFFF5F7FA)
val TextSecondary = Color(0xFF9AA5B1)
val FocusBorder = Color(0xFFFFD166)

private val FireMindColorScheme = darkColorScheme(
    primary = Brand,
    onPrimary = Color.Black,
    secondary = FocusBorder,
    onSecondary = Color.Black,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary
)

/**
 * App always renders dark - televisions are viewed in living rooms and
 * large bright surfaces cause eye strain; contrast stays high regardless
 * of system setting.
 */
@Composable
fun FireMindTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme() // evaluated for completeness; result intentionally ignored
    MaterialTheme(
        colorScheme = FireMindColorScheme,
        content = content
    )
}
