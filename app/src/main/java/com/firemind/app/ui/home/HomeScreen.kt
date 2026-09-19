package com.firemind.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.firemind.app.FireMindViewModel
import com.firemind.app.data.CatalogRepository
import com.firemind.app.ui.common.MoodTile
import com.firemind.app.ui.common.PosterCard
import com.firemind.app.ui.common.SectionHeader
import com.firemind.app.ui.theme.Cyan
import com.firemind.app.ui.theme.GhostBorder
import com.firemind.app.ui.theme.Outfit
import com.firemind.app.ui.theme.SlateCard
import com.firemind.app.ui.theme.SlateHigh
import com.firemind.app.ui.theme.SlatePanel
import com.firemind.app.ui.theme.TextBright
import com.firemind.app.ui.theme.TextSoft

/**
 * Home in the Stitch "Cinematic AI Television" layout: the content column on
 * the left, the glowing voice/insight panel on the right, and the Amazon-style
 * remote key hints along the bottom.
 */
@Composable
fun HomeScreen(
    viewModel: FireMindViewModel,
    onAsk: (String) -> Unit,
    onOpenDetails: (String) -> Unit
) {
    val askFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { askFocus.requestFocus() }
    val lastQuery by viewModel.lastQuery.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .weight(1f)
                .padding(horizontal = 48.dp, vertical = 20.dp)
        ) {
            // ---------------- LEFT: content column ----------------
            // Scrollable: TV focus brings clipped content into view as the
            // viewer moves down - the Fire TV home behaves the same way.
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Prompt bar - focus lands here first so the demo starts
                // with a single press.
                Surface(
                    onClick = { onAsk("") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(askFocus),
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SlatePanel,
                        focusedContainerColor = SlateHigh
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "What to watch? Speak or type...",
                            fontSize = 24.sp,
                            fontFamily = Outfit,
                            fontWeight = FontWeight.Medium,
                            color = TextSoft,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 28.dp, vertical = 16.dp)
                        )
                        WaveformIcon(Modifier.padding(end = 22.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tell FireMind your mood, your time budget, or nothing at all.",
                    fontSize = 16.sp,
                    fontFamily = Outfit,
                    color = TextSoft
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionHeader("Quick moods")
                    Text(
                        "Select to query automatically",
                        fontSize = 14.sp,
                        fontFamily = Outfit,
                        color = TextSoft
                    )
                }
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(CatalogRepository.MOOD_CHIPS) { index, mood ->
                        MoodTile(label = mood, badge = index + 1, onClick = { onAsk(mood) })
                    }
                }
                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionHeader("Recommended for you")
                    Text(
                        "View all (${viewModel.catalogMovies.size})",
                        fontSize = 14.sp,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        color = Cyan
                    )
                }
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(viewModel.catalogMovies.take(10)) { movie ->
                        PosterCard(
                            title = movie.title,
                            year = movie.year,
                            runtime = movie.runtime,
                            onClick = { onOpenDetails(movie.id) }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                ViewingProfileBar()
            }

            Spacer(Modifier.width(24.dp))

            // ---------------- RIGHT: voice/insight panel ----------------
            VoiceInsightPanel(
                lastQuery = lastQuery,
                onReRun = { onAsk(it) },
                modifier = Modifier.width(320.dp)
            )
        }

        KeyHintsFooter()
    }
}

/** Small cyan waveform - the mock's mic glyph, drawn (no emoji risk). */
@Composable
private fun WaveformIcon(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .size(48.dp)
            .background(SlateCard, CircleShape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        listOf(10, 20, 26, 20, 10).forEach { h ->
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(5.dp, h.dp)
                    .background(Cyan, RoundedCornerShape(2.dp))
            )
        }
    }
}

/** The mock's "Neural Context Profile" bar, honest to what the app does. */
@Composable
private fun ViewingProfileBar() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(SlatePanel, RoundedCornerShape(14.dp))
            .border(1.dp, GhostBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text("Viewing Profile:", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = Outfit, color = TextBright)
        Spacer(Modifier.width(14.dp))
        listOf("Family-safe", "Under 2 hours", "Feel-good").forEach { chip ->
            Box(
                Modifier
                    .padding(end = 10.dp)
                    .background(SlateCard, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(chip, fontSize = 13.sp, fontFamily = Outfit, color = TextSoft)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Set moods in DISCOVER", fontSize = 13.sp, fontFamily = Outfit, color = Cyan)
    }
}

/**
 * The mock's right-side panel: mic circle with cyan ring, "voice ready" pill,
 * honest tip text, and the previous-query Re-run card pinned at the bottom.
 */
@Composable
private fun VoiceInsightPanel(
    lastQuery: String?,
    onReRun: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(SlatePanel, RoundedCornerShape(18.dp))
            .border(1.dp, GhostBorder, RoundedCornerShape(18.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(130.dp)
                .background(
                    Brush.radialGradient(listOf(Cyan.copy(alpha = 0.22f), Color.Transparent)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .background(Color(0xFF0E2530), CircleShape)
                    .border(2.dp, Cyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                WaveformIcon()
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xFF0E2530), RoundedCornerShape(999.dp))
                .border(1.dp, Cyan.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Box(Modifier.size(7.dp).background(Cyan, CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(
                "AI VOICE READY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Outfit,
                letterSpacing = 1.4.sp,
                color = Cyan
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Speak with your remote mic, or type in the bar - FireMind reads mood, not titles.",
            fontSize = 14.sp,
            fontFamily = Outfit,
            color = TextSoft,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        if (lastQuery != null) {
            Surface(
                onClick = { onReRun(lastQuery) },
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = SlateCard,
                    focusedContainerColor = SlateHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "PREVIOUS QUERY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Outfit,
                        letterSpacing = 1.6.sp,
                        color = TextSoft
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "\"$lastQuery\"",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Outfit,
                        color = TextBright,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Cyan, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Re-run",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Outfit,
                            color = Color(0xFF003543)
                        )
                    }
                }
            }
        }
    }
}

/** Amazon-style remote key hints along the bottom edge. */
@Composable
private fun KeyHintsFooter() {
    val hints = listOf(
        "↑↓←→" to "Navigate",
        "OK" to "Select",
        "BACK" to "Cancel"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(SlatePanel.copy(alpha = 0.65f))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        hints.forEachIndexed { i, (key, label) ->
            if (i > 0) Spacer(Modifier.width(30.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    key,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Outfit,
                    color = TextBright,
                    modifier = Modifier
                        .background(SlateCard, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(label, fontSize = 13.sp, fontFamily = Outfit, color = TextSoft)
            }
        }
    }
}
