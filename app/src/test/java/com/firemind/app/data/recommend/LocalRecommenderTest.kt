package com.firemind.app.data.recommend

import com.firemind.app.data.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mirrors `backend/test/catalog.test.js` one-for-one so the app's offline
 * engine and the backend's fallback engine are held to the same
 * expectations. If a behavior changes on one side, both suites should
 * need updating.
 */
class LocalRecommenderTest {

    private val catalog = TestCatalog.movies

    // --- runtime parsing (backend: "runtime parsing: minutes and hours") ---

    @Test
    fun `parses a runtime cap given in minutes`() {
        assertEquals(100, LocalRecommender.parseRuntimeMax("funny movie under 100 minutes"))
    }

    @Test
    fun `parses a runtime cap given in numeric hours`() {
        assertEquals(120, LocalRecommender.parseRuntimeMax("something under 2 hours"))
    }

    @Test
    fun `returns null when no runtime is mentioned`() {
        assertNull(LocalRecommender.parseRuntimeMax("no constraint here"))
    }

    // --- spelled-out numbers (backend regression: "two hours") ---

    @Test
    fun `parses spelled-out hour counts`() {
        assertEquals(120, LocalRecommender.parseRuntimeMax("mind-bending sci-fi movie under two hours"))
        assertEquals(60, LocalRecommender.parseRuntimeMax("something for one hour"))
        assertEquals(60, LocalRecommender.parseRuntimeMax("about half an hour"))
    }

    @Test
    fun `every pick respects a spelled-out two hour cap - regression for the ignored constraint`() {
        val recommendations = LocalRecommender.recommend(
            "mind-bending sci-fi under two hours",
            catalog
        )
        assertTrue("expected at least 3 picks", recommendations.size >= 3)
        recommendations.forEach { rec ->
            assertTrue(
                "'${rec.title}' runs ${rec.runtime} min, over the 120 minute cap",
                rec.runtime <= 120
            )
        }
    }

    // --- mood parsing (backend: "mood parsing: synonyms and direct tags") ---

    @Test
    fun `maps mood synonyms to catalog moods`() {
        assertEquals("Cozy", LocalRecommender.parseMood("something relaxing"))
        assertEquals("Funny", LocalRecommender.parseMood("a hilarious comedy"))
        assertEquals("Mind-bending", LocalRecommender.parseMood("clever and twisty"))
        assertEquals("Tense", LocalRecommender.parseMood("something suspenseful"))
    }

    @Test
    fun `matches direct catalog mood tags`() {
        assertEquals("Mind-bending", LocalRecommender.parseMood("mind-bending"))
        assertEquals("Cozy", LocalRecommender.parseMood("cozy"))
    }

    @Test
    fun `returns null when no mood is expressed`() {
        assertNull(LocalRecommender.parseMood("gibberish"))
    }

    // --- genre parsing (backend: "genre parsing: synonyms and direct tags") ---

    @Test
    fun `maps genre synonyms to catalog genres`() {
        assertEquals("Sci-Fi", LocalRecommender.parseGenre("sci-fi please"))
        assertEquals("Sci-Fi", LocalRecommender.parseGenre("a space movie"))
        assertEquals("Romance", LocalRecommender.parseGenre("something romantic"))
        assertEquals("Family", LocalRecommender.parseGenre("an animated pick"))
        assertEquals("Thriller", LocalRecommender.parseGenre("thriller"))
        assertNull(LocalRecommender.parseGenre("gibberish"))
    }

    @Test
    fun `mood parsing never matches a tag that is not a catalog mood - regression for the chips`() {
        // "sci-fi" and "family" are genres. The earlier engine returned
        // them as moods, so those chips produced no constraint at all.
        assertNull(LocalRecommender.parseMood("sci-fi please"))
        assertNull(LocalRecommender.parseMood("family movie"))
        assertNull(LocalRecommender.parseMood("animated pick"))
    }

    @Test
    fun `a genre request returns titles of that genre - regression for the Sci-Fi chip`() {
        val recommendations = LocalRecommender.recommend("Sci-Fi", catalog)

        assertTrue("expected at least 3 picks", recommendations.size >= 3)
        recommendations.forEach { rec ->
            val movie = catalog.first { it.id == rec.id }
            assertTrue(
                "'${movie.title}' has genres ${movie.genres}, not sci-fi",
                movie.genres.contains("Sci-Fi")
            )
        }
    }

    @Test
    fun `a family request returns family friendly titles only - regression for the Family chip`() {
        val recommendations = LocalRecommender.recommend("Family", catalog)

        assertTrue("expected at least 3 picks", recommendations.size >= 3)
        recommendations.forEach { rec ->
            val movie = catalog.first { it.id == rec.id }
            assertTrue("'${movie.title}' is not family friendly", movie.familyFriendly)
        }
    }

    @Test
    fun `reason strings name the interpreted genre`() {
        val recommendations = LocalRecommender.recommend("I want a sci-fi movie under two hours", catalog)
        assertTrue(
            "reason should name the genre: ${recommendations.first().reason}",
            recommendations.first().reason.contains("Sci-Fi titles")
        )
    }

    // --- ranking (backend: "fallback respects runtime cap and orders by mood then rating") ---

    @Test
    fun `respects the runtime cap and ranks mood matches first`() {
        val recommendations = LocalRecommender.recommend("something funny under 100 minutes", catalog)

        assertTrue(recommendations.size in 3..4)
        recommendations.forEach { rec ->
            assertTrue("'${rec.title}' runs ${rec.runtime} min", rec.runtime <= 100)
        }

        // The best pick should actually carry the requested mood.
        val topPick = catalog.first { it.id == recommendations.first().id }
        assertTrue(
            "top pick '${topPick.title}' has moods ${topPick.moods}",
            topPick.moods.contains("Funny")
        )
    }

    @Test
    fun `never returns more than the result cap`() {
        assertEquals(
            LocalRecommender.MAX_RESULTS,
            LocalRecommender.recommend("anything at all", catalog).size
        )
    }

    @Test
    fun `is deterministic for the same request`() {
        val first = LocalRecommender.recommend("something exciting under two hours", catalog)
        val second = LocalRecommender.recommend("something exciting under two hours", catalog)
        assertEquals(first.map { it.id }, second.map { it.id })
    }

    // --- audience filter (backend: "fallback family filter never returns adult titles") ---

    @Test
    fun `family filter never returns a non-family title`() {
        val recommendations = LocalRecommender.recommend("family movie night", catalog, familyOnly = true)
        assertTrue(recommendations.isNotEmpty())
        recommendations.forEach { rec ->
            val movie = catalog.first { it.id == rec.id }
            assertTrue("'${movie.title}' is not family friendly", movie.familyFriendly)
        }
    }

    @Test
    fun `family wording in the query applies the filter without the explicit flag`() {
        val recommendations = LocalRecommender.recommend("something for the kids", catalog)
        recommendations.forEach { rec ->
            val movie = catalog.first { it.id == rec.id }
            assertTrue("'${movie.title}' is not family friendly", movie.familyFriendly)
        }
    }

    // --- constraint relaxation (app-only: the backend path can also hit this) ---

    @Test
    fun `relaxes an impossible runtime cap instead of returning nothing`() {
        val recommendations = LocalRecommender.recommend("funny under 5 minutes", catalog)
        assertTrue("should still return picks", recommendations.isNotEmpty())
    }

    @Test
    fun `returns top rated titles for a request with no usable intent`() {
        val recommendations = LocalRecommender.recommend("hello", catalog)
        assertEquals(LocalRecommender.MAX_RESULTS, recommendations.size)
        assertTrue(
            "reason should fall back to the generic wording",
            recommendations.first().reason.startsWith("Here are FireMind's top picks.")
        )
    }

    // --- reason strings (backend: "fallback reason strings reference the request") ---

    @Test
    fun `reason strings name the interpreted request and the title`() {
        val recommendations = LocalRecommender.recommend("I want something funny under two hours", catalog)

        recommendations.forEach { rec ->
            assertTrue(
                "reason should quote the request: ${rec.reason}",
                rec.reason.contains("You asked for")
            )
            assertTrue(
                "reason should name the title: ${rec.reason}",
                rec.reason.contains(rec.title)
            )
            assertTrue(
                "reason should state the runtime",
                rec.reason.contains("min, rated")
            )
        }
    }

    @Test
    fun `reason mentions family friendly only when the flag was explicit`() {
        val explicit = LocalRecommender.recommend("movie night", catalog, familyOnly = true)
        assertTrue(explicit.first().reason.contains("family-friendly"))

        val implicit = LocalRecommender.recommend("movie night for the kids", catalog)
        assertFalse(implicit.first().reason.contains("family-friendly"))
    }

    // --- similar titles (backend: "similar excludes the source title and shares mood/genre") ---

    @Test
    fun `similar excludes the source title and leads with a related pick`() {
        val source = catalog.first { it.id == "fv003" } // Midnight Cartography
        val recommendations = LocalRecommender.similarTo(source, catalog)

        assertTrue(recommendations.size >= 3)
        assertFalse(recommendations.any { it.id == source.id })

        val best = catalog.first { it.id == recommendations.first().id }
        val related = best.moods.any { it in source.moods } || best.genres.any { it in source.genres }
        assertTrue("top similar pick '${best.title}' shares no mood or genre", related)
    }

    @Test
    fun `similar picks carry a reason naming the source title`() {
        val source = catalog.first { it.id == "fv022" } // Deep Field
        val recommendations = LocalRecommender.similarTo(source, catalog)
        recommendations.forEach { rec ->
            assertTrue(rec.reason.contains("Similar to \"${source.title}\""))
            assertNotNull(rec.summary)
        }
    }

    // --- defensive behavior ---

    @Test
    fun `handles an empty catalog without crashing`() {
        val recommendations = LocalRecommender.recommend("something funny", emptyList())
        assertTrue(recommendations.isEmpty())
    }

    @Test
    fun `handles an empty query without crashing`() {
        val recommendations = LocalRecommender.recommend("", catalog)
        assertEquals(LocalRecommender.MAX_RESULTS, recommendations.size)
    }

    @Test
    fun `ignores unknown ids safely in similar`() {
        val ghost = Movie(
            id = "fv999",
            title = "Not In Catalog",
            year = 2020,
            genres = listOf("Drama"),
            moods = listOf("Serious"),
            runtime = 100,
            familyFriendly = false,
            rating = 7.0,
            description = "A title that does not exist in the catalog."
        )
        // A ghost source still yields picks scored purely on the ghost's metadata.
        val recommendations = LocalRecommender.similarTo(ghost, catalog)
        assertTrue(recommendations.isNotEmpty())
        assertFalse(recommendations.any { it.id == ghost.id })
    }
}
