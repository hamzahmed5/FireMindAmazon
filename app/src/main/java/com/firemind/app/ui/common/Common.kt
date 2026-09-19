package com.firemind.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.flow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.firemind.app.ui.theme.Amber
import com.firemind.app.ui.theme.Cyan
import com.firemind.app.ui.theme.Heading
import com.firemind.app.ui.theme.Outfit
import com.firemind.app.ui.theme.SlateCard
import com.firemind.app.ui.theme.SlateHigh
import com.firemind.app.ui.theme.SlatePanel
import com.firemind.app.ui.theme.TextBright
import com.firemind.app.ui.theme.TextSecondary
import com.firemind.app.ui.theme.TextSoft
import com.firemind.app.ui.theme.titleArtColor

/** Full-screen loading state; shown instead of freezing during async work. */
@Composable
fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 26.sp, color = TextSoft, fontFamily = Outfit)
    }
}

/** Full-screen error state with the reason and a retry action. */
@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", fontSize = 30.sp, color = Heading, fontFamily = Outfit, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(message, fontSize = 20.sp, color = TextSoft, fontFamily = Outfit)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Try again", fontSize = 20.sp) }
    }
}

/** Empty-state helper (watchlist with no saved titles, no results, etc). */
@Composable
fun EmptyScreen(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 30.sp, color = Heading, fontFamily = Outfit, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(subtitle, fontSize = 20.sp, color = TextSoft, fontFamily = Outfit)
    }
}

/**
 * The Stitch top navigation bar: brand + Online pill on the left, the five
 * destinations across the middle. The focused tab gets the cyan signature;
 * the selected tab keeps a soft cyan underline so you always know where you
 * are. Same destinations and D-pad behavior as the old side rail - pure
 * restyle. BACK from any screen still pops the navigation stack.
 */
@Composable
fun TopNav(
    current: String,
    online: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Stitch mock shows a live clock in the top bar - TV idles on this
    // screen, so minute granularity is right. Recomputes every 30s.
    val clock = remember {
        flow {
            while (true) {
                val now = java.util.Calendar.getInstance()
                val h = now.get(java.util.Calendar.HOUR)
                val h12 = if (h == 0) 12 else h
                val m = now.get(java.util.Calendar.MINUTE)
                val ampm = if (now.get(java.util.Calendar.AM_PM) == java.util.Calendar.PM) "PM" else "AM"
                emit(String.format("%d:%02d %s", h12, m, ampm))
                kotlinx.coroutines.delay(30_000)
            }
        }
    }.collectAsState(initial = "")
    val items = listOf(
        "home" to "ASK",
        "browse" to "DISCOVER",
        "watchlist" to "WATCHLIST",
        "settings" to "SETTINGS"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SlatePanel)
            .padding(horizontal = 32.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand: white "FIREMIND" + cyan "AI".
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "FIREMIND",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = Outfit,
                color = TextBright,
                letterSpacing = 1.2.sp
            )
            Text(
                "AI",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = Outfit,
                color = Cyan,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.width(14.dp))
            // Online pill: cyan when the backend answers, grey when not.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        if (online) Cyan.copy(alpha = 0.14f) else SlateCard,
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            if (online) Cyan else TextSoft,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    if (online) "Online" else "Offline",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Outfit,
                    color = if (online) Cyan else TextSoft
                )
            }
        }

        // Destination tabs.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            items.forEach { (route, label) ->
                val selected = current == route
                Surface(
                    onClick = { onSelect(route) },
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = SlateCard,
                        contentColor = if (selected) TextBright else TextSoft,
                        focusedContentColor = TextBright
                    ),
                    scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 17.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = Outfit,
                            letterSpacing = 1.6.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .background(
                                    if (selected) Cyan else Color.Transparent,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }

        // Live clock, right-aligned per the Stitch mock.
        Text(
            clock.value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = Outfit,
            color = TextSoft,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Section header in the Stitch "title-tv" style: uppercase, bold, white,
 * with the amber marker this design system reserves for curation.
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = Outfit,
        letterSpacing = 1.6.sp,
        color = TextBright,
        modifier = modifier
    )
}

/**
 * Screen title with the brand rule underneath - cyan for the app's own
 * headers, per the Stitch hierarchy.
 */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    Column {
        Text(
            title,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = Outfit,
            color = Heading
        )
        Box(
            Modifier
                .padding(top = 8.dp)
                .width(56.dp)
                .height(4.dp)
                .background(Cyan, RoundedCornerShape(2.dp))
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, fontSize = 18.sp, color = TextSoft, fontFamily = Outfit)
        }
    }
}

/** Small rounded label. The AI badge keeps the app's honesty visible. */
@Composable
fun Badge(text: String, highlighted: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (highlighted) Cyan else SlateCard,
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Outfit,
            letterSpacing = 0.8.sp,
            color = if (highlighted) androidx.compose.ui.graphics.Color(0xFF003543) else TextSoft
        )
    }
}

/** Star + rating. Amber is this design system's warmth accent. */
@Composable
fun RatingPill(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("★", fontSize = 16.sp, color = Amber)
        Text(" $rating", fontSize = 16.sp, color = TextSecondary, fontFamily = Outfit, fontWeight = FontWeight.Medium)
    }
}

/**
 * 16:9 media card per the Stitch spec: art zone with a vignette, resting
 * state has no border; focused state is drawn by the TV focus system.
 */
@Composable
fun PosterCard(
    title: String,
    year: Int,
    runtime: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(240.dp)
            .height(150.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SlateCard,
            focusedContainerColor = SlateHigh
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(
                        Brush.verticalGradient(
                            listOf(titleArtColor(title), androidx.compose.ui.graphics.Color(0xE6101319))
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Outfit,
                    color = TextBright,
                    maxLines = 2,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
            Row(
                Modifier
                    .background(SlateCard)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$year", fontSize = 14.sp, color = TextSoft, fontFamily = Outfit)
                Text("$runtime min", fontSize = 14.sp, color = TextSoft, fontFamily = Outfit)
            }
        }
    }
}

/**
 * Wide cinematic mood tile (Stitch "Mood Chips"): full-bleed tinted field,
 * dead-center uppercase label, amber wash when active - amber is reserved
 * for exactly this active-state warmth.
 */
@Composable
fun MoodTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(170.dp)
            .height(96.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SlateCard,
            focusedContainerColor = SlateHigh
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(titleArtColor(label), androidx.compose.ui.graphics.Color(0xCC15181E))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label.uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = Outfit,
                letterSpacing = 1.4.sp,
                color = TextBright
            )
        }
    }
}
