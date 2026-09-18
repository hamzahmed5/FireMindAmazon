package com.firemind.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// FireMind brand palette - dark, high contrast, TV-safe.
val Brand = Color(0xFFFF6B35)
val BrandDeep = Color(0xFFB33E12)
val Background = Color(0xFF0B0E14)
val Surface = Color(0xFF141A24)
val SurfaceVariant = Color(0xFF1E2733)
val SurfaceRaised = Color(0xFF232E3D)
val TextPrimary = Color(0xFFF5F7FA)
val TextSecondary = Color(0xFF9AA5B1)
val FocusBorder = Color(0xFFFFD166)
val RatingStar = Color(0xFFF5C518)

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

/**
 * The backdrop every screen sits on: a very dark wash with a slow brand-colour
 * glow entering from the top-left. Keeps large areas from reading as flat
 * black on a TV panel while text contrast stays near 15:1.
 */
@Composable
fun GradientBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(BrandDeep.copy(alpha = 0.28f), Color.Transparent),
                    radius = 1400f,
                )
            )
            .background(Background)
    ) {
        content()
    }
}
