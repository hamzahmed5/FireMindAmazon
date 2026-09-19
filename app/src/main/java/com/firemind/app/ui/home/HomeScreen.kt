package com.firemind.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firemind.app.FireMindViewModel
import com.firemind.app.data.CatalogRepository
import com.firemind.app.ui.common.MoodTile
import com.firemind.app.ui.common.PosterCard
import com.firemind.app.ui.common.SectionHeader
import com.firemind.app.ui.theme.Cyan
import com.firemind.app.ui.theme.Outfit
import com.firemind.app.ui.theme.SlatePanel
import com.firemind.app.ui.theme.TextSoft

/**
 * Home: the 10-second story, on the Stitch "Cinematic AI Television" system.
 * A glowing prompt bar (one press opens the Ask screen), quick moods that
 * query immediately, and the recommended rail. Focus lands on the prompt bar
 * first so the demo starts with a single D-pad press.
 */
@Composable
fun HomeScreen(
    viewModel: FireMindViewModel,
    onAsk: (String) -> Unit,
    onOpenDetails: (String) -> Unit
) {
    val askFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { askFocus.requestFocus() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                // The prompt bar. Focusable surface styled like the Stitch
                // mock: dark field, cyan ring when focused, mic-less on TV.
                Surface(
                    onClick = { onAsk("") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(askFocus),
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SlatePanel,
                        focusedContainerColor = SlatePanel
                    )
                ) {
                    Text(
                        "What to watch? Speak or type...",
                        fontSize = 26.sp,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.Medium,
                        color = TextSoft,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tell FireMind your mood, your time budget, or nothing at all.",
                    fontSize = 17.sp,
                    fontFamily = Outfit,
                    color = TextSoft
                )
            }
        }
        item {
            Column {
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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CatalogRepository.MOOD_CHIPS) { mood ->
                        MoodTile(label = mood, onClick = { onAsk(mood) })
                    }
                }
            }
        }
        item {
            Column {
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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(viewModel.catalogMovies.take(10)) { movie ->
                        PosterCard(
                            title = movie.title,
                            year = movie.year,
                            runtime = movie.runtime,
                            onClick = { onOpenDetails(movie.id) }
                        )
                    }
                }
            }
        }
    }
}
