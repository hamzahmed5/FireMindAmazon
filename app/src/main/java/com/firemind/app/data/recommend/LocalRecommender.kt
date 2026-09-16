package com.firemind.app.data.recommend

import com.firemind.app.data.model.Movie
import com.firemind.app.data.model.Recommendation

/**
 * Deterministic recommendation engine used when the AI backend is
 * unreachable or fails. Deliberately pure (no Android or coroutine
 * dependencies) so it can be unit tested directly.
 *
 * This mirrors `backend/lib/catalog.js` one-for-one: same intent parsing,
 * same ranking, same reason wording. Keeping the two in lockstep is what
 * makes the app's "curated picks" and the backend's "fallback" results
 * indistinguishable to the user, and it lets the Kotlin and JS suites
 * assert the same expectations. Any behavioral change here must be
 * mirrored there.
 */
object LocalRecommender {

    /** The backend caps fallback results at 4; the UI shows them all. */
    const val MAX_RESULTS = 4

    /** How many ranked candidates the engine considers before trimming. */
    private const val CANDIDATE_LIMIT = 12

    /**
     * Mood tags that actually exist in the catalog. Used for direct
     * matching, so a request for a genre word (or any other word that
     * happens to look like a mood) can never "match" a nonexistent tag.
     */
    private val CATALOG_MOODS = listOf(
        "Serious", "Cozy", "Funny", "Whimsical",
        "Mind-bending", "Heartfelt", "Exciting", "Tense"
    )

    /** Genre tags that actually exist in the catalog. */
    private val CATALOG_GENRES = listOf(
        "Drama", "Mystery", "Sci-Fi", "Comedy", "Family",
        "Action", "Thriller", "Adventure", "Romance"
    )

    /**
     * Spelled-out numbers, matching the backend's table. Values are Double
     * because "half" is fractional; the map is typed explicitly so Kotlin
     * infers a numeric type with arithmetic operators.
     */
    private val WORD_NUMBERS: Map<String, Double> = mapOf(
        "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
        "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
        "a" to 1.0, "an" to 1.0, "half" to 0.5
    )

    /**
     * Words that imply a catalog mood. Mirrors the backend table exactly.
     * Entries deliberately absent: "kids"/"children"/"animated"/"family"
     * map to the audience filter and the Family *genre*, not to a mood —
     * there is no mood called "Family" in the catalog.
     */
    private val MOOD_SYNONYMS: Map<String, String> = mapOf(
        "relaxing" to "Cozy", "chill" to "Cozy", "calm" to "Cozy",
        "heartwarming" to "Heartfelt", "touching" to "Heartfelt",
        "twist" to "Mind-bending", "smart" to "Mind-bending", "clever" to "Mind-bending",
        "hilarious" to "Funny", "laugh" to "Funny", "funny" to "Funny",
        "thrilling" to "Exciting", "adrenaline" to "Exciting",
        "suspense" to "Tense", "scary" to "Tense", "spooky" to "Tense"
    )

    /**
     * Words that imply a catalog genre. Genre matching is what makes the
     * "Sci-Fi" and "Family" chips do what they say.
     */
    private val GENRE_SYNONYMS: Map<String, String> = mapOf(
        "sci-fi" to "Sci-Fi", "scifi" to "Sci-Fi", "sci fi" to "Sci-Fi",
        "science fiction" to "Sci-Fi", "space" to "Sci-Fi",
        "comedy" to "Comedy", "comedies" to "Comedy", "sitcom" to "Comedy",
        "mystery" to "Mystery", "mysteries" to "Mystery", "whodunit" to "Mystery",
        "thriller" to "Thriller", "thrillers" to "Thriller",
        "action" to "Action",
        "drama" to "Drama", "dramas" to "Drama",
        "romance" to "Romance", "romantic" to "Romance",
        "rom com" to "Romance", "rom-com" to "Romance", "love story" to "Romance",
        "adventure" to "Adventure", "adventures" to "Adventure",
        "family" to "Family", "animated" to "Family"
    )

    /** Words that put the request in family-friendly territory. */
    private val AUDIENCE_WORDS = listOf(
        "family", "families", "kid", "kids", "child", "children", "animated", "all ages"
    )

    /**
     * Extract a runtime cap in minutes from free text, understanding both
     * digits ("under 100 minutes", "2 hours") and spelled-out numbers
     * ("under two hours"). Returns null when the request names no limit.
     */
    fun parseRuntimeMax(query: String): Int? {
        val lower = query.lowercase()

        Regex("(\\d+)\\s*(min|minute)").find(lower)
            ?.groupValues?.get(1)?.toIntOrNull()
            ?.let { return it }

        Regex("(one|two|three|four|five|six|seven|eight|nine|ten|a|an|half)\\s*(hour|hr)")
            .find(lower)?.groupValues?.get(1)
            ?.let { word -> WORD_NUMBERS[word]?.let { return (it * 60).toInt() } }

        Regex("(\\d+)\\s*(hour|hr)").find(lower)
            ?.groupValues?.get(1)?.toIntOrNull()
            ?.let { return it * 60 }

        return null
    }

    /**
     * Detect the intended mood: curated synonyms first, then a direct
     * match against mood tags that exist in the catalog. Returns null when
     * the request expresses no mood.
     */
    fun parseMood(query: String): String? {
        val lower = query.lowercase()
        MOOD_SYNONYMS.entries.firstOrNull { lower.contains(it.key) }
            ?.let { return it.value }
        return CATALOG_MOODS.firstOrNull { lower.contains(it.lowercase()) }
    }

    /**
     * Detect the intended genre: curated synonyms first, then a direct
     * match against genres that exist in the catalog. Returns null when
     * the request names no genre.
     */
    fun parseGenre(query: String): String? {
        val lower = query.lowercase()
        GENRE_SYNONYMS.entries.firstOrNull { lower.contains(it.key) }
            ?.let { return it.value }
        return CATALOG_GENRES.firstOrNull { lower.contains(it.lowercase()) }
    }

    /** True when the request should be limited to family-friendly titles. */
    fun wantsFamilyFriendly(query: String, familyOnly: Boolean = false): Boolean =
        familyOnly || AUDIENCE_WORDS.any { query.lowercase().contains(it) }

    /**
     * Filter the catalog by intent and rank by mood match, then genre
     * match, then rating.
     *
     * When the constraints leave nothing, they are relaxed in the
     * documented order — runtime cap first, family flag second — and
     * finally the unfiltered catalog is used, so a request can never
     * return zero picks. Mirrors `filterCandidates` in the backend.
     */
    fun filterCandidates(
        query: String,
        catalog: List<Movie>,
        familyOnly: Boolean = false
    ): List<Movie> {
        val mood = parseMood(query)
        val genre = parseGenre(query)
        val runtimeMax = parseRuntimeMax(query)
        val familyFilter = wantsFamilyFriendly(query, familyOnly)

        var pool = catalog
        if (familyFilter) pool = pool.filter { it.familyFriendly }
        if (runtimeMax != null) pool = pool.filter { it.runtime <= runtimeMax }
        if (pool.isEmpty()) {
            // Relax the runtime cap; keep the audience constraint.
            pool = if (familyFilter) catalog.filter { it.familyFriendly } else catalog
        }
        if (pool.isEmpty()) pool = catalog

        return pool
            .sortedWith(
                compareByDescending<Movie> { mood != null && it.moods.contains(mood) }
                    .thenByDescending { genre != null && it.genres.contains(genre) }
                    .thenByDescending { it.rating }
            )
            .take(CANDIDATE_LIMIT)
    }

    /**
     * Build fallback recommendations for a query. The reason string names
     * the interpreted intent, so the user can see what FireMind understood.
     *
     * Note: like the backend, the reason only mentions "family-friendly"
     * when the caller passed the flag explicitly — the query-derived
     * audience filter still applies to the results either way.
     */
    fun recommend(
        query: String,
        catalog: List<Movie>,
        familyOnly: Boolean = false,
        limit: Int = MAX_RESULTS
    ): List<Recommendation> {
        val mood = parseMood(query)
        val genre = parseGenre(query)
        val runtimeMax = parseRuntimeMax(query)

        val parts = buildList {
            mood?.let { add("a $it mood") }
            genre?.let { add("$it titles") }
            runtimeMax?.let { add("under $runtimeMax minutes") }
            if (familyOnly) add("family-friendly")
        }
        val prefix = if (parts.isNotEmpty()) {
            "You asked for ${parts.joinToString(" and ")}."
        } else {
            "Here are FireMind's top picks."
        }

        return filterCandidates(query, catalog, familyOnly)
            .take(limit)
            .map { movie -> movie.toRecommendation(reason = buildReason(prefix, movie)) }
    }

    /**
     * Titles similar to the given one, scored by mood overlap (×10),
     * genre overlap (×5), then rating. The source title is always
     * excluded. Mirrors `similarById` in the backend.
     */
    fun similarTo(movie: Movie, catalog: List<Movie>, limit: Int = 4): List<Recommendation> =
        catalog.asSequence()
            .filter { it.id != movie.id }
            .map { candidate ->
                candidate to (
                    candidate.moods.count { it in movie.moods } * 10 +
                        candidate.genres.count { it in movie.genres } * 5 +
                        candidate.rating
                    )
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (candidate, _) ->
                candidate.toRecommendation(
                    reason = "Similar to \"${movie.title}\": shares its " +
                        "${candidate.moods.joinToString("/").lowercase()} tone."
                )
            }
            .toList()

    private fun buildReason(prefix: String, movie: Movie): String =
        "$prefix \"${movie.title}\" fits: " +
            "${movie.moods.joinToString("/").lowercase()}, " +
            "${movie.runtime} min, rated ${movie.rating}."

    private fun Movie.toRecommendation(reason: String) = Recommendation(
        id = id,
        title = title,
        year = year,
        genres = genres,
        runtime = runtime,
        rating = rating,
        reason = reason,
        summary = description
    )
}
