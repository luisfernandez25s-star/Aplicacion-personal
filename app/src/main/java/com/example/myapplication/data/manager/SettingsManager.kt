package com.example.myapplication.data.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val API_URI_KEY = stringPreferencesKey("api_uri")
        private val MONGODB_URI_KEY = stringPreferencesKey("mongodb_uri") // For backward compatibility if needed
    }

    val mongoUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[API_URI_KEY] ?: preferences[MONGODB_URI_KEY] ?: "https://my-backend-sensores.onrender.com/"
    }

    suspend fun saveMongoUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[API_URI_KEY] = uri
        }
    }
}
