package com.firemind.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firemind.app.ui.assistant.AssistantScreen
import com.firemind.app.ui.browse.BrowseScreen
import com.firemind.app.ui.common.NavRail
import com.firemind.app.ui.details.DetailsScreen
import com.firemind.app.ui.home.HomeScreen
import com.firemind.app.ui.recommendations.ResultsScreen
import com.firemind.app.ui.settings.SettingsScreen
import com.firemind.app.ui.theme.GradientBackdrop
import com.firemind.app.ui.watchlist.WatchlistScreen

private fun railSectionFor(route: String): String = when {
    route.startsWith("details") -> "browse"
    else -> route
}

/** App shell: TV nav rail + navigation graph. Back pops the nav stack. */
@Composable
fun FireMindAppUi(viewModel: FireMindViewModel) {
    val navController = rememberNavController()
    val assistantState by viewModel.assistant.collectAsState()

    // Backdrop wraps the WHOLE shell. (Wrapping only the rail would put a
    // fillMaxSize Box inside this Row, which swallows the entire width and
    // squeezes the content pane to zero - exactly what the Fire OS run caught.)
    GradientBackdrop {
        Row(Modifier.fillMaxSize()) {
        NavRail(
            current = railSectionFor(currentRouteOr(navController, "home")),
            onSelect = { section ->
                navController.navigate(section) {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onAsk = { prompt ->
                        if (prompt.isBlank()) {
                            navController.navigate("assistant")
                        } else {
                            viewModel.ask(prompt)
                            navController.navigate("results")
                        }
                    },
                    onOpenDetails = { id -> navController.navigate("details/$id") }
                )
            }
            composable("assistant") {
                AssistantScreen(
                    viewModel = viewModel,
                    onSubmit = { query ->
                        viewModel.ask(query)
                        navController.navigate("results")
                    }
                )
            }
            composable("results") {
                ResultsScreen(
                    state = assistantState,
                    viewModel = viewModel,
                    onOpenDetails = { id -> navController.navigate("details/$id") },
                    onRetry = {
                        viewModel.resetAssistant()
                        navController.navigate("assistant") { launchSingleTop = true }
                    }
                )
            }
            composable("browse") {
                BrowseScreen(
                    viewModel = viewModel,
                    onOpenDetails = { id -> navController.navigate("details/$id") }
                )
            }
            composable("details/{movieId}") { entry ->
                val movieId = entry.arguments?.getString("movieId").orEmpty()
                DetailsScreen(
                    movieId = movieId,
                    viewModel = viewModel,
                    onOpenDetails = { id -> navController.navigate("details/$id") }
                )
            }
            composable("watchlist") {
                WatchlistScreen(
                    viewModel = viewModel,
                    onOpenDetails = { id -> navController.navigate("details/$id") }
                )
            }
            composable("settings") {
                SettingsScreen(viewModel)
            }
        }
        }
    }
}

@Composable
private fun currentRouteOr(
    navController: androidx.navigation.NavController,
    fallback: String
): String =
    navController.currentBackStackEntry?.destination?.route ?: fallback
