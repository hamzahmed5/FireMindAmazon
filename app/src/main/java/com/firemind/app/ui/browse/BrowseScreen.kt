package com.firemind.app.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firemind.app.FireMindViewModel
import com.firemind.app.ui.home.MovieCard

/** Full catalog grid, grouped implicitly by popularity (rating order). */
@Composable
fun BrowseScreen(
    viewModel: FireMindViewModel,
    onOpenDetails: (String) -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }

    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Text("Browse the catalog", fontSize = 36.sp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.catalogMovies.sortedByDescending { it.rating }) { movie ->
                val isFirst = movie === viewModel.catalogMovies.sortedByDescending { it.rating }.first()
                MovieCard(
                    title = movie.title,
                    year = movie.year,
                    runtime = movie.runtime,
                    onClick = { onOpenDetails(movie.id) },
                    modifier = if (isFirst) Modifier.focusRequester(firstFocus) else Modifier
                )
            }
        }
    }
}
