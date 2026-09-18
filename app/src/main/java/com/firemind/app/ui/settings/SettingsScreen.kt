package com.firemind.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firemind.app.BuildConfig
import com.firemind.app.FireMindViewModel
import com.firemind.app.ai.FireMindClient
import com.firemind.app.ui.common.Badge
import com.firemind.app.ui.common.ScreenHeader
import com.firemind.app.ui.theme.TextSecondary

/** About + backend health. Uses the same client the assistant uses. */
@Composable
fun SettingsScreen(viewModel: FireMindViewModel) {
    var status by remember { mutableStateOf("checking…") }
    var aiLive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val health = FireMindClient(BuildConfig.BACKEND_URL).health()
        when {
            health == null -> status = "backend unreachable — offline picks will be used"
            health.aiConfigured -> {
                status = "backend online, AI enabled"
                aiLive = true
            }
            else -> status = "backend online, AI disabled (fallback mode)"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader("About FireMind")
        Text("An AI-powered viewing companion for Fire TV.", fontSize = 20.sp, color = TextSecondary)
        Text("Catalog: 60 original demo titles (fictional metadata).", fontSize = 20.sp, color = TextSecondary)
        Text("Backend: ${BuildConfig.BACKEND_URL}", fontSize = 18.sp, color = TextSecondary)
        Row {
            Badge(
                when {
                    status.startsWith("backend online, AI enabled") -> "AI enabled"
                    status.startsWith("backend online") -> "fallback mode"
                    status == "checking…" -> "checking…"
                    else -> "offline"
                },
                highlighted = aiLive
            )
            Spacer(Modifier.height(0.dp))
        }
        Text("Status: $status", fontSize = 20.sp, color = if (aiLive) com.firemind.app.ui.theme.Brand else TextSecondary)
        Text(
            "All recommendations are explanations, not links — FireMind helps you decide what to watch.",
            fontSize = 16.sp, color = TextSecondary
        )
    }
}
