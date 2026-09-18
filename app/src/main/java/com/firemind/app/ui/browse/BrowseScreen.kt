package com.firemind.app.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.firemind.app.FireMindViewModel
import com.firemind.app.ui.common.PosterCard
import com.firemind.app.ui.common.ScreenHeader
import com.firemind.app.ui.theme.TextSecondary
import androidx.compose.ui.unit.sp

/** Full catalog grid, grouped implicitly by popularity (rating order). */
@Composable
fun BrowseScreen(
    viewModel: FireMindViewModel,
    onOpenDetails: (String) -> Unit
) {
    val byRating = viewModel.catalogMovies.sortedByDescending { it.rating }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 28.dp)
    ) {
        ScreenHeader("Browse the catalog", "All 60 titles, highest rated first")
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(byRating) { movie ->
                val isFirst = movie === byRating.first()
                PosterCard(
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
