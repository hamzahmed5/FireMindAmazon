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
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firemind.app.AssistantUiState
import com.firemind.app.FireMindViewModel
import com.firemind.app.ui.theme.Brand
import com.firemind.app.ui.theme.FocusBorder
import com.firemind.app.ui.theme.TextSecondary
import com.firemind.app.ui.common.EmptyScreen
import com.firemind.app.ui.common.ErrorScreen
import com.firemind.app.ui.common.LoadingScreen

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
                .padding(40.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("For: \"${state.query}\"", fontSize = 24.sp)
                Spacer(Modifier.width(16.dp))
                Text(
                    if (state.fromAi) "AI" else "curated picks",
                    fontSize = 16.sp,
                    color = if (state.fromAi) Brand else FocusBorder
                )
            }
            val firstFocus = remember { FocusRequester() }
            LaunchedEffect(state.query) { firstFocus.requestFocus() }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.response.recommendations) { rec ->
                    androidx.tv.material3.Surface(
                        onClick = { onOpenDetails(rec.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .then(
                                if (rec === state.response.recommendations.first())
                                    Modifier.focusRequester(firstFocus)
                                else Modifier
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${rec.title}  ·  ${rec.year}  ·  ${rec.runtime} min",
                                    fontSize = 24.sp)
                                Text(
                                    "Why FireMind recommends it: ${rec.reason}",
                                    fontSize = 17.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                rec.genres.joinToString(" / ") + "  ·  ★ ${rec.rating}",
                                fontSize = 16.sp,
                                color = Brand
                            )
                        }
                    }
                }
            }
        }
    }
}
