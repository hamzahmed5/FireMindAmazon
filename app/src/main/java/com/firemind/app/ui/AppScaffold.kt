package com.firemind.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firemind.app.ai.FireMindClient
import com.firemind.app.ui.assistant.AssistantScreen
import com.firemind.app.ui.browse.BrowseScreen
import com.firemind.app.ui.common.TopNav
import com.firemind.app.ui.details.DetailsScreen
import com.firemind.app.ui.home.HomeScreen
import com.firemind.app.ui.recommendations.ResultsScreen
import com.firemind.app.ui.settings.SettingsScreen
import com.firemind.app.ui.theme.GradientBackdrop
import com.firemind.app.ui.watchlist.WatchlistScreen
import com.firemind.app.BuildConfig

private fun railSectionFor(route: String): String = when {
    route.startsWith("details") -> "browse"
    else -> route
}

/** App shell: Stitch-style top nav + navigation graph. Back pops the stack. */
@Composable
fun FireMindAppUi(viewModel: FireMindViewModel) {
    val navController = rememberNavController()
    val assistantState by viewModel.assistant.collectAsState()
    var online by remember { mutableStateOf(false) }

    // One health probe for the nav bar's Online pill; silently retries on the
    // About screen's own probe cadence. Failure just means the pill stays grey.
    LaunchedEffect(Unit) {
        while (true) {
            online = FireMindClient(BuildConfig.BACKEND_URL).health() != null
            kotlinx.coroutines.delay(30_000)
        }
    }

    // Backdrop wraps the WHOLE shell. (Wrapping an inner pane would put a
    // fillMaxSize Box inside this Column, swallowing the whole space - the
    // lesson from the first UI refresh.)
    GradientBackdrop {
        Column(Modifier.fillMaxSize()) {
            TopNav(
                current = railSectionFor(currentRouteOr(navController, "home")),
                online = online,
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
                startDestination = "home",
                modifier = Modifier.fillMaxSize()
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
