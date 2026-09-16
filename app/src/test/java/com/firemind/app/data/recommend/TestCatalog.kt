package com.firemind.app.data.recommend

import com.firemind.app.data.model.Movie
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loads the real shipped catalog (`app/src/main/assets/catalog.json`) for
 * unit tests, so tests assert against production data rather than a
 * fixture that could drift from it.
 *
 * The file is on the test classpath via the `resources.srcDir` wiring in
 * `app/build.gradle.kts`; the file-path fallback keeps the tests working
 * if a runner does not expose it as a resource.
 */
object TestCatalog {

    private val json = Json { ignoreUnknownKeys = true }

    val movies: List<Movie> by lazy {
        json.decodeFromString<List<Movie>>(readCatalogText())
    }

    private fun readCatalogText(): String {
        TestCatalog::class.java.classLoader
            ?.getResourceAsStream("catalog.json")
            ?.bufferedReader()
            ?.use { return it.readText() }

        // Unit tests run with the module directory as the working directory.
        val localCopy = File("src/main/assets/catalog.json")
        check(localCopy.exists()) {
            "catalog.json not found on the test classpath or at ${localCopy.absolutePath}"
        }
        return localCopy.readText()
    }
}
