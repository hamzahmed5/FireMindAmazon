package com.firemind.app.ui.assistant

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firemind.app.FireMindViewModel
import com.firemind.app.ui.theme.Brand
import com.firemind.app.ui.theme.TextSecondary

private val PROMPTS = listOf(
    "I want a mind-bending sci-fi movie under two hours",
    "Something funny under 100 minutes",
    "A cozy mystery for tonight",
    "Give me a family pick for movie night",
    "Surprise me with a top-rated drama",
    "An exciting thriller, nothing long"
)

private val KEY_ROWS = listOf(
    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
    listOf("Z", "X", "C", "V", "B", "N", "M", "SPACE", "BKSP")
)

/**
 * Ask FireMind. Remote typing is painful, so one D-pad press runs a strong
 * preset prompt; the on-screen keyboard stays available for custom asks.
 */
@Composable
fun AssistantScreen(
    viewModel: FireMindViewModel,
    onSubmit: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ask FireMind", fontSize = 36.sp)
        Text(
            "Pick a prompt or type your own, then press Ask.",
            fontSize = 20.sp,
            color = TextSecondary
        )

        LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
        ) {
            items(PROMPTS) { prompt ->
                Button(
                    onClick = { onSubmit(prompt) },
                    modifier = Modifier.focusProperties { up = FocusRequester.Cancel }
                ) {
                    Text(prompt, fontSize = 18.sp)
                }
            }
        }

        Text(
            if (draft.isBlank()) "Your question: (type below)" else "Your question: $draft",
            fontSize = 22.sp,
            color = if (draft.isBlank()) TextSecondary else Brand
        )

        KEY_ROWS.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.focusGroup()
            ) {
                row.forEach { key ->
                    Button(
                        onClick = {
                            draft = when (key) {
                                "BKSP" -> draft.dropLast(1)
                                "SPACE" -> "$draft "
                                else -> draft + key.lowercase()
                            }
                        }
                    ) {
                        Text(
                            when (key) {
                                "SPACE" -> "␣"
                                "BKSP" -> "⌫"
                                else -> key
                            },
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        Button(
            onClick = { if (draft.isNotBlank()) onSubmit(draft) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        ) {
            Text("Ask", fontSize = 22.sp)
        }
    }
}
