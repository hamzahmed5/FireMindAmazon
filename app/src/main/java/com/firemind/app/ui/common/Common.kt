package com.firemind.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.firemind.app.ui.theme.Brand
import com.firemind.app.ui.theme.FocusBorder
import com.firemind.app.ui.theme.RatingStar
import com.firemind.app.ui.theme.SurfaceRaised
import com.firemind.app.ui.theme.SurfaceVariant
import com.firemind.app.ui.theme.TextSecondary

/**
 * Shared TV navigation rail. Every destination is reachable with
 * LEFT/RIGHT focus movement; the focused item is always obvious, and the
 * current section is highlighted so you always know where you are.
 */
@Composable
fun NavRail(
    current: String,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(112.dp),
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(Brand, FocusBorder)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("FM", color = androidx.compose.ui.graphics.Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.height(16.dp))
            NavRailItem("Home", current == "home") { onSelect("home") }
            NavRailItem("Ask", current == "assistant") { onSelect("assistant") }
            NavRailItem("Browse", current == "browse") { onSelect("browse") }
            NavRailItem("Watchlist", current == "watchlist") { onSelect("watchlist") }
            NavRailItem("About", current == "settings") { onSelect("settings") }
        }
    }
}

@Composable
private fun NavRailItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(96.dp)
            .height(56.dp)
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) FocusBorder else TextSecondary
        )
    }
}

/** Full-screen loading state; shown instead of freezing during async work. */
@Composable
fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 26.sp, color = TextSecondary)
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
        Text("Something went wrong", fontSize = 30.sp)
        Box(Modifier.height(12.dp))
        Text(message, fontSize = 20.sp, color = TextSecondary)
        Box(Modifier.height(24.dp))
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
        Text(title, fontSize = 30.sp)
        Box(Modifier.height(12.dp))
        Text(subtitle, fontSize = 20.sp, color = TextSecondary)
    }
}

/**
 * Screen title with a short brand rule underneath - the consistent header
 * every content screen shares.
 */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    Column {
        Text(title, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .padding(top = 6.dp)
                .width(56.dp)
                .height(4.dp)
                .background(Brand, RoundedCornerShape(2.dp))
        )
        if (subtitle != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
            Text(subtitle, fontSize = 18.sp, color = TextSecondary)
        }
    }
}

/** Small rounded label. The AI/curated badge keeps the app's honesty visible. */
@Composable
fun Badge(text: String, highlighted: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (highlighted) Brand else SurfaceVariant,
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) androidx.compose.ui.graphics.Color.Black else TextSecondary
        )
    }
}

/** Star + rating, the only place the star colour is allowed. */
@Composable
fun RatingPill(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("★", fontSize = 16.sp, color = RatingStar)
        Text(" $rating", fontSize = 16.sp, color = TextSecondary)
    }
}

/** Poster-style card used by every rail of titles across the app. */
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
            .width(220.dp)
            .height(150.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceRaised,
            focusedContainerColor = SurfaceVariant
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Art zone: a title-coloured wash standing in for artwork.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(SurfaceVariant, titleArtColor(title))
                        )
                    )
            ) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$year", fontSize = 15.sp, color = TextSecondary)
                Text("$runtime min", fontSize = 15.sp, color = TextSecondary)
            }
        }
    }
}

/** Deterministic per-title tint so each card feels distinct without artwork. */
private fun titleArtColor(title: String) = when (title.length % 5) {
    0 -> Brand.copy(alpha = 0.55f)
    1 -> FocusBorder.copy(alpha = 0.40f)
    2 -> androidx.compose.ui.graphics.Color(0xFF3E6B8F)
    3 -> androidx.compose.ui.graphics.Color(0xFF7A4E8F)
    else -> androidx.compose.ui.graphics.Color(0xFF2F7A5B)
}
