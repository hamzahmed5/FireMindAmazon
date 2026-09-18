package com.firemind.app.ui.recommendations

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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Surface
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.firemind.app.AssistantUiState
import com.firemind.app.FireMindViewModel
import com.firemind.app.ui.common.Badge
import com.firemind.app.ui.common.EmptyScreen
import com.firemind.app.ui.common.ErrorScreen
import com.firemind.app.ui.common.LoadingScreen
import com.firemind.app.ui.common.RatingPill
import com.firemind.app.ui.theme.Brand
import com.firemind.app.ui.theme.SurfaceRaised
import com.firemind.app.ui.theme.SurfaceVariant
import com.firemind.app.ui.theme.TextPrimary
import com.firemind.app.ui.theme.TextSecondary

/**
 * Results for an ask. Shows whether the answer came from the AI backend or
 * the local fallback path (honest labeling), then up to four picks, each
 * with an explicit why-this line.
 */
@Composable
fun ResultsScreen(
    state: AssistantUiState,
    viewModel: FireMindViewModel,
    onOpenDetails: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is AssistantUiState.Loading ->
            LoadingScreen("FireMind is thinking…")
        is AssistantUiState.Error ->
            ErrorScreen(state.message, onRetry)
        is AssistantUiState.Idle ->
            EmptyScreen(
                "Ask away",
                "Choose a prompt and FireMind will pick a few titles."
            )
        is AssistantUiState.Results -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("For you", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "\"${state.query}\"",
                        fontSize = 18.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(12.dp))
                Badge(
                    if (state.fromAi) "AI · Amazon Bedrock" else "Curated picks",
                    highlighted = state.fromAi
                )
            }
            Spacer(Modifier.height(16.dp))
            val firstFocus = remember { FocusRequester() }
            LaunchedEffect(state.query) { firstFocus.requestFocus() }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.response.recommendations) { rec ->
                    Surface(
                        onClick = { onOpenDetails(rec.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .then(
                                if (rec === state.response.recommendations.first())
                                    Modifier.focusRequester(firstFocus)
                                else Modifier
                            ),
                        shape = ClickableSurfaceDefaults.shape(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = SurfaceRaised,
                            focusedContainerColor = SurfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "${rec.title}  ·  ${rec.year}  ·  ${rec.runtime} min",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    rec.reason,
                                    fontSize = 17.sp,
                                    color = TextSecondary,
                                    maxLines = 2
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    rec.genres.joinToString(" / "),
                                    fontSize = 16.sp,
                                    color = Brand
                                )
                                Spacer(Modifier.width(14.dp))
                                RatingPill(rec.rating)
                            }
                        }
                    }
                }
            }
        }
    }
}
