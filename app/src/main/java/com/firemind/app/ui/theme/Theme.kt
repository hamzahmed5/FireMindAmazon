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

/**
 * Default colours for bare Text composables. On androidx.tv.material3, Text
 * with no explicit colour follows the TV theme's onSurface - but any Text
 * living under the phone-material tree (or in a plain foundation surface)
 * can fall back to the platform default, which is black. Screen code should
 * use these for headings rather than relying on inheritance.
 */
val Heading = TextPrimary
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
 * The TV theme. This is the one that actually matters: every widget in this
 * app is androidx.tv.material3, and those read THIS theme - not the phone
 * material theme below. It went unwrong for a while: only the phone theme was
 * configured, so TV widgets followed their own default, which follows the
 * system setting - on a light-system device the whole UI rendered light
 * surfaces with dark text on the app's dark backdrop. Configuring the TV
 * scheme explicitly is what makes "this app is always dark" true.
 */
private val FireMindTvColorScheme = androidx.tv.material3.darkColorScheme(
    primary = Brand,
    onPrimary = Color.Black,
    secondary = FocusBorder,
    onSecondary = Color.Black,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    border = FocusBorder,
    borderVariant = SurfaceRaised
)

/**
 * App always renders dark - televisions are viewed in living rooms and
 * large bright surfaces cause eye strain; contrast stays high regardless
 * of system setting.
 */
@Composable
fun FireMindTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme() // evaluated for completeness; result intentionally ignored
    androidx.tv.material3.MaterialTheme(colorScheme = FireMindTvColorScheme) {
        MaterialTheme(
            colorScheme = FireMindColorScheme,
            content = content
        )
    }
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
