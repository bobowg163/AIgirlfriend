package com.example.aigirlfriend.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

object UserPreferences {
    val API_KEY = stringPreferencesKey("api_key")
    val CHAT_HISTORY = stringPreferencesKey("chat_history")
    val DEFAULT_CITY = stringPreferencesKey("default_city")
}

class PreferencesManager(private val context: Context) {
    fun getApiKey(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[UserPreferences.API_KEY]
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[UserPreferences.API_KEY] = key
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(UserPreferences.API_KEY)
        }
    }

    fun getDefaultCity(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[UserPreferences.DEFAULT_CITY] ?: "三亚"
    }

    suspend fun saveDefaultCity(city: String) {
        context.dataStore.edit { preferences ->
            preferences[UserPreferences.DEFAULT_CITY] = city
        }
    }
}
