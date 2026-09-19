package com.firemind.app.ui.assistant

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firemind.app.FireMindViewModel
import com.firemind.app.ui.theme.Cyan
import com.firemind.app.ui.theme.Outfit
import com.firemind.app.ui.theme.SlateCard
import com.firemind.app.ui.theme.SlateHigh
import com.firemind.app.ui.theme.TextBright
import com.firemind.app.ui.theme.TextSoft

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
    listOf("Z", "X", "C", "V", "B", "N", "M", "SPACE", "BKSP", "ENTER")
)

/**
 * Ask FireMind. Remote typing is painful, so one D-pad press runs a strong
 * preset prompt; the on-screen keyboard stays for custom asks and now ends
 * in an ENTER key - type and search without leaving the keyboard.
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
            .padding(horizontal = 48.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Ask FireMind",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = Outfit,
            color = TextBright
        )
        Text(
            "Pick a ready prompt, or type your own and press ENTER.",
            fontSize = 18.sp,
            fontFamily = Outfit,
            color = TextSoft
        )

        LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
        ) {
            items(PROMPTS) { prompt ->
                Surface(
                    onClick = { onSubmit(prompt) },
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SlateCard,
                        focusedContainerColor = SlateHigh
                    ),
                    modifier = Modifier.focusProperties { up = FocusRequester.Cancel }
                ) {
                    Text(
                        prompt,
                        fontSize = 16.sp,
                        fontFamily = Outfit,
                        color = TextBright,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }
            }
        }

        // Live draft echo - cyan when there is something to search for.
        Text(
            if (draft.isBlank()) "Your question: (type below)" else "Your question: \"$draft\"",
            fontSize = 20.sp,
            fontFamily = Outfit,
            fontWeight = FontWeight.SemiBold,
            color = if (draft.isBlank()) TextSoft else Cyan,
            maxLines = 1
        )

        KEY_ROWS.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.focusGroup()
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        label = key,
                        onClick = {
                            when (key) {
                                "BKSP" -> draft = draft.dropLast(1)
                                "SPACE" -> draft = "$draft "
                                "ENTER" -> if (draft.isNotBlank()) onSubmit(draft.trim())
                                // Empty-draft ENTER is a deliberate no-op:
                                // the key stays focusable so D-pad RIGHT
                                // never escapes the keyboard into the nav.
                                else -> draft = draft + key.lowercase()
                            }
                        }
                    )
                }
            }
        }

        Surface(
            onClick = { if (draft.isNotBlank()) onSubmit(draft.trim()) },
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (draft.isBlank()) SlateCard else Cyan,
                focusedContainerColor = if (draft.isBlank()) SlateHigh else Cyan
            ),
            scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1.03f),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (draft.isBlank()) "Type something first" else "Search:  \"$draft\"",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Outfit,
                    color = if (draft.isBlank()) TextSoft else Color(0xFF003543),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * One compact keyboard key. Fixed 56dp cells so the whole QWERTY grid always
 * fits the TV width; ENTER is wider and cyan - it is the primary action.
 */
@Composable
private fun KeyboardKey(
    label: String,
    onClick: () -> Unit
) {
    val isEnter = label == "ENTER"
    val display = when (label) {
        "SPACE" -> "␣"
        "BKSP" -> "⌫"
        "ENTER" -> "↵ ENTER"
        else -> label
    }
    val keyWidth = when {
        isEnter -> 110.dp
        label == "SPACE" -> 90.dp
        else -> 56.dp
    }
    Surface(
        onClick = onClick,
        modifier = if (isEnter) {
            // RIGHT on the row's last key must clamp here, never flee to
            // the top nav mid-typing.
            Modifier.focusProperties { right = FocusRequester.Cancel }
        } else Modifier,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isEnter) Cyan else SlateCard,
            focusedContainerColor = if (isEnter) Cyan else SlateHigh,
            contentColor = if (isEnter) Color(0xFF003543) else TextBright
        ),
        scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1.08f)
    ) {
        Box(
            Modifier
                .width(keyWidth)
                .height(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                display,
                fontSize = if (isEnter) 15.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Outfit
            )
        }
    }
}
