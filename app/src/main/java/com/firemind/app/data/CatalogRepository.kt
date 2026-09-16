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

    companion object {
        const val CATALOG_ASSET = "catalog.json"

        /**
         * Quick-mood chips shown on Home and the Assistant screen. Also
         * the direct-tag fallback for [com.firemind.app.data.recommend.LocalRecommender].
         */
        val MOOD_CHIPS = listOf(
            "Funny", "Exciting", "Family", "Sci-Fi", "Relaxing", "Cozy", "Mind-bending"
        )
    }
}
