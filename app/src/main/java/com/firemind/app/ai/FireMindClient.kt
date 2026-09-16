package com.firemind.app.ai

import com.firemind.app.data.model.HealthResponse
import com.firemind.app.data.model.RecommendRequest
import com.firemind.app.data.model.RecommendResponse
import com.firemind.app.data.model.SummarizeRequest
import com.firemind.app.data.model.SummarizeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Thin HTTP client for the FireMind backend.
 *
 * Every call returns null on any failure (timeout, connection refused,
 * malformed body); callers fall back to the local recommendation path.
 * The backend URL is injected at construction so tests and builds can
 * point at different hosts; no credentials are ever embedded.
 */
class FireMindClient(baseUrl: String) {

    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val healthUrl = "$baseUrl/api/health"
    private val recommendUrl = "$baseUrl/api/recommend"
    private val summarizeUrl = "$baseUrl/api/summarize"
    private val similarUrl = "$baseUrl/api/similar"

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun health(): HealthResponse? = withContext(Dispatchers.IO) {
        runCatching {
            val body = http.newCall(Request.Builder().url(healthUrl).build())
                .execute().use { resp -> resp.body?.string().orEmpty() }
            json.decodeFromString<HealthResponse>(body)
        }.getOrNull()
    }

    suspend fun recommend(request: RecommendRequest): RecommendResponse? =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = json.encodeToString(RecommendRequest.serializer(), request)
                val req = Request.Builder()
                    .url(recommendUrl)
                    .post(payload.toRequestBody(jsonMedia))
                    .build()
                val body = http.newCall(req).execute().use { resp -> resp.body?.string().orEmpty() }
                json.decodeFromString<RecommendResponse>(body)
            }.getOrNull()
        }

    suspend fun summarize(id: String): SummarizeResponse? = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.encodeToString(SummarizeRequest.serializer(), SummarizeRequest(id))
            val req = Request.Builder()
                .url(summarizeUrl)
                .post(payload.toRequestBody(jsonMedia))
                .build()
            val body = http.newCall(req).execute().use { resp -> resp.body?.string().orEmpty() }
            json.decodeFromString<SummarizeResponse>(body)
        }.getOrNull()
    }

    suspend fun similar(id: String): RecommendResponse? = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.encodeToString(SummarizeRequest.serializer(), SummarizeRequest(id))
            val req = Request.Builder()
                .url(similarUrl)
                .post(payload.toRequestBody(jsonMedia))
                .build()
            val body = http.newCall(req).execute().use { resp -> resp.body?.string().orEmpty() }
            json.decodeFromString<RecommendResponse>(body)
        }.getOrNull()
    }
}
