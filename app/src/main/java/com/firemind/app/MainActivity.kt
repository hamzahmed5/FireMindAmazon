package com.firemind.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firemind.app.ui.theme.FireMindTheme

/**
 * Factory so the ViewModel gets app-scoped dependencies without a
 * reflection-based framework.
 */
class FireMindViewModelFactory(private val app: FireMindApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FireMindViewModel(app.catalogRepository, app.watchlistStore, app.fireMindClient) as T
}

/** FireMind entry point. Single activity, Compose TV UI, D-pad first. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FireMindApp
        setContent {
            FireMindTheme {
                val viewModel: FireMindViewModel = viewModel(
                    factory = FireMindViewModelFactory(app)
                )
                FireMindAppUi(viewModel)
            }
        }
    }
}
