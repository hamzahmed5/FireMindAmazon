package com.firemind.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firemind.app.ai.FireMindClient
import com.firemind.app.data.CatalogRepository
import com.firemind.app.data.WatchlistStore
import com.firemind.app.data.recommend.LocalRecommender
import com.firemind.app.data.model.Filters
import com.firemind.app.data.model.RecommendResponse
import com.firemind.app.data.model.Recommendation
import com.firemind.app.data.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** States for the ask -> results journey. Never leaves the UI frozen. */
sealed interface AssistantUiState {
    data object Idle : AssistantUiState
    data object Loading : AssistantUiState
    data class Results(
        val query: String,
        val response: RecommendResponse,
        val fromAi: Boolean
    ) : AssistantUiState

    data class Error(val message: String) : AssistantUiState
}

/**
 * Single source of UI truth. Every async path has a fallback so the
 * experience degrades gracefully instead of breaking.
 */
class FireMindViewModel(
    private val catalog: CatalogRepository,
    private val watchlist: WatchlistStore,
    private val client: FireMindClient
) : ViewModel() {

    private val _assistant = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val assistant: StateFlow<AssistantUiState> = _assistant.asStateFlow()

    private val _lastSummary = MutableStateFlow<String?>(null)
    val lastSummary: StateFlow<String?> = _lastSummary.asStateFlow()

    /** Persists across screens for the Home "Previous query / Re-run" card. */
    private val _lastQuery = MutableStateFlow<String?>(null)
    val lastQuery: StateFlow<String?> = _lastQuery.asStateFlow()

    val watchlistIds: StateFlow<Set<String>> = watchlist.ids
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val catalogMovies: List<Movie> get() = catalog.catalog

    /** Ask FireMind: AI backend first, deterministic local fallback second. */
    fun ask(query: String, familyOnly: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _lastQuery.value = trimmed
        _assistant.value = AssistantUiState.Loading
        viewModelScope.launch {
            val response = client.recommend(
                com.firemind.app.data.model.RecommendRequest(
                    query = trimmed,
                    filters = Filters(familyOnly = familyOnly)
                )
            )
            _assistant.value = if (response != null && response.recommendations.isNotEmpty()) {
                AssistantUiState.Results(trimmed, response, fromAi = response.source == "ai")
            } else {
                val fallback = localRecommend(trimmed, familyOnly)
                AssistantUiState.Results(
                    trimmed,
                    RecommendResponse(source = "fallback", recommendations = fallback),
                    fromAi = false
                )
            }
        }
    }

    /**
     * Deterministic in-app recommendation path (also used on backend
     * failure). The logic lives in [LocalRecommender] so it can be unit
     * tested without Android or coroutine machinery.
     */
    private fun localRecommend(query: String, familyOnly: Boolean): List<Recommendation> =
        LocalRecommender.recommend(query, catalog.catalog, familyOnly)

    fun movieById(id: String): Movie? = catalog.byId(id)

    fun resetAssistant() {
        _assistant.value = AssistantUiState.Idle
        _lastSummary.value = null
    }

    /** Spoiler-safe summary: AI when available, catalog description otherwise. */
    fun summarize(id: String) {
        viewModelScope.launch {
            _lastSummary.value = null
            val ai = client.summarize(id)
            _lastSummary.value = ai?.summary ?: catalog.byId(id)?.description
        }
    }

    fun similar(id: String, onDone: (RecommendResponse) -> Unit) {
        viewModelScope.launch {
            val movie = catalog.byId(id)
            val ai = client.similar(id)
            val response = ai ?: RecommendResponse(
                source = "fallback",
                recommendations = movie?.let { m ->
                    LocalRecommender.similarTo(m, catalog.catalog)
                }.orEmpty()
            )
            onDone(response)
        }
    }

    fun toggleWatchlist(id: String) {
        viewModelScope.launch {
            if (id in watchlistIds.value) watchlist.remove(id) else watchlist.add(id)
        }
    }
}
