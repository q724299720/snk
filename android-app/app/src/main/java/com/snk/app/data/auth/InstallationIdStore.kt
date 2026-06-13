package com.snk.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

interface InstallationIdStoreContract {
    suspend fun getOrCreateInstallationId(): String
    suspend fun saveSession(session: AnonymousSession)
    suspend fun getCachedSession(): AnonymousSession?
}

class InstallationIdStore(
    private val context: Context,
) : InstallationIdStoreContract {
    override suspend fun getOrCreateInstallationId(): String {
        val existing = readPreferences()[Keys.INSTALLATION_ID]
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val generated = UUID.randomUUID().toString()
        context.authDataStore.edit { preferences ->
            preferences[Keys.INSTALLATION_ID] = generated
        }
        return generated
    }

    override suspend fun saveSession(session: AnonymousSession) {
        context.authDataStore.edit { preferences ->
            preferences[Keys.USER_ID] = session.userId
            preferences[Keys.AUTH_PROVIDER] = session.authProvider
            preferences[Keys.LAST_INSTALLATION_ID] = session.installationId
            preferences[Keys.CREATED_AT] = session.createdAt
            preferences[Keys.LAST_SEEN_AT] = session.lastSeenAt
        }
    }

    override suspend fun getCachedSession(): AnonymousSession? {
        val preferences = readPreferences()
        val installationId = preferences[Keys.LAST_INSTALLATION_ID] ?: return null
        val userId = preferences[Keys.USER_ID] ?: return null

        return AnonymousSession(
            userId = userId,
            authProvider = preferences[Keys.AUTH_PROVIDER] ?: "anonymous",
            installationId = installationId,
            newlyCreated = false,
            createdAt = preferences[Keys.CREATED_AT] ?: "",
            lastSeenAt = preferences[Keys.LAST_SEEN_AT] ?: "",
        )
    }

    private suspend fun readPreferences(): Preferences = context.authDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .first()

    private object Keys {
        val INSTALLATION_ID = stringPreferencesKey("installation_id")
        val USER_ID = longPreferencesKey("anonymous_user_id")
        val AUTH_PROVIDER = stringPreferencesKey("auth_provider")
        val LAST_INSTALLATION_ID = stringPreferencesKey("last_installation_id")
        val CREATED_AT = stringPreferencesKey("created_at")
        val LAST_SEEN_AT = stringPreferencesKey("last_seen_at")
    }
}
