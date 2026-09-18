package com.firemind.app.ui.home

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
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firemind.app.FireMindViewModel
import com.firemind.app.data.CatalogRepository
import com.firemind.app.ui.common.Badge
import com.firemind.app.ui.common.PosterCard
import com.firemind.app.ui.common.ScreenHeader
import com.firemind.app.ui.theme.Brand
import com.firemind.app.ui.theme.TextSecondary

/**
 * Home: the 10-second story. Ask FireMind, quick moods, and a browse rail.
 * Focus lands on [Ask FireMind] first so the demo starts with one D-pad press.
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
            .padding(horizontal = 40.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text("What do you want to watch?", fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Badge("60 original titles", highlighted = false)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Tell FireMind your mood, your time budget, or nothing at all.",
                        fontSize = 19.sp,
                        color = TextSecondary
                    )
                }
            }
        }
        item {
            Button(
                onClick = { onAsk("") },
                modifier = Modifier.focusRequester(askFocus)
            ) {
                Text("Ask FireMind", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column {
                Text("Quick moods", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CatalogRepository.MOOD_CHIPS) { mood ->
                        Button(onClick = { onAsk(mood) }) {
                            Text(mood, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
        item {
            Column {
                Text("Recommended for you", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
