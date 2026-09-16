package com.firemind.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firemind.app.ai.FireMindClient
import com.firemind.app.data.CatalogRepository
import com.firemind.app.data.WatchlistStore
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

    val watchlistIds: StateFlow<Set<String>> = watchlist.ids
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val catalogMovies: List<Movie> get() = catalog.catalog

    /** Ask FireMind: AI backend first, deterministic local fallback second. */
    fun ask(query: String, familyOnly: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
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

    /** Deterministic in-app recommendation path (also used on backend failure). */
    private fun localRecommend(query: String, familyOnly: Boolean): List<Recommendation> {
        val q = query.lowercase()
        val wordNumbers: Map<String, Double> = mapOf(
            "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
            "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
            "a" to 1.0, "an" to 1.0, "half" to 0.5
        )
        val runtimeMax = Regex("(\\d+)\\s*(min|minute)").find(q)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("(one|two|three|four|five|six|seven|eight|nine|ten|a|an|half)\\s*(hour|hr)")
                .find(q)?.groupValues?.get(1)?.let { word ->
                    wordNumbers[word]?.let { n -> (n * 60).toInt() }
                }
            ?: Regex("(\\d+)\\s*(hour|hr)").find(q)
                ?.groupValues?.get(1)?.toIntOrNull()?.times(60)
        val mood = CatalogRepository.MOOD_SYNONYMS.entries
            .firstOrNull { q.contains(it.key) }?.value
            ?: CatalogRepository.MOOD_CHIPS.firstOrNull { q.contains(it.lowercase()) }

        var pool = catalog.catalog
        if (familyOnly || q.contains("family") || q.contains("kid")) {
            pool = pool.filter { it.familyFriendly }
        }
        runtimeMax?.let { max -> pool = pool.filter { it.runtime <= max } }
        if (pool.isEmpty()) pool = catalog.catalog

        val ranked = if (mood != null) {
            pool.sortedByDescending { if (mood in it.moods) 1 else 0 }
                .sortedByDescending { it.rating }
        } else {
            pool.sortedByDescending { it.rating }
        }.take(4)

        val matched = buildString {
            append("You asked for")
            mood?.let { append(" a $it mood") }
            runtimeMax?.let { append(" under $runtimeMax minutes") }
            if (familyOnly) append(" (family-friendly)")
            append(".")
        }
        return ranked.map { m ->
            Recommendation(
                id = m.id,
                title = m.title,
                year = m.year,
                genres = m.genres,
                runtime = m.runtime,
                rating = m.rating,
                reason = "$matched \"${m.title}\" fits: ${m.moods.joinToString("/").lowercase()}," +
                    " ${m.runtime} min, rated ${m.rating}.",
                summary = m.description
            )
        }
    }

    fun recommendationFromMovie(movie: Movie): Recommendation = Recommendation(
        id = movie.id,
        title = movie.title,
        year = movie.year,
        genres = movie.genres,
        runtime = movie.runtime,
        rating = movie.rating,
        reason = "Top rated in the FireMind catalog for its ${movie.moods.joinToString("/").lowercase()} tone.",
        summary = movie.description
    )

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
                    catalog.similarTo(m).map(::recommendationFromMovie)
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
