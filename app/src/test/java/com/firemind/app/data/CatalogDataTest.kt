package com.firemind.app.data

import com.firemind.app.data.recommend.LocalRecommender
import com.firemind.app.data.recommend.TestCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integrity checks for the shipped catalog, mirroring
 * "catalog loads with 60 valid titles" in the backend suite. Guards
 * against a malformed or truncated asset shipping in the APK.
 */
class CatalogDataTest {

    private val movies = TestCatalog.movies

    @Test
    fun `catalog ships with 60 titles`() {
        assertEquals(60, movies.size)
    }

    @Test
    fun `every title has a unique well-formed id`() {
        movies.forEach { movie ->
            assertTrue("bad id: ${movie.id}", Regex("^fv\\d{3}$").matches(movie.id))
        }
        assertEquals(
            "ids must be unique",
            movies.size,
            movies.map { it.id }.toSet().size
        )
    }

    @Test
    fun `every title has usable metadata`() {
        movies.forEach { movie ->
            assertTrue("empty title", movie.title.isNotBlank())
            assertTrue("${movie.title}: implausible runtime ${movie.runtime}", movie.runtime in 60..140)
            assertTrue("${movie.title}: rating out of range ${movie.rating}", movie.rating in 6.0..9.0)
            assertTrue("${movie.title}: no genres", movie.genres.isNotEmpty())
            assertTrue("${movie.title}: no moods", movie.moods.isNotEmpty())
            assertTrue(
                "${movie.title}: description too short",
                movie.description.length > 20
            )
        }
    }

    /**
     * The invariant that matters for the Home screen: tapping a chip must
     * constrain results to something real. This caught two chips that
     * resolved to nothing at all ("Sci-Fi" and "Family" are genres, not
     * mood tags, and the engine had no genre concept).
     */
    @Test
    fun `every UI chip resolves to a real catalog mood or genre`() {
        val catalogMoods = movies.flatMap { it.moods }.toSet()
        val catalogGenres = movies.flatMap { it.genres }.toSet()

        CatalogRepository.MOOD_CHIPS.forEach { chip ->
            val mood = LocalRecommender.parseMood(chip)
            val genre = LocalRecommender.parseGenre(chip)

            assertTrue(
                "chip '$chip' resolves to neither a mood nor a genre",
                mood != null || genre != null
            )
            mood?.let {
                assertTrue("chip '$chip' maps to unknown mood '$it'", catalogMoods.contains(it))
            }
            genre?.let {
                assertTrue("chip '$chip' maps to unknown genre '$it'", catalogGenres.contains(it))
            }
        }
    }

    @Test
    fun `catalog includes family friendly titles for the audience filter`() {
        assertTrue(movies.count { it.familyFriendly } >= 10)
    }
}
