package com.snk.app.data.food

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.searchDataStore by preferencesDataStore(name = "search_prefs")

interface RecentSearchStoreContract {
    suspend fun getRecentQueries(): List<String>
    suspend fun rememberQuery(query: String): List<String>
    suspend fun clearRecentQueries()
}

class RecentSearchStore(
    private val context: Context,
    private val maxEntries: Int = 8,
) : RecentSearchStoreContract {
    private val json = Json

    override suspend fun getRecentQueries(): List<String> = readQueries()

    override suspend fun rememberQuery(query: String): List<String> {
        val normalizedQuery = query.trim().replace(Regex("\\s+"), " ")
        if (normalizedQuery.length < 2) {
            return readQueries()
        }

        val updatedQueries = buildList {
            add(normalizedQuery)
            addAll(
                readQueries().filterNot {
                    it.equals(normalizedQuery, ignoreCase = true)
                },
            )
        }.take(maxEntries)

        context.searchDataStore.edit { preferences ->
            preferences[Keys.RECENT_QUERIES] = json.encodeToString(
                ListSerializer(String.serializer()),
                updatedQueries,
            )
        }
        return updatedQueries
    }

    override suspend fun clearRecentQueries() {
        context.searchDataStore.edit { preferences ->
            preferences.remove(Keys.RECENT_QUERIES)
        }
    }

    private suspend fun readQueries(): List<String> {
        val rawValue = readPreferences()[Keys.RECENT_QUERIES] ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), rawValue)
        }.getOrDefault(emptyList())
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(maxEntries)
    }

    private suspend fun readPreferences(): Preferences = context.searchDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .first()

    private object Keys {
        val RECENT_QUERIES = stringPreferencesKey("recent_queries")
    }
}
