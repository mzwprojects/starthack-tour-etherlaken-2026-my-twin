package com.mzwprojects.mytwin.data.datasource

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mzwprojects.mytwin.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.userProfileStore by preferencesDataStore(name = "user_profile")

/**
 * Persists the full [UserProfile] as a single JSON string. Single-field reads
 * are fine — DataStore caches the latest value in memory.
 */
class UserProfileDataSource(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val profile: Flow<UserProfile> = context.userProfileStore.data.map { prefs ->
        prefs[KEY_PROFILE_JSON]
            ?.let { runCatching { json.decodeFromString<UserProfile>(it) }.getOrNull() }
            ?: UserProfile()
    }

    suspend fun update(transform: (UserProfile) -> UserProfile) {
        context.userProfileStore.edit { prefs ->
            val current = prefs[KEY_PROFILE_JSON]
                ?.let { runCatching { json.decodeFromString<UserProfile>(it) }.getOrNull() }
                ?: UserProfile()
            prefs[KEY_PROFILE_JSON] = json.encodeToString(transform(current))
        }
    }

    private companion object {
        val KEY_PROFILE_JSON = stringPreferencesKey("profile_json")
    }
}