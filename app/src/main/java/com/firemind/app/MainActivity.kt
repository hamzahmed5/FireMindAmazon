package com.firemind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import com.firemind.app.ui.theme.FireMindTheme

/**
 * FireMind entry point. TV-first: single activity, Compose UI,
 * all interaction D-pad reachable.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FireMindTheme {
                LaunchScreen(
                    onAskFireMind = { /* wired in Phase 4 navigation */ }
                )
            }
        }
    }
}

@Composable
private fun LaunchScreen(onAskFireMind: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.firemind.app.ui.theme.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "FIREMIND",
                color = com.firemind.app.ui.theme.TextPrimary,
                fontSize = 56.sp
            )
            Text(
                text = "Your AI viewing companion",
                color = com.firemind.app.ui.theme.TextSecondary,
                fontSize = 22.sp
            )
            Button(onClick = onAskFireMind) {
                Text("Ask FireMind", fontSize = 22.sp)
            }
            Box(Modifier.padding(bottom = 8.dp))
        }
    }
}
