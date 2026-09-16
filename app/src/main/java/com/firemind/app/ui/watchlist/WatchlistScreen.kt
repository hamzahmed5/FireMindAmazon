package com.firemind.app.ui.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firemind.app.FireMindViewModel
import com.firemind.app.ui.common.EmptyScreen
import com.firemind.app.ui.home.MovieCard

/** Saved titles. Persisted locally; survives restarts. */
@Composable
fun WatchlistScreen(
    viewModel: FireMindViewModel,
    onOpenDetails: (String) -> Unit
) {
    val ids by viewModel.watchlistIds.collectAsState()
    val movies = ids.mapNotNull { viewModel.movieById(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Watchlist", fontSize = 36.sp)
        if (movies.isEmpty()) {
            EmptyScreen(
                "Nothing saved yet",
                "Add titles from any details screen; they stay here across restarts."
            )
        } else {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(movies) { movie ->
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
