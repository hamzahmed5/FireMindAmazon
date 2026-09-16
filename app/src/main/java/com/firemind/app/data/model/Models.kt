package com.firemind.app.data.model

import kotlinx.serialization.Serializable

/** A title in the FireMind catalog. Mirrors the schema in docs/API_SPEC.md. */
@Serializable
data class Movie(
    val id: String,
    val title: String,
    val year: Int,
    val genres: List<String>,
    val moods: List<String>,
    val runtime: Int,
    val familyFriendly: Boolean,
    val rating: Double,
    val description: String
)

/** Filters the app can apply client-side before/instead of an AI round trip. */
@Serializable
data class Filters(
    val runtimeMax: Int? = null,
    val familyOnly: Boolean = false
)

/** Request body for POST /api/recommend. */
@Serializable
data class RecommendRequest(
    val query: String,
    val filters: Filters = Filters()
)

/** One recommendation returned by the backend (AI or deterministic fallback). */
@Serializable
data class Recommendation(
    val id: String,
    val title: String,
    val year: Int,
    val genres: List<String>,
    val runtime: Int,
    val rating: Double,
    val reason: String,
    val summary: String
)

/** Response for /api/recommend and /api/similar. */
@Serializable
data class RecommendResponse(
    val source: String,
    val recommendations: List<Recommendation>
)

/** Response for GET /api/health. */
@Serializable
data class HealthResponse(
    val status: String,
    val aiConfigured: Boolean
)

/** Request/response for POST /api/summarize. */
@Serializable
data class SummarizeRequest(val id: String)

@Serializable
data class SummarizeResponse(val summary: String)
