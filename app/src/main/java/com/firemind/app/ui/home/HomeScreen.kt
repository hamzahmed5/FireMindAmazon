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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firemind.app.FireMindViewModel
import com.firemind.app.data.CatalogRepository
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text("What do you want to watch?", fontSize = 40.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tell FireMind your mood, your time budget, or nothing at all.",
                    fontSize = 20.sp,
                    color = TextSecondary
                )
            }
        }
        item {
            Button(onClick = { onAsk("") }) {
                Text("Ask FireMind", fontSize = 24.sp)
            }
        }
        item {
            Column {
                Text("Quick moods", fontSize = 24.sp)
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
                Text("Recommended for you", fontSize = 24.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(viewModel.catalogMovies.take(10)) { movie ->
                        MovieCard(
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

@Composable
fun MovieCard(title: String, year: Int, runtime: Int, onClick: () -> Unit) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(140.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 20.sp, maxLines = 2)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$year", fontSize = 16.sp, color = Brand)
                Text("$runtime min", fontSize = 16.sp, color = TextSecondary)
            }
        }
    }
}
