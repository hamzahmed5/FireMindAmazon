package com.firemind.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.firemind.app.R

// ---------------------------------------------------------------------------
// FireMind "Cinematic AI Television" palette - taken from the Stitch design
// system (docs/design/cinematic_ai_television). Cyan is the focus signature,
// amber marks moods/favorites, everything sits on deep charcoal slate.
// ---------------------------------------------------------------------------
val Cyan = Color(0xFF00D2FF)          // primary: focus rings, active states
val CyanGlow = Color(0xFF2CE8F5)      // outer volumetric glow
val CyanDeep = Color(0xFF0090B8)      // pressed/darker cyan
val Amber = Color(0xFFFFB020)         // secondary: moods, favorites, warmth
val AmberDeep = Color(0xFFE5A93C)     // amber wash
val SlateCanvas = Color(0xFF15181E)   // app canvas
val SlatePanel = Color(0xFF1A1D24)    // translucent-ish panels
val SlateCard = Color(0xFF222630)     // elevated card base
val SlateHigh = Color(0xFF272A30)     // highest container
val TextBright = Color(0xFFFFFFFF)   // primary text (100% luminance)
val TextSoft = Color(0xFF9CA3AF)      // subtext/prompts
val GhostBorder = Color(0x1FFFFFFF)   // rgba(255,255,255,0.12)

// Legacy names kept so older screens keep compiling; mapped to the new system.
val Brand = Cyan
val BrandDeep = CyanDeep
val Background = SlateCanvas
val Surface = SlatePanel
val SurfaceVariant = SlateCard
val SurfaceRaised = SlateCard
val TextPrimary = TextBright
val TextSecondary = TextSoft
val FocusBorder = Cyan
val RatingStar = Amber
val Heading = TextBright

/** Outfit, the Stitch system's typeface, bundled in res/font. */
val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
)

private val FireMindColorScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Color(0xFF003543),
    secondary = Amber,
    onSecondary = Color(0xFF442B00),
    background = SlateCanvas,
    onBackground = TextBright,
    surface = SlatePanel,
    onSurface = TextBright,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSoft
)

/**
 * The TV theme. Every widget in this app is androidx.tv.material3, and those
 * read THIS theme - not the phone material theme below. Configure it
 * explicitly so "this app is always dark" is true regardless of system
 * setting (learned the hard way - see the dark-on-dark fix).
 */
private val FireMindTvColorScheme = androidx.tv.material3.darkColorScheme(
    primary = Cyan,
    onPrimary = Color(0xFF003543),
    secondary = Amber,
    onSecondary = Color(0xFF442B00),
    background = SlateCanvas,
    onBackground = TextBright,
    surface = SlatePanel,
    onSurface = TextBright,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSoft,
    border = Cyan,
    borderVariant = SlateHigh
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
 * The backdrop every screen sits on: the Stitch canvas with a faint cyan
 * energy field entering from the top - "light-absorbing charcoal" with just
 * enough glow to not read as a switched-off panel.
 */
@Composable
fun GradientBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Cyan.copy(alpha = 0.10f), Color.Transparent),
                    radius = 1600f,
                )
            )
            .background(SlateCanvas)
    ) {
        content()
    }
}

/** Deterministic per-title art tint - cool hues so amber stays special. */
fun titleArtColor(title: String): Color = when (title.length % 5) {
    0 -> Cyan.copy(alpha = 0.35f)
    1 -> CyanDeep.copy(alpha = 0.45f)
    2 -> Color(0xFF2F4858)
    3 -> Color(0xFF3A3550)
    else -> Color(0xFF23414A)
}
