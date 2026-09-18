package com.firemind.app.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firemind.app.FireMindViewModel
import com.firemind.app.ui.common.PosterCard
import com.firemind.app.ui.common.RatingPill
import com.firemind.app.ui.common.ScreenHeader
import com.firemind.app.ui.theme.Brand
import com.firemind.app.ui.theme.TextPrimary
import com.firemind.app.ui.theme.TextSecondary

/**
 * Content details: metadata, description, watchlist toggle, and
 * "recommend something similar".
 */
@Composable
fun DetailsScreen(
    movieId: String,
    viewModel: FireMindViewModel,
    onOpenDetails: (String) -> Unit
) {
    val movie = viewModel.movieById(movieId)
    var inWatchlist by remember(movieId) {
        mutableStateOf(movieId in viewModel.watchlistIds.value)
    }
    var similar by remember { mutableStateOf<com.firemind.app.data.model.RecommendResponse?>(null) }
    var showSimilar by remember { mutableStateOf(false) }
    val watchlistButtonFocus = remember { FocusRequester() }
    LaunchedEffect(movieId) { watchlistButtonFocus.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (movie == null) {
            Text("Title not found", fontSize = 30.sp)
            return@Column
        }

        ScreenHeader(movie.title)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("${movie.year}", fontSize = 20.sp, color = Brand, fontWeight = FontWeight.SemiBold)
            Text(movie.genres.joinToString(" / "), fontSize = 20.sp, color = TextSecondary)
            Text("${movie.runtime} min", fontSize = 20.sp, color = TextSecondary)
            RatingPill(movie.rating)
        }
        Text(movie.description, fontSize = 22.sp, lineHeight = 30.sp, color = TextPrimary)

        Button(
            onClick = {
                viewModel.toggleWatchlist(movie.id)
                inWatchlist = !inWatchlist
            },
            modifier = Modifier.focusRequester(watchlistButtonFocus)
        ) {
            Text(
                if (inWatchlist) "✓ In Watchlist (remove)" else "Add to Watchlist",
                fontSize = 20.sp
            )
        }

        Button(onClick = {
            showSimilar = true
            viewModel.similar(movie.id) { response -> similar = response }
        }) {
            Text("Recommend something similar", fontSize = 20.sp)
        }

        if (showSimilar) {
            Text("Because you viewed ${movie.title}", fontSize = 22.sp, color = Brand, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(similar?.recommendations ?: emptyList()) { rec ->
                    PosterCard(
                        title = rec.title,
                        year = rec.year,
                        runtime = rec.runtime,
                        onClick = { onOpenDetails(rec.id) }
                    )
                }
            }
        }
    }
}
