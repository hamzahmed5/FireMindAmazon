package com.firemind.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.watchlistDataStore by preferencesDataStore(name = "watchlist")

/**
 * Watchlist persistence via Jetpack DataStore. Survives app restarts and
 * device reboots; no account system needed for the MVP.
 */
class WatchlistStore(private val context: Context) {

    private val key = stringSetPreferencesKey("watchlist_ids")

    val ids: Flow<Set<String>> = context.watchlistDataStore.data
        .map { prefs -> prefs[key] ?: emptySet() }

    suspend fun add(id: String) = context.watchlistDataStore.edit { prefs ->
        prefs[key] = (prefs[key] ?: emptySet()) + id
    }

    suspend fun remove(id: String) = context.watchlistDataStore.edit { prefs ->
        prefs[key] = (prefs[key] ?: emptySet()) - id
    }
}
