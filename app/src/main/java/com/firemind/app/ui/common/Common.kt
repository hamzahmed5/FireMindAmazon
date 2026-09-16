package com.firemind.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.firemind.app.ui.theme.Brand
import com.firemind.app.ui.theme.FocusBorder
import com.firemind.app.ui.theme.TextSecondary

/**
 * Shared TV navigation rail. Every destination is reachable with
 * LEFT/RIGHT focus movement; the focused item is always obvious.
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
                    .background(Brand, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("FM", color = androidx.compose.ui.graphics.Color.Black, fontSize = 16.sp)
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
