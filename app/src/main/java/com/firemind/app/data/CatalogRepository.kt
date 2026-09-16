package com.firemind.app.data

import android.content.Context
import com.firemind.app.data.model.Movie
import kotlinx.serialization.json.Json

/**
 * Loads the bundled catalog and provides the deterministic filtering and
 * fallback ranking used when the AI backend is unreachable or fails.
 *
 * The catalog ships in assets and is the same dataset the backend uses,
 * so results stay consistent whether recommendations come from AI or
 * from the local fallback path.
 */
class CatalogRepository(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val catalog: List<Movie> by lazy {
        runCatching {
            val text = context.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
            json.decodeFromString<List<Movie>>(text)
        }.getOrElse { emptyList() }
    }

    fun byId(id: String): Movie? = catalog.firstOrNull { it.id == id }

    /** Mood chips map directly onto catalog mood tags. */
    fun byMood(mood: String, limit: Int = 5): List<Movie> =
        catalog.filter { movie -> movie.moods.any { it.equals(mood, ignoreCase = true) } }
            .sortedByDescending { it.rating }
            .take(limit)

    /** Deterministic fallback recommendations independent of the query. */
    fun fallback(limit: Int = 4): List<Movie> =
        catalog.sortedByDescending { it.rating }.take(limit)

    /**
     * Local stand-in for /api/similar: same mood overlap first, then genre
     * overlap, always excluding the source title.
     */
    fun similarTo(movie: Movie, limit: Int = 4): List<Movie> =
        catalog.asSequence()
            .filter { it.id != movie.id }
            .sortedByDescending { candidate ->
                val moodOverlap = candidate.moods.count { it in movie.moods }
                val genreOverlap = candidate.genres.count { it in movie.genres }
                moodOverlap * 10 + genreOverlap * 5 + candidate.rating
            }
            .take(limit)
            .toList()

    companion object {
        const val CATALOG_ASSET = "catalog.json"

        /** Quick-mood chips shown on Home and the Assistant screen. */
        val MOOD_CHIPS = listOf(
            "Funny", "Exciting", "Family", "Sci-Fi", "Relaxing", "Cozy", "Mind-bending"
        )

        /** Moods users can ask for that have no direct catalog tag. */
        val MOOD_SYNONYMS = mapOf(
            "relaxing" to "Cozy",
            "chill" to "Cozy",
            "calm" to "Cozy",
            "heartwarming" to "Heartfelt",
            "touching" to "Heartfelt",
            "twist" to "Mind-bending",
            "smart" to "Mind-bending",
            "hilarious" to "Funny",
            "laugh" to "Funny",
            "comedy" to "Funny",
            "thrilling" to "Exciting",
            "adrenaline" to "Exciting",
            "action" to "Exciting",
            "kids" to "Family",
            "children" to "Family"
        )
    }
}
