package com.mzwprojects.mytwin.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mytwin_prefs")

class OnboardingDataSource(private val context: Context) {

    private val onboardingKey = booleanPreferencesKey("onboarding_completed")

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[onboardingKey] ?: false }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[onboardingKey] = true
        }
    }
}